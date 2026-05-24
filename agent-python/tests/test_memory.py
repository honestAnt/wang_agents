"""Test memory system components."""

import pytest
from app.memory.short_term import ShortTermMemory
from app.memory.memory_injector import MemoryInjector


class TestShortTermMemory:

    @pytest.mark.asyncio
    async def test_add_and_retrieve(self):
        mem = ShortTermMemory()
        await mem.add("s1", {"role": "user", "content": "hello"})
        await mem.add("s1", {"role": "assistant", "content": "hi!"})

        messages = await mem.get("s1")
        assert len(messages) == 2
        assert messages[0]["content"] == "hello"

    @pytest.mark.asyncio
    async def test_limit_respects_max(self):
        mem = ShortTermMemory()
        for i in range(30):
            await mem.add("s2", {"role": "user", "content": f"msg{i}"})

        messages = await mem.get("s2", limit=10)
        assert len(messages) == 10
        # Should return last 10 messages
        assert messages[-1]["content"] == "msg29"

    @pytest.mark.asyncio
    async def test_sliding_window(self):
        mem = ShortTermMemory()
        for i in range(25):
            await mem.add("s3", {"role": "user", "content": f"msg{i}"})

        messages = await mem.get("s3", limit=30)
        # MAX_WINDOW is 20
        assert len(messages) == 20
        assert messages[0]["content"] == "msg5"  # oldest in window


class TestMemoryInjector:

    @pytest.mark.asyncio
    async def test_inject_returns_prompt_when_no_memories(self):
        injector = MemoryInjector()
        result = await injector.inject("t1", "u1", "s1", "You are helpful.")
        assert result == "You are helpful."
