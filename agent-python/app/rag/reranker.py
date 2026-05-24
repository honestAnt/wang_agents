"""Reranker — re-ranks retrieved chunks for relevance."""


class Reranker:
    """Re-ranks search results using a cross-encoder model."""

    DEFAULT_MODEL = "BAAI/bge-reranker-v2-m3"

    async def rerank(
        self,
        query: str,
        chunks: list[dict],
        top_k: int = 5,
    ) -> list[dict]:
        """Re-rank chunks by relevance to the query.

        In production: use FlagEmbedding, Cohere Rerank API, or Jina Reranker.
        For MVP: return top chunks sorted by existing score.
        """
        sorted_chunks = sorted(
            chunks,
            key=lambda c: c.get("rerank_score", c.get("vector_score", 0)),
            reverse=True,
        )
        return sorted_chunks[:top_k]
