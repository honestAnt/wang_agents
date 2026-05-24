"""Intent router — classify user intent and route to appropriate skill."""


class IntentRouter:
    """Classifies user intent to determine which skill/tool path to use."""

    INTENTS = [
        "chat",
        "code",
        "analysis",
        "search",
        "translation",
        "summarization",
        "data_extraction",
        "customer_service",
    ]

    async def route(self, message: str) -> str:
        """Determine intent from user message.

        In production, this calls a lightweight classification LLM.
        For MVP, use keyword-based heuristics.
        """
        msg_lower = message.lower()

        if any(kw in msg_lower for kw in ["code", "debug", "fix", "implement", "function", "api"]):
            return "code"
        if any(kw in msg_lower for kw in ["analyze", "analysis", "chart", "graph", "statistics", "report"]):
            return "analysis"
        if any(kw in msg_lower for kw in ["search", "find", "lookup", "query"]):
            return "search"
        if any(kw in msg_lower for kw in ["translate", "translation"]):
            return "translation"
        if any(kw in msg_lower for kw in ["summarize", "summary", "tl;dr"]):
            return "summarization"
        if any(kw in msg_lower for kw in ["extract", "parse", "pdf", "excel"]):
            return "data_extraction"
        if any(kw in msg_lower for kw in ["help", "support", "issue", "problem", "complaint"]):
            return "customer_service"

        return "chat"
