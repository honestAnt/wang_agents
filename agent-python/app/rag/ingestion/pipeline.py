"""Ingestion pipeline — orchestrate load → chunk → embed → index."""

from __future__ import annotations

import logging
import os
import uuid

from app.rag.ingestion.types import Document, ChunkConfig, IngestResult
from app.rag.ingestion.loaders import loader_for
from app.rag.ingestion.chunkers import chunker_for
from app.rag.index_store import IndexStore

logger = logging.getLogger(__name__)


class IngestionPipeline:
    """Orchestrates the full document ingestion pipeline.

    Flow: source → loader.load() → chunker.chunk() → embedder.embed() → index_store.add_points()
    """

    def __init__(self):
        self._index_store = IndexStore()

    async def run(
        self,
        source: str,
        kb_id: str,
        tenant_id: str,
        chunk_config: ChunkConfig | None = None,
        embedding_model: str = "text-embedding-3-small",
    ) -> IngestResult:
        """Run the full ingestion pipeline on a source file or S3 URI."""
        result = IngestResult(kb_id=kb_id)
        cfg = chunk_config or ChunkConfig()

        # Step 1: Load documents
        try:
            loader = self._get_loader(source)
            documents = loader.load(source)
        except Exception as e:
            result.errors.append(f"Load error: {e}")
            logger.error("Failed to load %s: %s", source, e)
            return result

        # Filter out empty documents
        documents = [d for d in documents if d.content and d.content.strip()]
        if not documents:
            logger.info("No content extracted from %s", source)
            return result

        for doc in documents:
            doc.metadata["tenant_id"] = tenant_id
            doc.metadata["kb_id"] = kb_id

        result.document_count = len(documents)
        result.doc_ids = [d.doc_id for d in documents]

        # Step 2: Chunk documents
        try:
            chunker = chunker_for(cfg)
            chunks = chunker.chunk(documents)
        except Exception as e:
            result.errors.append(f"Chunk error: {e}")
            logger.error("Failed to chunk: %s", e)
            return result

        if not chunks:
            return result

        result.chunk_count = len(chunks)

        # Step 3: Embed chunks
        try:
            from app.rag.embedding import EmbeddingClient
            embedder = EmbeddingClient()
            embeddings = await embedder.embed([c.content for c in chunks], model=embedding_model)
        except Exception as e:
            result.errors.append(f"Embed error: {e}")
            logger.error("Failed to embed: %s", e)
            return result

        # Step 4: Index into Qdrant
        if embeddings and len(embeddings) == len(chunks):
            points = []
            for chunk, vector in zip(chunks, embeddings):
                point_id = str(uuid.uuid4())
                points.append({
                    "id": point_id,
                    "vector": vector,
                    "payload": {
                        "content": chunk.content,
                        "doc_id": chunk.doc_id,
                        "tenant_id": tenant_id,
                        "kb_id": kb_id,
                        **chunk.metadata,
                    },
                })

            self._index_store.create(kb_id, tenant_id)
            self._index_store.add_points(kb_id, tenant_id, points)
            result.vector_count = len(points)

        return result

    def _get_loader(self, source: str):
        if source.startswith("s3://"):
            from app.rag.ingestion.loaders import S3Loader
            return S3Loader()
        return loader_for(source)
