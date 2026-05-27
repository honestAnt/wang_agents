"""Tool executor — executes registered tools via HTTP to Java tool-service."""

import logging
import os

import httpx

from app.config import TOOL_SERVICE_URL

logger = logging.getLogger(__name__)


class ToolExecutor:
    """Executes tool calls — resolves tool definitions from Java tool-service and proxies execution."""

    def __init__(self):
        self._tool_service_url = TOOL_SERVICE_URL

    async def execute(
        self,
        tool_name: str,
        parameters: dict,
        timeout_ms: int = 30000,
    ) -> dict:
        """Resolve tool definition from tool-service, then execute.

        Flow:
          1. GET  /api/tools/resolve?name={tool_name}  → get tool endpoint + config
          2. POST <resolved-endpoint>                  → execute the tool
        """
        async with httpx.AsyncClient(timeout=timeout_ms / 1000) as client:
            try:
                resolve_url = f"{self._tool_service_url}/api/tools/resolve"
                resolve_resp = await client.get(resolve_url, params={"name": tool_name})
                resolve_resp.raise_for_status()
                tool_def = resolve_resp.json()
            except Exception as e:
                logger.warning("Tool-service resolve failed for '%s': %s", tool_name, e)
                return {
                    "status": "error",
                    "tool_name": tool_name,
                    "error": f"Tool '{tool_name}' not found: {e}",
                }

            endpoint = tool_def.get("endpoint", "")
            if not endpoint:
                return {
                    "status": "error",
                    "tool_name": tool_name,
                    "error": f"No endpoint configured for tool '{tool_name}'",
                }

            try:
                exec_resp = await client.post(
                    endpoint,
                    json={"tool_name": tool_name, "parameters": parameters},
                )
                exec_resp.raise_for_status()
                return exec_resp.json()
            except Exception as e:
                logger.error("Tool execution failed for '%s': %s", tool_name, e)
                return {
                    "status": "error",
                    "tool_name": tool_name,
                    "parameters": parameters,
                    "error": str(e),
                }
