"""Tool client — execute registered tools."""

import httpx


class ToolClient:
    """Client for executing tools via the tool registry."""

    def __init__(self, base_url: str = "http://localhost:8080", api_key: str | None = None):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def _headers(self) -> dict:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return headers

    async def execute(
        self,
        tenant_id: str,
        tool_name: str,
        parameters: dict | None = None,
    ) -> dict:
        """Execute a registered tool."""
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{self.base_url}/api/tools/execute",
                json={
                    "tenant_id": tenant_id,
                    "tool_name": tool_name,
                    "parameters": parameters or {},
                },
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json()

    async def list_tools(self, tenant_id: str) -> list[dict]:
        """List all registered tools for a tenant."""
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/tools",
                params={"tenantId": tenant_id},
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", [])
