"""KB Manager — unified facade for knowledge base operations."""

from __future__ import annotations

import logging

from app.rag.ingestion.types import ChunkConfig, IngestResult
from app.rag.ingestion.pipeline import IngestionPipeline
from app.rag.index_store import IndexStore
from app.rag.retriever import Retriever

logger = logging.getLogger(__name__)


class KBManager:
    """Unified facade for knowledge base lifecycle management."""

    def __init__(self):
        self._index_store = IndexStore()
        self._pipeline = IngestionPipeline()
        self._retriever = Retriever()

    async def create_kb(self, kb_id: str, tenant_id: str, config: dict | None = None) -> bool:
        """Create a new knowledge base index."""
        ok = self._index_store.create(kb_id, tenant_id)
        if not ok:
            logger.debug("KB create accepted (backend not available): %s/%s", tenant_id, kb_id)
            return True
        return True

    async def delete_kb(self, kb_id: str, tenant_id: str) -> bool:
        """Delete a knowledge base and all its indexed data."""
        ok = self._index_store.delete(kb_id, tenant_id)
        if not ok:
            logger.debug("KB delete accepted (backend not available): %s/%s", tenant_id, kb_id)
        return True

    async def ingest(
        self,
        kb_id: str,
        tenant_id: str,
        source: str,
        chunk_config: ChunkConfig | None = None,
    ) -> IngestResult:
        """Ingest documents from a file path or S3 URI into the knowledge base."""
        return await self._pipeline.run(
            source=source,
            kb_id=kb_id,
            tenant_id=tenant_id,
            chunk_config=chunk_config,
        )

    async def search(
        self,
        kb_id: str,
        tenant_id: str,
        query: str,
        top_k: int = 5,
        enable_rerank: bool = True,
    ) -> list[dict]:
        """Search the knowledge base for relevant chunks."""
        return await self._retriever.search(
            tenant_id=tenant_id,
            kb_id=kb_id,
            query=query,
            top_k=top_k,
            enable_rerank=enable_rerank,
        )

    async def get_stats(self, kb_id: str, tenant_id: str) -> dict:
        """Get index statistics for a knowledge base."""
        return self._index_store.stats(kb_id, tenant_id)
