"""Context builder — assembles system prompt, memory, and RAG context."""


class ContextBuilder:
    """Assembles the full context for an LLM call."""

    async def build(
        self,
        tenant_id: str,
        user_message: str,
        session_id: str,
        agent_id: str | None = None,
    ) -> dict:
        """Build the prompt context from memory, RAG, and tool definitions."""
        return {
            "system_prompt": "You are an enterprise AI assistant.",
            "messages": [],
            "rag_context": [],
            "available_tools": [],
            "available_skills": [],
        }
