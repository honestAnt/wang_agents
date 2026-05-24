"""Chat client — streaming chat completion with the agent platform."""

from collections.abc import AsyncIterator

import httpx


class ChatClient:
    """Async client for chat completions with streaming SSE support."""

    def __init__(self, base_url: str = "http://localhost:8080", api_key: str | None = None):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def _headers(self) -> dict:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return headers

    async def chat(
        self,
        tenant_id: str,
        message: str,
        *,
        session_id: str | None = None,
        agent_id: str | None = None,
        model: str | None = None,
    ) -> AsyncIterator[str]:
        """Send a chat message and stream the response."""
        async with httpx.AsyncClient(timeout=httpx.Timeout(120.0)) as client:
            async with client.stream(
                "POST",
                f"{self.base_url}/api/v1/chat",
                json={
                    "tenant_id": tenant_id,
                    "message": message,
                    "session_id": session_id,
                    "agent_id": agent_id,
                    "model": model,
                },
                headers=self._headers(),
            ) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        yield line[6:]

    async def chat_sync(
        self,
        tenant_id: str,
        message: str,
        *,
        session_id: str | None = None,
        agent_id: str | None = None,
        model: str | None = None,
    ) -> str:
        """Send a chat message and return the full response."""
        chunks = []
        async for chunk in self.chat(tenant_id, message, session_id=session_id,
                                      agent_id=agent_id, model=model):
            chunks.append(chunk)
        return "".join(chunks)
