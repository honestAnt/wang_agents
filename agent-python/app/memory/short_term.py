"""Short-term memory — Redis-backed session context with sliding window."""

import json


class ShortTermMemory:
    """Redis-backed short-term memory for session conversation context."""

    MAX_WINDOW = 20  # max messages per session

    def __init__(self):
        # In production: redis.Redis.from_url(os.getenv("REDIS_URL"))
        self._store: dict[str, list[dict]] = {}

    async def add(self, session_id: str, message: dict) -> None:
        """Add a message to the session's sliding window."""
        if session_id not in self._store:
            self._store[session_id] = []
        self._store[session_id].append(message)
        if len(self._store[session_id]) > self.MAX_WINDOW:
            self._store[session_id] = self._store[session_id][-self.MAX_WINDOW:]

    async def get(self, session_id: str, limit: int = 10) -> list[dict]:
        """Get recent messages for a session."""
        messages = self._store.get(session_id, [])
        return messages[-limit:]
