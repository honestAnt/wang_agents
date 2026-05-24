"""Test intelligent model router — routing, cost estimation, fallback, statistics."""

import pytest
from app.llm.model_router import ModelRouter, RouteDecision, RouteStats


@pytest.fixture
def router():
    r = ModelRouter()
    yield r
    r.reset_stats()


class TestLegacySelect:

    @pytest.mark.asyncio
    async def test_uses_preferred_model(self, router):
        result = await router.select("t1", "chat", preferred_model="gpt-4.1")
        assert result == "gpt-4.1"

    @pytest.mark.asyncio
    async def test_code_intent_uses_gpt4(self, router):
        result = await router.select("t1", "code")
        assert result in ("gpt-4.1", "claude-sonnet-4-6")

    @pytest.mark.asyncio
    async def test_chat_intent_uses_cheaper_model(self, router):
        result = await router.select("t1", "chat")
        assert result in ("claude-haiku-4-5-20251001", "gpt-4.1-mini")

    @pytest.mark.asyncio
    async def test_search_intent_uses_haiku(self, router):
        result = await router.select("t1", "search")
        assert result in ("gpt-4.1-mini", "claude-haiku-4-5-20251001", "gemini-2.0-flash")

    @pytest.mark.asyncio
    async def test_translation_intent_uses_qwen_or_deepseek(self, router):
        result = await router.select("t1", "translation")
        assert result in ("deepseek-v3", "qwen-max")

    @pytest.mark.asyncio
    async def test_unknown_intent_falls_back(self, router):
        result = await router.select("t1", "unknown_task")
        assert result == "gpt-4.1"


class TestRoute:

    @pytest.mark.asyncio
    async def test_route_code_high_complexity(self, router):
        decision = await router.route(
            "implement a distributed microservice with event sourcing and saga patterns")
        assert decision.model in ("gpt-4.1", "claude-sonnet-4-6")
        assert len(decision.fallback_chain) >= 1
        assert decision.complexity in ("high", "medium")

    @pytest.mark.asyncio
    async def test_route_chat_low_complexity(self, router):
        decision = await router.route("hello, quick question")
        assert decision.task_type in ("chat", "unknown")
        assert decision.estimated_cost > 0

    @pytest.mark.asyncio
    async def test_route_with_preferred_model(self, router):
        decision = await router.route("analyze this data", preferred_model="deepseek-v3")
        assert decision.model == "deepseek-v3"
        assert len(decision.fallback_chain) >= 1

    @pytest.mark.asyncio
    async def test_route_builds_fallback_chain(self, router):
        decision = await router.route("write complex enterprise code")
        assert isinstance(decision.fallback_chain, list)
        assert decision.model not in decision.fallback_chain

    @pytest.mark.asyncio
    async def test_route_with_budget(self, router):
        decision = await router.route("hello world", budget_limit=0.00001)
        assert isinstance(decision.model, str)

    @pytest.mark.asyncio
    async def test_route_returns_route_decision(self, router):
        decision = await router.route("summarize this article")
        assert isinstance(decision, RouteDecision)
        assert isinstance(decision.reason, str)
        assert decision.estimated_tokens > 0


class TestCostEstimation:

    def test_estimate_cost_gpt4(self, router):
        cost = router.estimate_cost("gpt-4.1", 1000)
        assert cost > 0.001
        assert cost < 0.01

    def test_estimate_cost_cheap_model(self, router):
        cost_gpt4 = router.estimate_cost("gpt-4.1", 1000)
        cost_haiku = router.estimate_cost("claude-haiku-4-5-20251001", 1000)
        assert cost_haiku < cost_gpt4

    def test_estimate_cost_unknown_model(self, router):
        cost = router.estimate_cost("nonexistent-model", 1000)
        assert cost > 0

    def test_estimate_all_costs(self, router):
        costs = router.estimate_all_costs(1000)
        assert len(costs) >= 5
        assert "gpt-4.1" in costs
        assert "claude-haiku-4-5-20251001" in costs


class TestFallback:

    def test_mark_unavailable(self, router):
        router.mark_unavailable("gpt-4.1")
        assert "gpt-4.1" in router._unavailable_models

    def test_mark_available(self, router):
        router.mark_unavailable("gpt-4.1")
        router.mark_available("gpt-4.1")
        assert "gpt-4.1" not in router._unavailable_models

    @pytest.mark.asyncio
    async def test_unavailable_model_not_selected(self, router):
        router.mark_unavailable("gpt-4.1")
        router.mark_unavailable("claude-sonnet-4-6")
        decision = await router.route("write code")
        assert decision.model not in ("gpt-4.1", "claude-sonnet-4-6")


class TestStatistics:

    def test_record_and_get_stats(self, router):
        router.record_result("code-high", "gpt-4.1", True, 850.0, 0.003)
        router.record_result("code-high", "gpt-4.1", True, 900.0, 0.003)
        router.record_result("code-high", "claude-sonnet-4-6", False, 2000.0, 0.005)

        stats = router.get_stats()
        assert "code-high" in stats
        assert stats["code-high"]["attempts"] == 3
        assert stats["code-high"]["successes"] == 2
        assert stats["code-high"]["failures"] == 1
        assert 0.6 < stats["code-high"]["success_rate"] < 0.7

    def test_comparison_summary(self, router):
        router.record_result("code-high", "gpt-4.1", True, 500.0, 0.002)
        router.record_result("chat-low", "gemini-2.0-flash", True, 300.0, 0.0005)

        summary = router.get_comparison_summary()
        assert summary["total_attempts"] == 2
        assert summary["overall_success_rate"] == 1.0
        assert "gpt-4.1" in summary["model_usage"]

    def test_reset_stats(self, router):
        router.record_result("code-high", "gpt-4.1", True, 500.0, 0.002)
        router.reset_stats()
        assert len(router.get_stats()) == 0


class TestRouteStats:

    def test_route_stats_defaults(self):
        stats = RouteStats(route_key="test-key")
        assert stats.route_key == "test-key"
        assert stats.success_rate == 1.0
        assert stats.avg_latency_ms == 0.0
        assert stats.avg_cost == 0.0
