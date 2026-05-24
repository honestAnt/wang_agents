"""Agent client — manage agents programmatically."""

import httpx


class AgentClient:
    """Client for managing agents: create, list, debug, marketplace."""

    def __init__(self, base_url: str = "http://localhost:8080", api_key: str | None = None):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def _headers(self) -> dict:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return headers

    async def create(
        self, tenant_id: str, name: str, description: str = "",
        system_prompt: str = "", model_id: str | None = None,
    ) -> dict:
        """Create a new agent."""
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{self.base_url}/api/agents",
                params={
                    "tenantId": tenant_id, "name": name,
                    "description": description, "systemPrompt": system_prompt,
                    "modelId": model_id or "",
                },
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", {})

    async def list(self, tenant_id: str) -> list[dict]:
        """List all agents for a tenant."""
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/agents",
                params={"tenantId": tenant_id},
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", [])

    async def get(self, agent_id: str) -> dict:
        """Get agent by ID."""
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/agents/{agent_id}",
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", {})

    async def debug(self, agent_id: str, message: str) -> str:
        """Debug an agent with a test message."""
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                f"{self.base_url}/api/agents/{agent_id}/debug",
                content=message,
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", "")
