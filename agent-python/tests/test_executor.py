"""Test tool executor."""

import pytest
from app.core.executor.tool_executor import ToolExecutor


class TestToolExecutor:

    @pytest.mark.asyncio
    async def test_execute_returns_dict(self):
        executor = ToolExecutor()
        result = await executor.execute("weather_api", {"city": "Beijing"})
        assert isinstance(result, dict)
        assert result["tool_name"] == "weather_api"

    @pytest.mark.asyncio
    async def test_execute_includes_parameters(self):
        executor = ToolExecutor()
        result = await executor.execute("order_lookup", {"order_id": "12345"}, timeout_ms=5000)
        assert result["parameters"]["order_id"] == "12345"
        assert result["status"] == "placeholder"
