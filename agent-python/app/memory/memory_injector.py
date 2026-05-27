"""Memory injector — formats memories for prompt injection."""

import logging

from app.memory.short_term import ShortTermMemory
from app.memory.long_term import LongTermMemory
from app.memory.vector_memory import VectorMemory

logger = logging.getLogger(__name__)


class MemoryInjector:
    """Injects relevant memories and user preferences into the system prompt."""

    def __init__(self):
        self.short_term_memory = ShortTermMemory()
        self.long_term_memory = LongTermMemory()
        self.vector_memory = VectorMemory()

    async def inject(
        self,
        tenant_id: str,
        user_id: str,
        session_id: str,
        system_prompt: str,
    ) -> str:
        """Augment the system prompt with relevant memories.

        - Short-term: recent conversation context from Redis
        - Long-term: user preferences, historical patterns from memory-service
        - Vector: semantically similar past interactions from Qdrant
        """
        memory_parts: list[str] = []

        # Short-term: recent messages in this session
        try:
            recent = await self.short_term_memory.get(session_id, limit=5)
            if recent:
                formatted = "\n".join(
                    f"[{m.get('role', '?')}]: {m.get('content', '')[:200]}"
                    for m in recent
                )
                memory_parts.append(f"## Recent Conversation\n{formatted}")
        except Exception as e:
            logger.warning("Failed to fetch short-term memory: %s", e)

        # Long-term: user preferences and patterns
        try:
            long_term = await self.long_term_memory.retrieve(
                tenant_id=tenant_id,
                user_id=user_id,
                query=system_prompt[:200],
                memory_types=["semantic", "procedural"],
                limit=3,
            )
            if long_term:
                items = "\n".join(
                    f"- {m.get('content', '')[:200]}"
                    for m in long_term
                )
                memory_parts.append(f"## User Preferences & History\n{items}")
        except Exception as e:
            logger.warning("Failed to fetch long-term memory: %s", e)

        # Vector: semantically similar interactions
        try:
            from app.rag.embedding import EmbeddingClient
            from app.llm.model_wrapper import ModelWrapper

            wrapper = ModelWrapper()
            embeddings = await wrapper.embed([system_prompt[:500]])
            if embeddings and embeddings[0]:
                vector_results = await self.vector_memory.search(
                    tenant_id=tenant_id,
                    query_embedding=embeddings[0],
                    limit=3,
                    score_threshold=0.7,
                )
                if vector_results:
                    items = "\n".join(
                        f"- {r.get('content', '')[:200]}"
                        for r in vector_results
                    )
                    memory_parts.append(f"## Related Past Interactions\n{items}")
        except Exception as e:
            logger.warning("Failed to fetch vector memory: %s", e)

        if memory_parts:
            return f"{system_prompt}\n\n## User Context & Memory\n" + "\n\n".join(memory_parts)

        return system_prompt
