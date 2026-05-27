"""Test ContextBuilder integration."""

import pytest
from app.core.context.builder import ContextBuilder


class TestContextBuilder:

    @pytest.mark.asyncio
    async def test_build_returns_required_keys(self):
        builder = ContextBuilder()
        result = await builder.build("t1", "hello world", "session-1")
        assert "system_prompt" in result
        assert "messages" in result
        assert "rag_context" in result
        assert "available_skills" in result
        assert "available_tools" in result

    @pytest.mark.asyncio
    async def test_build_includes_default_prompt(self):
        builder = ContextBuilder()
        result = await builder.build("t1", "test message", "session-1")
        assert "enterprise AI assistant" in result["system_prompt"]

    @pytest.mark.asyncio
    async def test_build_returns_list_types(self):
        builder = ContextBuilder()
        result = await builder.build("t1", "query", "session-1")
        assert isinstance(result["messages"], list)
        assert isinstance(result["rag_context"], list)
        assert isinstance(result["available_skills"], list)
        assert isinstance(result["available_tools"], list)

    @pytest.mark.asyncio
    async def test_build_with_agent_id(self):
        builder = ContextBuilder()
        result = await builder.build("t1", "hello", "session-1", agent_id="agent-1")
        assert "enterprise AI assistant" in result["system_prompt"]

    def test_format_rag_context(self):
        builder = ContextBuilder()
        chunks = [
            {"content": "Machine learning basics", "score": 0.95, "metadata": {"source": "ml.pdf"}},
            {"content": "Deep learning advanced", "score": 0.82, "metadata": {"source": "dl.pdf"}},
        ]
        formatted = builder._format_rag_context(chunks)
        assert "ml.pdf" in formatted
        assert "dl.pdf" in formatted
        assert "0.95" in formatted
        assert "[1]" in formatted
        assert "[2]" in formatted
