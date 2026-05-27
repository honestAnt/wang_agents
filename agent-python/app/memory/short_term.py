"""Short-term memory — Redis-backed session context with sliding window."""

import json
import logging
import os

logger = logging.getLogger(__name__)


class ShortTermMemory:
    """Redis-backed short-term memory for session conversation context."""

    MAX_WINDOW = 20
    DEFAULT_TTL = 3600  # 1 hour

    def __init__(self):
        self._redis = None
        self._use_redis = False
        self._store: dict[str, list[dict]] = {}
        self._init_redis()

    def _init_redis(self):
        redis_url = os.getenv("REDIS_URL", "")
        if redis_url:
            try:
                import redis.asyncio as aioredis
                self._redis = aioredis.from_url(redis_url, decode_responses=True)
                self._use_redis = True
                logger.info("ShortTermMemory connected to Redis")
            except Exception as e:
                logger.warning("Redis unavailable, using in-memory store: %s", e)
        else:
            logger.info("REDIS_URL not set, using in-memory store")

    async def add(self, session_id: str, message: dict) -> None:
        """Add a message to the session's sliding window."""
        if self._use_redis and self._redis:
            key = f"session:{session_id}:messages"
            msg_json = json.dumps(message, ensure_ascii=False)
            await self._redis.rpush(key, msg_json)
            await self._redis.ltrim(key, -self.MAX_WINDOW, -1)
            await self._redis.expire(key, self.DEFAULT_TTL)
        else:
            if session_id not in self._store:
                self._store[session_id] = []
            self._store[session_id].append(message)
            if len(self._store[session_id]) > self.MAX_WINDOW:
                self._store[session_id] = self._store[session_id][-self.MAX_WINDOW:]

    async def get(self, session_id: str, limit: int = 10) -> list[dict]:
        """Get recent messages for a session."""
        if self._use_redis and self._redis:
            key = f"session:{session_id}:messages"
            raw = await self._redis.lrange(key, -limit, -1)
            return [json.loads(m) for m in raw]
        messages = self._store.get(session_id, [])
        return messages[-limit:]
