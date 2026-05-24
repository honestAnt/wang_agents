"""Skill router — intent-based skill matching and chain execution."""


class SkillRouter:
    """Routes user intent to registered skills, supports multi-skill chaining."""

    async def match(self, intent: str, tenant_id: str) -> list[dict]:
        """Find skills matching the given intent for a tenant.

        In production, calls Java skill-service to query published skills
        that match the intent category. Supports multi-skill chains.
        """
        # Placeholder: return empty list — production queries skill-service
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
        """Execute a single skill with its prompt template and tool chain."""
        return {"skill_id": skill_id, "status": "placeholder"}
