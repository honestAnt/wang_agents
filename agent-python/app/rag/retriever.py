"""RAG retriever — hybrid search via Java rag-service."""


class Retriever:
    """Hybrid retriever: BM25 + vector + metadata filter."""

    async def search(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int = 5,
        enable_rerank: bool = True,
        permission_filters: dict | None = None,
    ) -> list[dict]:
        """Execute hybrid search against the knowledge base.

        In production, calls Java rag-service for BM25 + vector search.
        """
        return []
