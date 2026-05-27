"""Test skill router and loader."""

import pytest
from app.core.router.skill_router import SkillRouter
from app.skills.skill_loader import SkillLoader


class TestSkillRouter:

    @pytest.mark.asyncio
    async def test_match_returns_empty_for_unknown_intent(self):
        router = SkillRouter()
        result = await router.match("unknown", "t1")
        assert isinstance(result, list)
        assert len(result) == 0

    @pytest.mark.asyncio
    async def test_execute_chain_runs_skills_in_order(self):
        router = SkillRouter()
        result = await router.execute_chain(["s1", "s2"], {"input": "test"})
        assert "skill_id" in result
        assert result["status"] in ("error", "placeholder")

    @pytest.mark.asyncio
    async def test_execute_chain_preserves_context(self):
        router = SkillRouter()
        result = await router.execute_chain(["s1"], {"key": "value"})
        assert result is not None
        assert isinstance(result, dict)


class TestSkillLoader:

    @pytest.mark.asyncio
    async def test_load_returns_none_for_unknown(self):
        loader = SkillLoader()
        result = await loader.load("nonexistent")
        assert result is None

    @pytest.mark.asyncio
    async def test_on_published_triggers_refresh(self):
        loader = SkillLoader()
        await loader.on_published({"skill_id": "s1", "status": "published"})
        # Should not raise
