"""Dynamic skill loader — hot-reload skills from Java skill-service and Kafka events."""

import asyncio
import json
import logging
import os
import time

import httpx

from app.config import SKILL_SERVICE_URL

logger = logging.getLogger(__name__)


class SkillLoader:
    """Loads skill definitions from Java skill-service, watches for updates via Kafka.

    Maintains an in-memory cache with TTL. Subscribes to Kafka topic
    'skill.published' for real-time cache invalidation and hot reload.
    """

    CACHE_TTL = 300  # 5 minutes

    def __init__(self):
        self._skill_service_url = SKILL_SERVICE_URL
        self._skill_cache: dict[str, tuple[dict, float]] = {}  # skill_id → (definition, loaded_at)
        self._kafka_consumer = None
        self._kafka_task: asyncio.Task | None = None

    async def start(self):
        """Start the Kafka consumer for hot-reload events."""
        bootstrap = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        try:
            from kafka import KafkaConsumer
            consumer = KafkaConsumer(
                "skill.published",
                bootstrap_servers=bootstrap,
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                auto_offset_reset="latest",
                group_id="skill-loader-agent-python",
                consumer_timeout_ms=10000,
            )
            self._kafka_consumer = consumer
            self._kafka_task = asyncio.create_task(self._consume_events())
            logger.info("SkillLoader subscribed to Kafka topic 'skill.published'")
        except Exception as e:
            logger.warning("Kafka unavailable for skill hot-reload: %s", e)

    async def load(self, skill_id: str) -> dict | None:
        """Load a skill definition by ID. Uses local cache with TTL."""
        now = time.time()
        cached = self._skill_cache.get(skill_id)
        if cached and (now - cached[1]) < self.CACHE_TTL:
            return cached[0]

        definition = await self._fetch_from_service(skill_id)
        if definition:
            self._skill_cache[skill_id] = (definition, now)
        return definition

    async def refresh(self, skill_id: str) -> dict | None:
        """Force refresh a skill from the skill-service."""
        self._skill_cache.pop(skill_id, None)
        definition = await self._fetch_from_service(skill_id)
        if definition:
            self._skill_cache[skill_id] = (definition, time.time())
        return definition

    async def on_published(self, event: dict) -> None:
        """Handle Kafka 'skill.published' event for hot reload."""
        skill_id = event.get("skill_id")
        if skill_id:
            await self.refresh(skill_id)
            logger.info("Hot-reloaded skill: %s", skill_id)

    async def _fetch_from_service(self, skill_id: str) -> dict | None:
        """Fetch skill definition from Java skill-service."""
        async with httpx.AsyncClient(timeout=10) as client:
            try:
                url = f"{self._skill_service_url}/api/skills/{skill_id}"
                resp = await client.get(url)
                resp.raise_for_status()
                return resp.json()
            except Exception as e:
                logger.warning("Failed to fetch skill %s: %s", skill_id, e)
                return None

    async def _consume_events(self):
        """Background task: consume Kafka events and hot-reload skills."""
        loop = asyncio.get_event_loop()
        while True:
            try:
                records = await loop.run_in_executor(
                    None, lambda: list(self._kafka_consumer.poll(timeout_ms=1000).values())
                )
                for batch in records:
                    for msg in batch:
                        await self.on_published(msg.value)
            except Exception as e:
                logger.error("Kafka consume error: %s", e)
            await asyncio.sleep(1)
