"""Intent router — keyword heuristics + LLM classification for user intent."""

import os


class IntentRouter:
    """Classifies user intent to determine which skill/tool path to use.

    Uses keyword heuristics for fast common cases, falling back to an LLM
    call for ambiguous prompts.
    """

    INTENTS = [
        "chat", "code", "analysis", "search", "translation",
        "summarization", "data_extraction", "customer_service",
    ]

    INTENT_KEYWORDS: dict[str, list[str]] = {
        "code": ["code", "debug", "fix", "implement", "function", "api"],
        "analysis": ["analyze", "analysis", "chart", "graph", "statistics", "report"],
        "search": ["search", "find", "lookup", "query"],
        "translation": ["translate", "translation"],
        "summarization": ["summarize", "summary", "tl;dr"],
        "data_extraction": ["extract", "parse", "pdf", "excel"],
        "customer_service": ["help", "support", "issue", "problem", "complaint"],
    }

    LLM_INTENT_PROMPT = (
        "Classify this user message into exactly ONE intent category.\n"
        "Categories: chat, code, analysis, search, translation, summarization, data_extraction, customer_service.\n"
        "Respond with ONLY the category name.\n\n"
        "Message: {message}\n\nIntent:"
    )

    async def route(self, message: str) -> str:
        """Determine intent from user message. Uses LLM for ambiguous cases."""
        msg_lower = message.lower()

        # Keyword-based classification
        scores: dict[str, int] = {}
        for intent, keywords in self.INTENT_KEYWORDS.items():
            score = sum(1 for kw in keywords if kw in msg_lower)
            if score > 0:
                scores[intent] = score

        if scores:
            best = max(scores, key=scores.get)
            return best

        # No keyword match — use LLM classification
        return await self._classify_with_llm(message)

    async def _classify_with_llm(self, message: str) -> str:
        """Use a lightweight LLM to classify ambiguous intents."""
        try:
            from app.llm.model_wrapper import ModelWrapper

            wrapper = ModelWrapper()
            prompt = self.LLM_INTENT_PROMPT.format(message=message[:1000])
            result = ""
            async for chunk in wrapper.chat(
                messages=[{"role": "user", "content": prompt}],
                model=os.getenv("ROUTER_MODEL", "claude-haiku-4-5-20251001"),
                temperature=0.0,
                max_tokens=15,
                stream=False,
            ):
                result += chunk

            intent = result.strip().lower()
            if intent in self.INTENTS:
                return intent
            return "chat"
        except Exception:
            return "chat"
