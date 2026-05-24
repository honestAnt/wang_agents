"""Vector memory — Qdrant-backed semantic memory with similarity search."""


class VectorMemory:
    """Long-term semantic memory using vector embeddings for similarity retrieval.

    In production, stores embeddings in Qdrant for fast ANN search.
    """

    COLLECTION_NAME = "enterprise_ai_memory"

    async def store(
        self,
        tenant_id: str,
        user_id: str,
        content: str,
        embedding: list[float] | None = None,
        metadata: dict | None = None,
    ) -> str:
        """Store a memory with its vector embedding."""
        return "vec-mem-placeholder"

    async def search(
        self,
        tenant_id: str,
        query_embedding: list[float],
        limit: int = 5,
        score_threshold: float = 0.7,
    ) -> list[dict]:
        """Search for similar memories by vector similarity."""
        return []
