"""Model router — selects the best model based on task type, cost, and availability."""


class ModelRouter:
    """Intelligent model routing based on task type and cost optimization."""

    DEFAULT_FALLBACK_CHAIN = [
        "gpt-4.1",
        "claude-sonnet-4-6",
        "deepseek-v3",
        "qwen-max",
    ]

    TASK_MODEL_MAP = {
        "code": ["gpt-4.1", "claude-sonnet-4-6"],
        "analysis": ["gpt-4.1", "claude-sonnet-4-6"],
        "chat": ["claude-haiku-4-5-20251001", "gpt-4.1-mini"],
        "search": ["claude-haiku-4-5-20251001"],
        "translation": ["deepseek-v3", "qwen-max"],
        "summarization": ["claude-haiku-4-5-20251001", "gpt-4.1-mini"],
    }

    async def select(
        self,
        tenant_id: str,
        intent: str,
        preferred_model: str | None = None,
    ) -> str:
        """Select the best model for the given task.

        In production, this calls the Java model-service to check
        quotas, budgets, and routing rules per tenant.
        """
        if preferred_model:
            return preferred_model

        candidates = self.TASK_MODEL_MAP.get(intent, self.DEFAULT_FALLBACK_CHAIN)
        # In production: check quota/budget, filter unavailable models
        return candidates[0]
