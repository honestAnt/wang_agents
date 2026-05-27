"""Chat API — SSE streaming endpoint and available models."""

import asyncio

from fastapi import APIRouter, Header, Request
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from app.core.agent_engine.engine import AgentEngine
from app.llm.model_wrapper import ModelWrapper
from app.trace.tracer import TraceContext, tracer

router = APIRouter()


class ChatRequest(BaseModel):
    session_id: str | None = None
    agent_id: str | None = None
    model: str | None = None
    message: str
    temperature: float = 0.7
    max_tokens: int = 4096


@router.post("/chat")
async def chat(
    request: ChatRequest,
    x_tenant_id: str = Header("", alias="X-Tenant-Id"),
    x_trace_id: str = Header(None, alias="X-Trace-Id"),
):
    """Stream agent response via SSE."""
    trace_id = x_trace_id or TraceContext.generate()

    async def event_stream():
        engine = AgentEngine()
        with tracer.start_span("agent.chat", trace_id=trace_id) as span:
            span.set_attribute("tenant_id", x_tenant_id)
            span.set_attribute("agent_id", request.agent_id or "default")
            span.set_attribute("model", request.model or "default")

            async for event in engine.run(
                tenant_id=x_tenant_id,
                user_message=request.message,
                session_id=request.session_id,
                agent_id=request.agent_id,
                model=request.model,
            ):
                yield event

    return EventSourceResponse(event_stream(), sep="\n")


@router.get("/models")
async def list_models():
    """Return available models from the provider registry."""
    models = []
    for name, cfg in ModelWrapper.PROVIDER_REGISTRY.items():
        input_price, output_price = ModelWrapper.COST_TABLE.get(name, ModelWrapper.DEFAULT_COST)
        models.append({
            "name": name,
            "provider": cfg.provider,
            "inputPrice": input_price,
            "outputPrice": output_price,
        })
    return {"models": models}
