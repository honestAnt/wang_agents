"""RAG retriever — local Qdrant vector search with Java rag-service fallback."""

import logging
import os

import httpx

logger = logging.getLogger(__name__)


class Retriever:
    """Hybrid retriever: local Qdrant ANN search with Java rag-service fallback.

    Priority:
        1. Local Qdrant vector search (fast, no network hop)
        2. Java rag-service HTTP API (fallback when Qdrant is unavailable)
    """

    def __init__(self):
        self._rag_service_url = os.getenv("RAG_SERVICE_URL", "http://localhost:8083")

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

        Tries local Qdrant first; falls back to Java rag-service on failure.
        """
        results = await self._search_local(
            tenant_id, kb_id, query, top_k, permission_filters
        )
        if results:
            if enable_rerank and len(results) > 1:
                results = await self._apply_rerank(query, results, top_k)
            return results[:top_k]

        return await self._search_remote(
            tenant_id, kb_id, query, top_k, enable_rerank, permission_filters
        )

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

    async def _search_remote(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int,
        enable_rerank: bool,
        permission_filters: dict | None,
    ) -> list[dict]:
        """Fallback: search via Java rag-service HTTP API."""
        async with httpx.AsyncClient(timeout=30) as client:
            try:
                url = f"{self._rag_service_url}/api/rag/search"
                payload = {
                    "tenant_id": tenant_id,
                    "kb_id": kb_id,
                    "query": query,
                    "top_k": top_k,
                    "enable_rerank": enable_rerank,
                    "permission_filters": permission_filters or {},
                }
                resp = await client.post(url, json=payload)
                resp.raise_for_status()
                data = resp.json()
                return data.get("results", data if isinstance(data, list) else [])
            except Exception as e:
                logger.error("RAG search failed kb=%s: %s", kb_id, e)
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
