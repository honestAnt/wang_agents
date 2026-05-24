"""Test intent router classification."""

import pytest
from app.core.router.intent_router import IntentRouter


@pytest.fixture
def router():
    return IntentRouter()


@pytest.mark.asyncio
async def test_routes_code_query(router):
    assert await router.route("fix the bug in my function") == "code"
    assert await router.route("implement an API endpoint") == "code"


@pytest.mark.asyncio
async def test_routes_analysis(router):
    assert await router.route("analyze this data") == "analysis"
    assert await router.route("create a chart") == "analysis"


@pytest.mark.asyncio
async def test_routes_search(router):
    assert await router.route("search for documents") == "search"
    assert await router.route("find meeting notes") == "search"


@pytest.mark.asyncio
async def test_routes_translation(router):
    assert await router.route("translate this to Chinese") == "translation"


@pytest.mark.asyncio
async def test_routes_summarization(router):
    assert await router.route("summarize this article") == "summarization"


@pytest.mark.asyncio
async def test_routes_data_extraction(router):
    assert await router.route("extract data from PDF") == "data_extraction"


@pytest.mark.asyncio
async def test_routes_customer_service(router):
    assert await router.route("I have a problem") == "customer_service"


@pytest.mark.asyncio
async def test_defaults_to_chat(router):
    assert await router.route("hello how are you") == "chat"
