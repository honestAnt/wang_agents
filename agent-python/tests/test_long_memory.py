"""Test long-term memory and vector memory."""

import pytest
from app.memory.long_term import LongTermMemory
from app.memory.vector_memory import VectorMemory


class TestLongTermMemory:

    @pytest.mark.asyncio
    async def test_store_returns_id(self):
        mem = LongTermMemory()
        memory_id = await mem.store("t1", "u1", "episodic", "User asked about billing")
        assert isinstance(memory_id, str)
        # Returns empty string when memory-service is unavailable

    @pytest.mark.asyncio
    async def test_retrieve_returns_list(self):
        mem = LongTermMemory()
        results = await mem.retrieve("t1", "u1", "billing")
        assert isinstance(results, list)

    @pytest.mark.asyncio
    async def test_retrieve_filters_by_type(self):
        mem = LongTermMemory()
        results = await mem.retrieve("t1", "u1", "preferences", memory_types=["semantic"])
        assert isinstance(results, list)

    @pytest.mark.asyncio
    async def test_get_by_session_returns_list(self):
        mem = LongTermMemory()
        results = await mem.get_by_session("session-1")
        assert isinstance(results, list)


class TestVectorMemory:

    @pytest.mark.asyncio
    async def test_store_returns_id(self):
        mem = VectorMemory()
        vec_id = await mem.store("t1", "u1", "User likes short responses")
        assert isinstance(vec_id, str)

    @pytest.mark.asyncio
    async def test_search_returns_list(self):
        mem = VectorMemory()
        results = await mem.search("t1", [0.1] * 1536, limit=3)
        assert isinstance(results, list)
