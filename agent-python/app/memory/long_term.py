"""Long-term memory — PostgreSQL-backed persistent memory."""


class LongTermMemory:
    """Persistent long-term memory with episodic, semantic, and procedural types.

    In production, delegates to Java memory-service via gRPC or REST API.
    """

    async def store(
        self,
        tenant_id: str,
        user_id: str,
        memory_type: str,  # episodic, semantic, procedural
        content: str,
        metadata: dict | None = None,
    ) -> str:
        """Store a memory entry."""
        return "mem-placeholder-id"

    async def retrieve(
        self,
        tenant_id: str,
        user_id: str,
        query: str,
        memory_types: list[str] | None = None,
        limit: int = 5,
    ) -> list[dict]:
        """Retrieve relevant memories by semantic similarity."""
        return []

    async def get_by_session(self, session_id: str) -> list[dict]:
        """Get all memories for a session (episodic timeline)."""
        return []
