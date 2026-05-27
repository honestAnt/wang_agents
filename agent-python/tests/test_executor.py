"""Test tool executor."""

import pytest
from app.core.executor.tool_executor import ToolExecutor


class TestToolExecutor:

    @pytest.mark.asyncio
    async def test_execute_returns_dict(self):
        executor = ToolExecutor()
        result = await executor.execute("weather_api", {"city": "Beijing"})
        assert isinstance(result, dict)
        assert "tool_name" in result or "status" in result

    @pytest.mark.asyncio
    async def test_execute_includes_parameters(self):
        executor = ToolExecutor()
        result = await executor.execute("order_lookup", {"order_id": "12345"}, timeout_ms=5000)
        assert "tool_name" in result
        assert result["tool_name"] == "order_lookup"
