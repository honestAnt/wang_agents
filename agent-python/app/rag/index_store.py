"""Index store — manage Qdrant collections for knowledge base indices."""

from __future__ import annotations

import logging
import os

logger = logging.getLogger(__name__)


class IndexStore:
    """Manages Qdrant collections for tenant-isolated knowledge base indices.

    Collection naming: {tenant_id}_{kb_id}
    """

    VECTOR_SIZE = 1536

    def __init__(self):
        self._client = None
        self._url = os.getenv("QDRANT_URL", "")
        self._available = False
        self._ensure_client()

    def _ensure_client(self):
        if not self._url:
            return
        try:
            from qdrant_client import QdrantClient
            self._client = QdrantClient(url=self._url)
            self._available = True
            logger.info("IndexStore connected to Qdrant at %s", self._url)
        except Exception as e:
            logger.warning("Qdrant unavailable for index store: %s", e)
            self._available = False

    def create(self, kb_id: str, tenant_id: str) -> bool:
        """Create a new collection for a knowledge base."""
        if not self._available or not self._client:
            logger.warning("Qdrant not available, cannot create index for %s", kb_id)
            return False

        from qdrant_client.models import Distance, VectorParams

        name = self._collection_name(kb_id, tenant_id)
        try:
            self._client.create_collection(
                collection_name=name,
                vectors_config=VectorParams(size=self.VECTOR_SIZE, distance=Distance.COSINE),
            )
            logger.info("Created index collection: %s", name)
            return True
        except Exception:
            return True  # collection already exists

    def delete(self, kb_id: str, tenant_id: str) -> bool:
        """Delete a knowledge base's collection."""
        if not self._available or not self._client:
            return False

        name = self._collection_name(kb_id, tenant_id)
        try:
            self._client.delete_collection(collection_name=name)
            logger.info("Deleted index collection: %s", name)
            return True
        except Exception as e:
            logger.warning("Failed to delete collection %s: %s", name, e)
            return False

    def add_points(self, kb_id: str, tenant_id: str, points: list) -> bool:
        """Add vector points to a knowledge base collection."""
        if not self._available or not self._client or not points:
            return False

        from qdrant_client.models import PointStruct

        name = self._collection_name(kb_id, tenant_id)
        try:
            qdrant_points = [
                PointStruct(
                    id=p["id"],
                    vector=p["vector"],
                    payload=p.get("payload", {}),
                )
                for p in points
            ]
            self._client.upsert(collection_name=name, points=qdrant_points)
            return True
        except Exception as e:
            logger.error("Failed to add points to %s: %s", name, e)
            return False

    def delete_points(self, kb_id: str, tenant_id: str, doc_ids: list[str]) -> bool:
        """Delete points by document IDs from a collection."""
        if not self._available or not self._client or not doc_ids:
            return False

        from qdrant_client.models import Filter, FieldCondition, MatchAny

        name = self._collection_name(kb_id, tenant_id)
        try:
            self._client.delete(
                collection_name=name,
                points_selector=Filter(
                    must=[FieldCondition(key="doc_id", match=MatchAny(any=doc_ids))]
                ),
            )
            return True
        except Exception as e:
            logger.error("Failed to delete points from %s: %s", name, e)
            return False

    def search(
        self,
        kb_id: str,
        tenant_id: str,
        query_vector: list[float],
        limit: int = 5,
        score_threshold: float = 0.0,
        filters: dict | None = None,
    ) -> list[dict]:
        """Search a knowledge base collection by vector similarity."""
        if not self._available or not self._client:
            return []

        from qdrant_client.models import Filter, FieldCondition, MatchValue

        name = self._collection_name(kb_id, tenant_id)
        must_conditions = [FieldCondition(key="tenant_id", match=MatchValue(value=tenant_id))]
        if filters:
            for key, value in filters.items():
                must_conditions.append(FieldCondition(key=key, match=MatchValue(value=value)))

        try:
            results = self._client.search(
                collection_name=name,
                query_vector=query_vector,
                query_filter=Filter(must=must_conditions),
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
            logger.error("Search failed for %s: %s", name, e)
            return []

    def stats(self, kb_id: str, tenant_id: str) -> dict:
        """Get statistics for a knowledge base collection."""
        if not self._available or not self._client:
            return {}

        name = self._collection_name(kb_id, tenant_id)
        try:
            info = self._client.get_collection(collection_name=name)
            count = self._client.count(collection_name=name)
            return {
                "collection_name": name,
                "vector_count": count.count,
                "vector_size": info.config.params.vectors.size,
            }
        except Exception:
            return {}

    def collection_exists(self, kb_id: str, tenant_id: str) -> bool:
        """Check if a collection exists."""
        if not self._available or not self._client:
            return False
        try:
            self._client.get_collection(collection_name=self._collection_name(kb_id, tenant_id))
            return True
        except Exception:
            return False

    @staticmethod
    def _collection_name(kb_id: str, tenant_id: str) -> str:
        safe_kb = kb_id.replace("/", "_").replace(" ", "_")
        safe_tenant = tenant_id.replace("/", "_").replace(" ", "_")
        return f"{safe_tenant}_{safe_kb}"
