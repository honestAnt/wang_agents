"""Long-term memory — delegates to Java memory-service via HTTP API."""

import logging
import os

import httpx

logger = logging.getLogger(__name__)


class LongTermMemory:
    """Persistent long-term memory with episodic, semantic, and procedural types.

    Delegates to Java memory-service for PostgreSQL-backed persistent storage
    with tenant-isolated queries.
    """

    def __init__(self):
        self._memory_service_url = os.getenv("MEMORY_SERVICE_URL", "http://localhost:8089")

    async def store(
        self,
        tenant_id: str,
        user_id: str,
        memory_type: str,  # episodic, semantic, procedural
        content: str,
        metadata: dict | None = None,
    ) -> str:
        """Store a memory entry via memory-service."""
        async with httpx.AsyncClient(timeout=15) as client:
            try:
                url = f"{self._memory_service_url}/api/memory/long-term"
                payload = {
                    "tenant_id": tenant_id,
                    "user_id": user_id,
                    "memory_type": memory_type,
                    "content": content,
                    "metadata": metadata or {},
                }
                resp = await client.post(url, json=payload)
                resp.raise_for_status()
                data = resp.json()
                return data.get("id", "")
            except Exception as e:
                logger.error("LongTermMemory store failed: %s", e)
                return ""

    async def retrieve(
        self,
        tenant_id: str,
        user_id: str,
        query: str,
        memory_types: list[str] | None = None,
        limit: int = 5,
    ) -> list[dict]:
        """Retrieve relevant memories by semantic similarity via memory-service."""
        async with httpx.AsyncClient(timeout=15) as client:
            try:
                url = f"{self._memory_service_url}/api/memory/long-term/retrieve"
                params = {
                    "tenant_id": tenant_id,
                    "user_id": user_id,
                    "query": query,
                    "limit": limit,
                }
                if memory_types:
                    params["memory_types"] = ",".join(memory_types)
                resp = await client.get(url, params=params)
                resp.raise_for_status()
                data = resp.json()
                return data.get("memories", data if isinstance(data, list) else [])
            except Exception as e:
                logger.warning("LongTermMemory retrieve failed: %s", e)
                return []

    async def get_by_session(self, session_id: str) -> list[dict]:
        """Get all memories for a session (episodic timeline)."""
        async with httpx.AsyncClient(timeout=10) as client:
            try:
                url = f"{self._memory_service_url}/api/memory/long-term/session/{session_id}"
                resp = await client.get(url)
                resp.raise_for_status()
                data = resp.json()
                return data.get("memories", data if isinstance(data, list) else [])
            except Exception as e:
                logger.warning("LongTermMemory get_by_session failed: %s", e)
                return []
