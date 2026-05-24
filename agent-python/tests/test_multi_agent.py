"""Test multi-agent coordinator."""

import pytest
from app.agents.multi_agent.coordinator import CoordinatorAgent


@pytest.fixture
def coordinator():
    return CoordinatorAgent()


class TestCoordinatorDecompose:

    @pytest.mark.asyncio
    async def test_decompose_search_task(self, coordinator):
        sub_tasks = await coordinator.decompose("find documents about compliance")
        assert len(sub_tasks) >= 1
        assert any(t["agent"] == "search" for t in sub_tasks)

    @pytest.mark.asyncio
    async def test_decompose_analysis_task(self, coordinator):
        sub_tasks = await coordinator.decompose("analyze the sales data and create a chart")
        agents = [t["agent"] for t in sub_tasks]
        assert "analysis" in agents

    @pytest.mark.asyncio
    async def test_decompose_report_task(self, coordinator):
        sub_tasks = await coordinator.decompose("write a summary report of our findings")
        agents = [t["agent"] for t in sub_tasks]
        assert "report" in agents

    @pytest.mark.asyncio
    async def test_decompose_complex_task(self, coordinator):
        sub_tasks = await coordinator.decompose(
            "find our Q3 financial data, analyze trends, and write a report"
        )
        assert len(sub_tasks) >= 2
        # Should be sorted by priority
        assert sub_tasks[0]["priority"] <= sub_tasks[-1]["priority"]

    @pytest.mark.asyncio
    async def test_decompose_default_fallback(self, coordinator):
        sub_tasks = await coordinator.decompose("hello")
        assert len(sub_tasks) >= 2  # search + report
        assert any(t["agent"] == "search" for t in sub_tasks)


class TestCoordinatorDispatch:

    @pytest.mark.asyncio
    async def test_dispatch_returns_results(self, coordinator):
        sub_tasks = [{"agent": "search", "task": "find docs", "priority": 1}]
        results = await coordinator.dispatch(sub_tasks)
        assert len(results) == 1
        assert results[0]["agent"] == "search"
        assert "result" in results[0]

    @pytest.mark.asyncio
    async def test_dispatch_multiple_agents(self, coordinator):
        sub_tasks = [
            {"agent": "search", "task": "find", "priority": 1},
            {"agent": "report", "task": "report", "priority": 2},
        ]
        results = await coordinator.dispatch(sub_tasks)
        assert len(results) == 2
        assert results[0]["agent"] == "search"
        assert results[1]["agent"] == "report"


class TestCoordinatorAggregate:

    @pytest.mark.asyncio
    async def test_aggregate_combines_results(self, coordinator):
        results = [
            {"agent": "search", "result": "Found 5 docs"},
            {"agent": "report", "result": "Report generated"},
        ]
        output = await coordinator.aggregate(results)
        assert "search" in output
        assert "report" in output
        assert "Found 5 docs" in output


class TestCoordinatorRun:

    @pytest.mark.asyncio
    async def test_run_full_pipeline(self, coordinator):
        output = await coordinator.run("find documents about compliance")
        assert isinstance(output, str)
        assert len(output) > 0
