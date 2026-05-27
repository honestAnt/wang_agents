"""Task classifier — hybrid keyword + LLM classification.

Classifies prompts into: code, analysis, chat, search, translation, summarization,
creative, math, data_science, and unknown. Uses keyword heuristics first,
falling back to LLM classification for ambiguous cases.
"""

import json
import os
import re
from dataclasses import dataclass, field


@dataclass
class ClassificationResult:
    task_type: str
    confidence: float  # 0.0–1.0
    complexity: str = "low"  # low, medium, high
    estimated_tokens: int = 0
    keywords_matched: list[str] = field(default_factory=list)


class TaskClassifier:
    """Hybrid classifier: keyword heuristics + optional LLM refinement."""

    TASK_PATTERNS: dict[str, list[str]] = {
        "code": [
            r"\b(code|program|function|class|debug|bug|compile|compile error|error|exception|api|endpoint|rest|graphql|sql|query|database|schema|migration|refactor|deploy|ci/cd|pipeline|docker|kubernetes|regex|pattern)\b",
            r"\b(write|fix|implement|create|build)\b.*\b(app|application|script|module|package|library|api|server|service|component)\b",
            r"```|import |def |class |const |let |var |function |async |await ",
        ],
        "analysis": [
            r"\b(analyze|analysis|analytics|insight|trend|pattern|correlat|statistic|metrics|kpi|dashboard|report|forecast|predict|compare|benchmark)\b",
            r"\b(data|dataset|csv|excel|spreadsheet|table)\b.*\b(analyze|analyse|explore|examine|investigate)\b",
        ],
        "chat": [
            r"\b(hello|hi|hey|help|thanks|thank you|please|what|how|who|when|where|why|can you|could you|would you|tell me|explain|describe)\b",
        ],
        "search": [
            r"\b(find|search|lookup|retrieve|locate|query|fetch|get)\b.*\b(document|documents|file|files|record|records|information|data|knowledge|article|articles|report|reports|policy|policies)\b",
            r"\b(look up|look for)\b",
        ],
        "translation": [
            r"\b(translate|translation|convert to|in english|in chinese|in french|in german|in japanese|in spanish|to english|to chinese)\b",
        ],
        "summarization": [
            r"\b(summarize|summary|summarization|tldr|key points|bullet points|condense|recap|overview|digest)\b",
        ],
        "creative": [
            r"\b(write|compose|generate|create|draft|design)\b.*\b(story|poem|article|blog|essay|letter|email|message|content|copy|slogan|tagline|headline)\b",
        ],
        "math": [
            r"\b(calculate|solve|equation|math|mathematics|formula|computation|algebra|calculus|geometry|probability|statistic)\b",
        ],
        "data_science": [
            r"\b(machine learning|deep learning|neural network|training|model|dataset|feature|accuracy|precision|recall|classification|regression|clustering|nlp|computer vision)\b",
            r"\b(pandas|numpy|scikit-learn|tensorflow|pytorch|keras|xgboost|lightgbm)\b",
        ],
    }

    COMPLEXITY_PATTERNS = {
        "high": [
            r"\b(multi-step|complex|sophisticated|advanced|enterprise|production|scalable|distributed|microservice|orchestrat|workflow|pipeline)\b",
            r"```[\s\S]*```",
            r".{500,}",
        ],
        "low": [
            r"\b(simple|quick|easy|basic|straightforward|brief|short)\b",
        ],
    }

    LLM_CLASSIFICATION_PROMPT = (
        "Classify the following user prompt into EXACTLY ONE category.\n"
        "Categories: code, analysis, chat, search, translation, summarization, creative, math, data_science.\n"
        "Respond with ONLY the category name, nothing else.\n\n"
        "Prompt: {prompt}\n\nCategory:"
    )

    def classify(self, prompt: str) -> ClassificationResult:
        """Classify a user prompt. Uses LLM refinement for low-confidence heuristic results."""
        prompt_lower = prompt.lower()
        scores: dict[str, float] = {}
        all_keywords: list[str] = []

        for task_type, patterns in self.TASK_PATTERNS.items():
            score = 0.0
            matched = []
            for pattern in patterns:
                matches = re.findall(pattern, prompt_lower, re.IGNORECASE)
                if matches:
                    score += min(len(matches), 3) * 0.33
                    matched.extend(matches[:3])
            if score > 0:
                scores[task_type] = min(score, 1.0)
                all_keywords.extend(matched)

        complexity = self._classify_complexity(prompt)
        estimated_tokens = self._estimate_tokens(prompt)

        if not scores:
            return ClassificationResult(
                task_type="unknown", confidence=0.0,
                complexity=complexity, estimated_tokens=estimated_tokens,
            )

        best_type = max(scores, key=scores.get)
        best_score = min(scores[best_type], 1.0)

        # For low-confidence results, refine via LLM
        if best_score < 0.5 and len(scores) > 1:
            refined = self._classify_with_llm(prompt)
            if refined and refined in self.TASK_PATTERNS:
                return ClassificationResult(
                    task_type=refined, confidence=0.6,
                    complexity=complexity, estimated_tokens=estimated_tokens,
                    keywords_matched=list(set(all_keywords))[:10],
                )

        return ClassificationResult(
            task_type=best_type, confidence=round(best_score, 2),
            complexity=complexity, estimated_tokens=estimated_tokens,
            keywords_matched=list(set(all_keywords))[:10],
        )

    def _classify_complexity(self, prompt: str) -> str:
        prompt_lower = prompt.lower()
        prompt_len = len(prompt)

        for pattern in self.COMPLEXITY_PATTERNS["high"]:
            if re.search(pattern, prompt_lower, re.IGNORECASE | re.DOTALL):
                return "high"

        if prompt_len > 300:
            return "medium"

        for pattern in self.COMPLEXITY_PATTERNS["low"]:
            if re.search(pattern, prompt_lower, re.IGNORECASE):
                return "low"

        return "medium"

    def _estimate_tokens(self, prompt: str) -> int:
        return max(1, len(prompt) // 4)

    def _classify_with_llm(self, prompt: str) -> str | None:
        """Use a lightweight LLM to classify ambiguous prompts."""
        try:
            import asyncio
            loop = asyncio.get_event_loop()
        except RuntimeError:
            return None

        async def _do():
            from app.llm.model_wrapper import ModelWrapper
            wrapper = ModelWrapper()
            classifier_prompt = self.LLM_CLASSIFICATION_PROMPT.format(prompt=prompt[:1000])
            result = ""
            async for chunk in wrapper.chat(
                messages=[{"role": "user", "content": classifier_prompt}],
                model=os.getenv("CLASSIFIER_MODEL", "claude-haiku-4-5-20251001"),
                temperature=0.0,
                max_tokens=20,
                stream=False,
            ):
                result += chunk
            return result.strip().lower()

        try:
            if loop.is_running():
                return None  # can't nest async calls
            return loop.run_until_complete(_do())
        except Exception:
            return None
