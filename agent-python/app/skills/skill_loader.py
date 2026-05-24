"""Dynamic skill loader — hot-reload skills from Kafka events."""


class SkillLoader:
    """Loads skill definitions from Java skill-service and watches for updates.

    In production, subscribes to Kafka topic 'skill.published' for hot reload.
    """

    def __init__(self):
        self._skill_cache: dict[str, dict] = {}

    async def load(self, skill_id: str) -> dict | None:
        """Load a skill definition by ID. Uses local cache with TTL."""
        return self._skill_cache.get(skill_id)

    async def refresh(self, skill_id: str) -> dict | None:
        """Force refresh a skill from the skill-service."""
        # In production: call Java skill-service GET /api/skills/{id}
        self._skill_cache.pop(skill_id, None)
        return None

    async def on_published(self, event: dict) -> None:
        """Handle Kafka 'skill.published' event for hot reload."""
        skill_id = event.get("skill_id")
        if skill_id:
            await self.refresh(skill_id)
