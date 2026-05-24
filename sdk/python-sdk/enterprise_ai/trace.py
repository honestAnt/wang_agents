"""Trace client — query trace sessions and spans."""

import httpx


class TraceClient:
    """Client for querying trace data: sessions, spans, cost analysis."""

    def __init__(self, base_url: str = "http://localhost:8080", api_key: str | None = None):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def _headers(self) -> dict:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return headers

    async def list_sessions(
        self,
        tenant_id: str,
        *,
        user_id: str | None = None,
        agent_id: str | None = None,
        model: str | None = None,
        status: str | None = None,
        limit: int = 20,
    ) -> list[dict]:
        """List trace sessions with optional filters."""
        params: dict = {"tenantId": tenant_id, "limit": limit}
        if user_id:
            params["userId"] = user_id
        if agent_id:
            params["agentId"] = agent_id
        if model:
            params["model"] = model
        if status:
            params["status"] = status

        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.get(
                f"{self.base_url}/api/trace/sessions",
                params=params,
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", [])

    async def get_session(self, session_id: str) -> dict:
        """Get a full trace session with span tree."""
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.get(
                f"{self.base_url}/api/trace/sessions/{session_id}",
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", {})

    async def get_cost_summary(
        self, tenant_id: str, period: str = "today"
    ) -> dict:
        """Get token cost summary for a period."""
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/trace/cost-summary",
                params={"tenantId": tenant_id, "period": period},
                headers=self._headers(),
            )
            response.raise_for_status()
            return response.json().get("data", {})
