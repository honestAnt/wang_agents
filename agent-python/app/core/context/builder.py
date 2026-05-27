"""Context builder — assembles system prompt with Memory, RAG, Skills, and Tools."""

import logging

from app.memory.memory_injector import MemoryInjector
from app.rag.kb_manager import KBManager
from app.core.router.skill_router import SkillRouter

logger = logging.getLogger(__name__)


class ContextBuilder:
    """Assembles the full context for an LLM call from all available sources.

    Injects:
      - Memory: short-term conversation + long-term preferences + vector similarity
      - RAG: relevant knowledge base chunks via KBManager
      - Skills: matched skills from skill-service
    """

    DEFAULT_SYSTEM_PROMPT = (
        "You are an enterprise AI assistant. You have access to the organization's "
        "knowledge base, tools, and skills. Answer accurately based on the provided "
        "context. If you don't know something, say so rather than guessing."
    )

    def __init__(self):
        self._memory = MemoryInjector()
        self._kb = KBManager()
        self._skill_router = SkillRouter()

    async def build(
        self,
        tenant_id: str,
        user_message: str,
        session_id: str,
        agent_id: str | None = None,
    ) -> dict:
        """Build the complete prompt context.

        Returns a dict with:
          - system_prompt: final augmented system prompt
          - messages: conversation history
          - rag_context: retrieved knowledge base chunks
          - available_skills: matched skills
          - available_tools: tool definitions
        """
        system_prompt = self.DEFAULT_SYSTEM_PROMPT

        # 1. Inject memory context (short-term + long-term + vector)
        augmented_prompt = system_prompt
        try:
            augmented_prompt = await self._memory.inject(
                tenant_id=tenant_id,
                user_id="default",  # TODO: extract from auth context
                session_id=session_id,
                system_prompt=system_prompt,
            )
        except Exception as e:
            logger.warning("Memory injection failed: %s", e)

        # 2. Retrieve RAG context from knowledge base
        rag_context: list[dict] = []
        try:
            rag_context = await self._kb.search(
                kb_id=f"{tenant_id}_default",
                tenant_id=tenant_id,
                query=user_message,
                top_k=3,
                enable_rerank=True,
            )
        except Exception as e:
            logger.warning("RAG retrieval failed: %s", e)

        # 3. Match skills for this tenant
        available_skills: list[dict] = []
        try:
            available_skills = await self._skill_router.match("chat", tenant_id)
        except Exception as e:
            logger.warning("Skill matching failed: %s", e)

        # 4. Augment system prompt with RAG context
        if rag_context:
            rag_section = self._format_rag_context(rag_context)
            augmented_prompt = f"{augmented_prompt}\n\n## Knowledge Base Context\n{rag_section}"

        # 5. Get conversation history from short-term memory
        messages: list[dict] = []
        try:
            history = await self._memory.short_term_memory.get(session_id, limit=10)
            if history:
                messages = history
        except Exception as e:
            logger.warning("Failed to load conversation history: %s", e)

        return {
            "system_prompt": augmented_prompt,
            "messages": messages,
            "rag_context": rag_context,
            "available_skills": available_skills,
            "available_tools": [],
        }

    def _format_rag_context(self, chunks: list[dict]) -> str:
        """Format retrieved chunks into a prompt section."""
        parts: list[str] = []
        for i, chunk in enumerate(chunks, 1):
            content = chunk.get("content", "")[:500]
            source = chunk.get("metadata", {}).get("source", "unknown")
            score = chunk.get("score", 0)
            parts.append(f"[{i}] (score: {score:.2f}, source: {source})\n{content}")
        return "\n\n".join(parts)
