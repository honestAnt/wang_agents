"""Intelligent model router — cost-optimized routing with auto-fallback.

Routes LLM requests to the best model based on:
- Task type (code, analysis, chat, search, etc.)
- Task complexity (low → cheap model, high → strong model)
- Cost budget (cheapest model that meets quality requirements)
- Model availability (auto-fallback on failure)

Inspired by: LiteLLM Router, Portkey AI Gateway
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from app.llm.task_classifier import ClassificationResult, TaskClassifier


@dataclass
class ModelInfo:
    """Metadata for a model in the routing table."""

    name: str
    provider: str
    input_cost_per_1k: float
    output_cost_per_1k: float
    quality_tier: int  # 1=premium, 2=standard, 3=budget
    max_tokens: int = 128000
    supports_vision: bool = False
    supports_tools: bool = True


@dataclass
class RouteDecision:
    """The result of a routing decision."""

    model: str
    fallback_chain: list[str]
    reason: str
    estimated_cost: float
    estimated_tokens: int
    task_type: str
    complexity: str


@dataclass
class RouteStats:
    """Collected statistics for a specific route."""

    route_key: str  # e.g., "code-high" or "chat-low"
    attempts: int = 0
    successes: int = 0
    failures: int = 0
    total_latency_ms: float = 0.0
    total_cost: float = 0.0
    models_used: dict[str, int] = field(default_factory=dict)

    @property
    def success_rate(self) -> float:
        if self.attempts == 0:
            return 1.0
        return self.successes / self.attempts

    @property
    def avg_latency_ms(self) -> float:
        if self.attempts == 0:
            return 0.0
        return self.total_latency_ms / self.attempts

    @property
    def avg_cost(self) -> float:
        if self.attempts == 0:
            return 0.0
        return self.total_cost / self.attempts


class ModelRouter:
    """Intelligent model routing with cost optimization and auto-fallback."""

    # ── Model Registry ────────────────────────────────────────

    MODELS: dict[str, ModelInfo] = {
        "gpt-4.1": ModelInfo(name="gpt-4.1", provider="openai",
                             input_cost_per_1k=0.003, output_cost_per_1k=0.006,
                             quality_tier=1, max_tokens=128000),
        "gpt-4.1-mini": ModelInfo(name="gpt-4.1-mini", provider="openai",
                                  input_cost_per_1k=0.00015, output_cost_per_1k=0.0006,
                                  quality_tier=2, max_tokens=128000),
        "claude-sonnet-4-6": ModelInfo(name="claude-sonnet-4-6", provider="anthropic",
                                       input_cost_per_1k=0.003, output_cost_per_1k=0.015,
                                       quality_tier=1, max_tokens=200000),
        "claude-haiku-4-5-20251001": ModelInfo(name="claude-haiku-4-5-20251001", provider="anthropic",
                                               input_cost_per_1k=0.0008, output_cost_per_1k=0.004,
                                               quality_tier=2, max_tokens=200000),
        "deepseek-v3": ModelInfo(name="deepseek-v3", provider="deepseek",
                                 input_cost_per_1k=0.00027, output_cost_per_1k=0.0011,
                                 quality_tier=2, max_tokens=65536),
        "qwen-max": ModelInfo(name="qwen-max", provider="qwen",
                              input_cost_per_1k=0.002, output_cost_per_1k=0.006,
                              quality_tier=2, max_tokens=32768),
        "qwen-turbo": ModelInfo(name="qwen-turbo", provider="qwen",
                                input_cost_per_1k=0.0003, output_cost_per_1k=0.0006,
                                quality_tier=3, max_tokens=32768),
        "gemini-2.0-flash": ModelInfo(name="gemini-2.0-flash", provider="google",
                                      input_cost_per_1k=0.0001, output_cost_per_1k=0.0004,
                                      quality_tier=3, max_tokens=1048576),
    }

    # ── Routing Rules ─────────────────────────────────────────

    # For each (task_type, complexity) → [primary, fallback, fallback, ...]
    ROUTING_TABLE: dict[str, dict[str, list[str]]] = {
        "code": {
            "high": ["gpt-4.1", "claude-sonnet-4-6", "deepseek-v3"],
            "medium": ["claude-sonnet-4-6", "deepseek-v3", "gpt-4.1-mini"],
            "low": ["deepseek-v3", "claude-haiku-4-5-20251001", "qwen-turbo"],
        },
        "analysis": {
            "high": ["gpt-4.1", "claude-sonnet-4-6", "deepseek-v3"],
            "medium": ["claude-sonnet-4-6", "gpt-4.1-mini", "deepseek-v3"],
            "low": ["claude-haiku-4-5-20251001", "gpt-4.1-mini", "qwen-turbo"],
        },
        "data_science": {
            "high": ["gpt-4.1", "claude-sonnet-4-6", "deepseek-v3"],
            "medium": ["claude-sonnet-4-6", "deepseek-v3", "gpt-4.1-mini"],
            "low": ["deepseek-v3", "claude-haiku-4-5-20251001"],
        },
        "math": {
            "high": ["gpt-4.1", "claude-sonnet-4-6"],
            "medium": ["claude-sonnet-4-6", "deepseek-v3"],
            "low": ["deepseek-v3", "claude-haiku-4-5-20251001"],
        },
        "chat": {
            "high": ["claude-sonnet-4-6", "gpt-4.1-mini"],
            "medium": ["gpt-4.1-mini", "claude-haiku-4-5-20251001"],
            "low": ["claude-haiku-4-5-20251001", "gemini-2.0-flash", "qwen-turbo"],
        },
        "search": {
            "high": ["gpt-4.1-mini", "claude-haiku-4-5-20251001"],
            "medium": ["claude-haiku-4-5-20251001", "gemini-2.0-flash"],
            "low": ["gemini-2.0-flash", "qwen-turbo"],
        },
        "translation": {
            "high": ["deepseek-v3", "qwen-max", "claude-sonnet-4-6"],
            "medium": ["qwen-max", "deepseek-v3", "gemini-2.0-flash"],
            "low": ["gemini-2.0-flash", "qwen-turbo"],
        },
        "summarization": {
            "high": ["claude-sonnet-4-6", "gpt-4.1-mini"],
            "medium": ["claude-haiku-4-5-20251001", "gpt-4.1-mini"],
            "low": ["gemini-2.0-flash", "claude-haiku-4-5-20251001"],
        },
        "creative": {
            "high": ["claude-sonnet-4-6", "gpt-4.1"],
            "medium": ["claude-sonnet-4-6", "gpt-4.1-mini"],
            "low": ["gpt-4.1-mini", "claude-haiku-4-5-20251001"],
        },
        "unknown": {
            "high": ["gpt-4.1", "claude-sonnet-4-6"],
            "medium": ["claude-sonnet-4-6", "gpt-4.1-mini"],
            "low": ["claude-haiku-4-5-20251001", "gpt-4.1-mini"],
        },
    }

    # Global fallback when everything else fails
    GLOBAL_FALLBACK_CHAIN = [
        "gpt-4.1", "claude-sonnet-4-6", "deepseek-v3",
        "qwen-max", "gemini-2.0-flash",
    ]

    def __init__(self):
        self.classifier = TaskClassifier()
        self.stats: dict[str, RouteStats] = {}
        self._unavailable_models: set[str] = set()

    # ── Public API ────────────────────────────────────────────

    async def route(self, prompt: str, *, tenant_id: str = "default",
                    preferred_model: str | None = None,
                    budget_limit: float | None = None) -> RouteDecision:
        """Full routing decision: classify → select model → build fallback chain.

        Returns a RouteDecision with the best model and full fallback chain.
        The caller iterates through the fallback chain on failure.
        """
        classification = self.classifier.classify(prompt)
        route_key = f"{classification.task_type}-{classification.complexity}"

        if preferred_model and preferred_model not in self._unavailable_models:
            # Use preferred but still build fallback chain
            candidates = self._get_candidates(classification)
            return RouteDecision(
                model=preferred_model,
                fallback_chain=[m for m in candidates if m != preferred_model],
                reason=f"User-specified preferred model: {preferred_model}",
                estimated_cost=self.estimate_cost(preferred_model, classification.estimated_tokens),
                estimated_tokens=classification.estimated_tokens,
                task_type=classification.task_type,
                complexity=classification.complexity,
            )

        # Get candidates for this (task_type, complexity)
        candidates = self._get_candidates(classification)
        primary = candidates[0] if candidates else self.GLOBAL_FALLBACK_CHAIN[0]

        # Filter out unavailable models
        available = [m for m in candidates if m not in self._unavailable_models]

        # Budget filtering: if budget is set, skip models exceeding it
        if budget_limit is not None:
            affordable = []
            for m in available:
                if self.estimate_cost(m, classification.estimated_tokens) <= budget_limit:
                    affordable.append(m)
            if affordable:
                available = affordable

        if not available:
            available = self.GLOBAL_FALLBACK_CHAIN.copy()
            available = [m for m in available if m not in self._unavailable_models]

        if not available:
            available = self.GLOBAL_FALLBACK_CHAIN.copy()
            reason = "All models unavailable or over budget, using global fallback"
        else:
            reason = f"Routed to {available[0]} for {route_key}"

        return RouteDecision(
            model=available[0],
            fallback_chain=available[1:],
            reason=reason,
            estimated_cost=self.estimate_cost(available[0], classification.estimated_tokens),
            estimated_tokens=classification.estimated_tokens,
            task_type=classification.task_type,
            complexity=classification.complexity,
        )

    async def select(self, tenant_id: str, intent: str,
                     preferred_model: str | None = None) -> str:
        """Legacy API: quick model selection by intent string."""
        if preferred_model:
            return preferred_model
        candidates = self.ROUTING_TABLE.get(intent, {}).get("medium", self.GLOBAL_FALLBACK_CHAIN)
        return candidates[0]

    def mark_unavailable(self, model: str) -> None:
        """Mark a model as unavailable (e.g., after repeated failures)."""
        self._unavailable_models.add(model)

    def mark_available(self, model: str) -> None:
        """Re-mark a model as available (e.g., after recovery)."""
        self._unavailable_models.discard(model)

    # ── Cost Estimation ───────────────────────────────────────

    def estimate_cost(self, model: str, estimated_tokens: int,
                      output_tokens: int | None = None) -> float:
        """Estimate total cost for a model given input tokens.

        Assumes output is ~20% of input for chat/completion.
        """
        info = self.MODELS.get(model)
        if info is None:
            return 0.001 * (estimated_tokens / 1000)

        output = output_tokens or max(1, estimated_tokens // 5)
        input_cost = (estimated_tokens / 1000) * info.input_cost_per_1k
        output_cost = (output / 1000) * info.output_cost_per_1k
        return round(input_cost + output_cost, 6)

    def estimate_all_costs(self, estimated_tokens: int) -> dict[str, float]:
        """Estimate cost for all registered models given input tokens."""
        return {
            name: self.estimate_cost(name, estimated_tokens)
            for name in self.MODELS
        }

    # ── Statistics ────────────────────────────────────────────

    def record_result(self, route_key: str, model: str,
                      success: bool, latency_ms: float, cost: float) -> None:
        """Record a routing outcome for statistics."""
        if route_key not in self.stats:
            self.stats[route_key] = RouteStats(route_key=route_key)

        stats = self.stats[route_key]
        stats.attempts += 1
        if success:
            stats.successes += 1
        else:
            stats.failures += 1
        stats.total_latency_ms += latency_ms
        stats.total_cost += cost
        stats.models_used[model] = stats.models_used.get(model, 0) + 1

    def get_stats(self) -> dict[str, dict[str, Any]]:
        """Get all route statistics."""
        return {
            key: {
                "attempts": s.attempts,
                "successes": s.successes,
                "failures": s.failures,
                "success_rate": s.success_rate,
                "avg_latency_ms": s.avg_latency_ms,
                "avg_cost": s.avg_cost,
                "models_used": s.models_used,
            }
            for key, s in self.stats.items()
        }

    def get_comparison_summary(self) -> dict[str, Any]:
        """High-level routing strategy comparison."""
        total_attempts = sum(s.attempts for s in self.stats.values())
        total_successes = sum(s.successes for s in self.stats.values())
        total_cost = sum(s.total_cost for s in self.stats.values())
        total_latency = sum(s.total_latency_ms for s in self.stats.values())

        model_usage: dict[str, int] = {}
        for s in self.stats.values():
            for model, count in s.models_used.items():
                model_usage[model] = model_usage.get(model, 0) + count

        # Find the most cost-efficient route
        best_route = None
        best_efficiency = float("inf")
        for s in self.stats.values():
            if s.attempts > 0:
                efficiency = s.avg_cost / max(s.success_rate, 0.01)
                if efficiency < best_efficiency:
                    best_efficiency = efficiency
                    best_route = s.route_key

        return {
            "total_attempts": total_attempts,
            "total_successes": total_successes,
            "overall_success_rate": total_successes / max(total_attempts, 1),
            "total_cost": round(total_cost, 4),
            "avg_latency_ms": round(total_latency / max(total_attempts, 1), 1),
            "model_usage": model_usage,
            "most_efficient_route": best_route,
            "unavailable_models": list(self._unavailable_models),
        }

    def reset_stats(self) -> None:
        """Reset all statistics."""
        self.stats.clear()
        self._unavailable_models.clear()

    # ── Internal ──────────────────────────────────────────────

    def _get_candidates(self, classification: ClassificationResult) -> list[str]:
        """Get candidate models for a task classification."""
        task_routes = self.ROUTING_TABLE.get(classification.task_type)
        if task_routes is None:
            task_routes = self.ROUTING_TABLE["unknown"]

        candidates = task_routes.get(classification.complexity)
        if candidates is None:
            candidates = task_routes.get("medium", self.GLOBAL_FALLBACK_CHAIN)

        return candidates.copy()
