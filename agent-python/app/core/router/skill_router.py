"""Skill router — queries Java skill-service for intent-based skill matching and chaining."""

import logging

import httpx

from app.config import SKILL_SERVICE_URL

logger = logging.getLogger(__name__)


class SkillRouter:
    """Routes user intent to registered skills via Java skill-service, supports multi-skill chaining."""

    def __init__(self):
        self._skill_service_url = SKILL_SERVICE_URL

    async def match(self, intent: str, tenant_id: str) -> list[dict]:
        """Find skills matching the given intent for a tenant.

        Queries Java skill-service for published skills that match the intent category.
        Returns skill definitions including prompt templates and tool chains.
        """
        async with httpx.AsyncClient(timeout=10) as client:
            try:
                url = f"{self._skill_service_url}/api/skills/match"
                resp = await client.get(url, params={"intent": intent, "tenant_id": tenant_id})
                resp.raise_for_status()
                data = resp.json()
                return data.get("skills", data if isinstance(data, list) else [])
            except Exception as e:
                logger.warning("Skill match failed for intent=%s: %s", intent, e)
                return []

    async def execute_chain(self, skill_ids: list[str], context: dict) -> dict:
        """Execute a chain of skills in sequence.

        Each skill's output becomes the next skill's input context.
        """
        result = context
        for skill_id in skill_ids:
            result = await self._execute_skill(skill_id, result)
        return result

    async def _execute_skill(self, skill_id: str, context: dict) -> dict:
        """Execute a single skill via the skill-service execution endpoint."""
        async with httpx.AsyncClient(timeout=60) as client:
            try:
                url = f"{self._skill_service_url}/api/skills/{skill_id}/execute"
                resp = await client.post(url, json=context)
                resp.raise_for_status()
                return resp.json()
            except Exception as e:
                logger.error("Skill execution failed for skill=%s: %s", skill_id, e)
                return {"skill_id": skill_id, "status": "error", "error": str(e)}
