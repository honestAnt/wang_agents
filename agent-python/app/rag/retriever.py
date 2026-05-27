"""RAG retriever — hybrid search via Java rag-service HTTP API."""

import logging
import os

import httpx

logger = logging.getLogger(__name__)


class Retriever:
    """Hybrid retriever: BM25 + vector + metadata filter via Java rag-service."""

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
        """Execute hybrid search against the knowledge base via rag-service.

        Args:
            tenant_id: Tenant ID for data isolation.
            kb_id: Knowledge base ID to search within.
            query: Search query string.
            top_k: Number of results to return.
            enable_rerank: Whether to apply reranking to results.
            permission_filters: Optional document-level permission filters.
        """
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
                results = data.get("results", data if isinstance(data, list) else [])
                return results
            except Exception as e:
                logger.error("RAG search failed kb=%s: %s", kb_id, e)
                return []
