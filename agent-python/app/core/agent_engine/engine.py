"""Agent execution engine — reason→act→observe loop."""

import json
import uuid
from collections.abc import AsyncIterator

from app.core.context.builder import ContextBuilder
from app.core.router.intent_router import IntentRouter
from app.llm.model_router import ModelRouter
from app.memory.short_term import ShortTermMemory
from app.trace.tracer import tracer


class AgentEngine:
    """Orchestrates a single agent execution: build context, route intent, call LLM, execute tools."""

    def __init__(self):
        self.context_builder = ContextBuilder()
        self.intent_router = IntentRouter()
        self.model_router = ModelRouter()
        self.short_term_memory = ShortTermMemory()

    async def run(
        self,
        tenant_id: str,
        user_message: str,
        session_id: str | None = None,
        agent_id: str | None = None,
        model: str | None = None,
    ) -> AsyncIterator[dict]:
        """Execute the agent loop, yielding SSE events."""
        sid = session_id or str(uuid.uuid4())

        # Step 1: Build context (system prompt + memory + RAG)
        context = await self.context_builder.build(
            tenant_id=tenant_id,
            user_message=user_message,
            session_id=sid,
            agent_id=agent_id,
        )

        # Step 2: Intent routing
        intent = await self.intent_router.route(user_message)

        # Step 3: Select model
        selected_model = await self.model_router.select(
            tenant_id=tenant_id,
            intent=intent,
            preferred_model=model,
        )

        # Step 4: Reason — call LLM
        with tracer.start_span("llm.call", parent_id=tracer.current_span_id) as span:
            span.set_attribute("model", selected_model)
            span.set_attribute("intent", intent)

            yield {
                "event": "delta",
                "data": json.dumps({"content": f"[Agent thinking... intent={intent}, model={selected_model}]"}),
            }

            # Placeholder for actual LLM streaming
            # In production, call LiteLLM client: response = await litellm_client.chat(...)

            yield {
                "event": "done",
                "data": json.dumps({
                    "session_id": sid,
                    "model": selected_model,
                    "intent": intent,
                }),
            }

        # Save to short-term memory
        await self.short_term_memory.add(sid, {"role": "user", "content": user_message})
