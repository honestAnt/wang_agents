"""RAG retriever — local Qdrant vector search with embedding + optional rerank."""

import logging

logger = logging.getLogger(__name__)


class Retriever:
    """Local retriever using Qdrant vector search with embedding and optional reranking."""

    async def search(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int = 5,
        enable_rerank: bool = True,
        permission_filters: dict | None = None,
    ) -> list[dict]:
        """Execute vector search against the local knowledge base via Qdrant."""
        results = await self._search_local(
            tenant_id, kb_id, query, top_k, permission_filters
        )
        if not results:
            logger.debug("RAG search returned no results for kb=%s", kb_id)
            return []

        if enable_rerank and len(results) > 1:
            results = await self._apply_rerank(query, results, top_k)
        return results[:top_k]

    async def _search_local(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int,
        permission_filters: dict | None,
    ) -> list[dict]:
        """Search via local Qdrant."""
        try:
            from app.rag.index_store import IndexStore
            from app.rag.embedding import EmbeddingClient

            index_store = IndexStore()
            if not index_store.collection_exists(kb_id, tenant_id):
                return []

            embedder = EmbeddingClient()
            embeddings = await embedder.embed([query])
            if not embeddings or not embeddings[0]:
                return []

            return index_store.search(
                kb_id=kb_id,
                tenant_id=tenant_id,
                query_vector=embeddings[0],
                limit=top_k * 2,
                filters=permission_filters,
            )
        except Exception as e:
            logger.debug("Local Qdrant search failed: %s", e)
            return []

    async def _apply_rerank(
        self, query: str, chunks: list[dict], top_k: int
    ) -> list[dict]:
        """Apply reranking to search results."""
        try:
            from app.rag.reranker import Reranker
            reranker = Reranker()
            return await reranker.rerank(query, chunks, top_k=top_k)
        except Exception as e:
            logger.debug("Rerank failed, returning original order: %s", e)
            return chunks[:top_k]
