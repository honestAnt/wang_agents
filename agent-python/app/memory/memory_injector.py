"""Memory injector — formats memories for prompt injection."""


class MemoryInjector:
    """Injects relevant memories and user preferences into the system prompt."""

    async def inject(
        self,
        tenant_id: str,
        user_id: str,
        session_id: str,
        system_prompt: str,
    ) -> str:
        """Augment the system prompt with relevant memories.

        - Short-term: recent conversation context
        - Long-term: user preferences, historical patterns
        - Vector: semantically similar past interactions
        """
        memories_section = ""

        # In production: fetch and format all three memory types
        # short_term = await self.short_term_memory.get(session_id)
        # long_term = await self.long_term_memory.retrieve(...)
        # vector = await self.vector_memory.search(...)

        if memories_section:
            return f"{system_prompt}\n\n## User Context & Memory\n{memories_section}"

        return system_prompt
