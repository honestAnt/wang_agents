"""Vector memory — Qdrant-backed semantic memory with similarity search."""

import logging
import os
import uuid

logger = logging.getLogger(__name__)


class VectorMemory:
    """Long-term semantic memory using Qdrant vector database for ANN search.

    Stores embeddings in Qdrant with tenant-level collection partitioning.
    """

    COLLECTION_NAME = "enterprise_ai_memory"
    VECTOR_SIZE = 1536  # text-embedding-3-small dimension

    def __init__(self):
        self._client = None
        self._use_qdrant = False
        self._init_qdrant()

    def _init_qdrant(self):
        qdrant_url = os.getenv("QDRANT_URL", "")
        if not qdrant_url:
            logger.info("QDRANT_URL not set, vector memory disabled")
            return
        try:
            from qdrant_client import QdrantClient
            from qdrant_client.models import Distance, VectorParams

            self._client = QdrantClient(url=qdrant_url)
            # Ensure collection exists
            try:
                self._client.create_collection(
                    collection_name=self.COLLECTION_NAME,
                    vectors_config=VectorParams(size=self.VECTOR_SIZE, distance=Distance.COSINE),
                )
                logger.info("Created Qdrant collection '%s'", self.COLLECTION_NAME)
            except Exception:
                pass  # collection already exists
            self._use_qdrant = True
            logger.info("VectorMemory connected to Qdrant at %s", qdrant_url)
        except Exception as e:
            logger.warning("Qdrant unavailable: %s", e)

    async def store(
        self,
        tenant_id: str,
        user_id: str,
        content: str,
        embedding: list[float] | None = None,
        metadata: dict | None = None,
    ) -> str:
        """Store a memory with its vector embedding in Qdrant."""
        if not self._use_qdrant or not self._client:
            return ""

        from qdrant_client.models import PointStruct

        point_id = str(uuid.uuid4())
        vector = embedding or [0.0] * self.VECTOR_SIZE

        payload = {
            "tenant_id": tenant_id,
            "user_id": user_id,
            "content": content,
            **(metadata or {}),
        }

        try:
            self._client.upsert(
                collection_name=self.COLLECTION_NAME,
                points=[PointStruct(id=point_id, vector=vector, payload=payload)],
            )
            return point_id
        except Exception as e:
            logger.error("VectorMemory store failed: %s", e)
            return ""

    async def search(
        self,
        tenant_id: str,
        query_embedding: list[float],
        limit: int = 5,
        score_threshold: float = 0.7,
    ) -> list[dict]:
        """Search for similar memories by vector similarity, filtered by tenant."""
        if not self._use_qdrant or not self._client:
            return []

        from qdrant_client.models import Filter, FieldCondition, MatchValue

        try:
            results = self._client.search(
                collection_name=self.COLLECTION_NAME,
                query_vector=query_embedding,
                query_filter=Filter(
                    must=[FieldCondition(key="tenant_id", match=MatchValue(value=tenant_id))]
                ),
                limit=limit,
                score_threshold=score_threshold,
            )
            return [
                {
                    "id": r.id,
                    "score": r.score,
                    "content": r.payload.get("content", ""),
                    "metadata": {k: v for k, v in r.payload.items() if k != "content"},
                }
                for r in results
            ]
        except Exception as e:
            logger.error("VectorMemory search failed: %s", e)
            return []
