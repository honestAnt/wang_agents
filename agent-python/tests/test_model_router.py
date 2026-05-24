"""Test model router selection."""

import pytest
from app.llm.model_router import ModelRouter


@pytest.fixture
def router():
    return ModelRouter()


@pytest.mark.asyncio
async def test_uses_preferred_model(router):
    result = await router.select("t1", "chat", preferred_model="gpt-4.1")
    assert result == "gpt-4.1"


@pytest.mark.asyncio
async def test_code_intent_uses_gpt4(router):
    result = await router.select("t1", "code")
    assert result in ("gpt-4.1", "claude-sonnet-4-6")


@pytest.mark.asyncio
async def test_chat_intent_uses_cheaper_model(router):
    result = await router.select("t1", "chat")
    assert result in ("claude-haiku-4-5-20251001", "gpt-4.1-mini")


@pytest.mark.asyncio
async def test_search_intent_uses_haiku(router):
    result = await router.select("t1", "search")
    assert result == "claude-haiku-4-5-20251001"


@pytest.mark.asyncio
async def test_translation_intent_uses_qwen_or_deepseek(router):
    result = await router.select("t1", "translation")
    assert result in ("deepseek-v3", "qwen-max")


@pytest.mark.asyncio
async def test_unknown_intent_falls_back(router):
    result = await router.select("t1", "unknown_task")
    assert result == "gpt-4.1"  # first in fallback chain
