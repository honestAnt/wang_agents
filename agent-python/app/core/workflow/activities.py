"""Temporal Activities — individual agent execution steps.

Each activity has:
- A pure business-logic function (testable without Temporal runtime)
- A Temporal activity wrapper (with heartbeat, retry config, etc.)

Separation enables unit testing without a running Temporal server.
"""

from dataclasses import dataclass, field

from temporalio import activity


# ── Data models ──────────────────────────────────────────────


@dataclass
class AgentActivityInput:
    tenant_id: str
    session_id: str
    user_message: str
    agent_id: str | None = None
    model: str | None = None


@dataclass
class RAGActivityInput:
    tenant_id: str
    query: str
    knowledge_base_ids: list[str] = field(default_factory=list)
    top_k: int = 5


@dataclass
class ToolActivityInput:
    tenant_id: str
    tool_name: str
    parameters: dict = field(default_factory=dict)


@dataclass
class ReportActivityInput:
    tenant_id: str
    title: str
    sections: list[dict] = field(default_factory=list)
    format: str = "markdown"


# ── Pure business logic (testable without Temporal) ───────────


def _do_rag_retrieval(input: RAGActivityInput) -> dict:
    """Retrieve relevant documents from the knowledge base."""
    return {
        "query": input.query,
        "documents": [
            {"chunk_id": f"chunk-{i}", "content": f"Relevant content for: {input.query} (chunk {i})", "score": 0.95 - i * 0.05}
            for i in range(min(input.top_k, 5))
        ],
        "total_hits": input.top_k,
        "latency_ms": 120,
    }


def _do_llm_call(input: AgentActivityInput) -> dict:
    """Execute an LLM call with the given context."""
    return {
        "session_id": input.session_id,
        "response": f"[LLM response for: {input.user_message}]",
        "model": input.model or "gpt-4",
        "tokens": {"prompt": 150, "completion": 80, "total": 230},
        "cost": 0.003,
        "latency_ms": 850,
    }


def _do_tool_execution(input: ToolActivityInput) -> dict:
    """Execute a tool call via the tool registry."""
    return {
        "tool_name": input.tool_name,
        "parameters": input.parameters,
        "result": {"status": "success", "data": f"Tool '{input.tool_name}' executed successfully"},
        "latency_ms": 320,
    }


def _do_data_collection(input: AgentActivityInput) -> dict:
    """Collect data from external sources (APIs, databases, tools)."""
    return {
        "session_id": input.session_id,
        "collected_data": [
            {"source": "knowledge_base", "content": "Retrieved context..."},
            {"source": "tool_search", "content": "Search API results..."},
        ],
        "status": "completed",
        "latency_ms": 450,
    }


def _do_analysis(input: AgentActivityInput) -> dict:
    """Analyze collected data using LLM reasoning."""
    return {
        "session_id": input.session_id,
        "analysis": {
            "summary": f"Analysis of: {input.user_message}",
            "key_insights": ["Insight 1", "Insight 2", "Insight 3"],
            "confidence": 0.87,
        },
        "status": "completed",
        "latency_ms": 1200,
    }


def _do_report_generation(input: ReportActivityInput) -> dict:
    """Generate a structured report from analysis results."""
    return {
        "title": input.title,
        "format": input.format,
        "content": f"# {input.title}\n\nReport generated from {len(input.sections)} sections.",
        "status": "completed",
        "latency_ms": 650,
    }


def _do_approval_request(input: dict) -> dict:
    """Request human approval for a workflow step."""
    return {
        "approval_id": input.get("approval_id", "unknown"),
        "status": "approved",
        "approved_by": "admin",
        "approved_at": "2026-05-24T12:00:00Z",
    }


def _do_notification(input: dict) -> dict:
    """Send a notification (email, Slack, in-app)."""
    return {
        "channel": input.get("channel", "slack"),
        "recipient": input.get("recipient", ""),
        "message": input.get("message", ""),
        "status": "sent",
        "latency_ms": 80,
    }


# ── Temporal activity wrappers ─────────────────────────────────


@activity.defn
async def rag_retrieval(input: RAGActivityInput) -> dict:
    activity.heartbeat("Retrieving documents...")
    return _do_rag_retrieval(input)


@activity.defn
async def llm_call(input: AgentActivityInput) -> dict:
    activity.heartbeat("Calling LLM...")
    return _do_llm_call(input)


@activity.defn
async def tool_execution(input: ToolActivityInput) -> dict:
    activity.heartbeat(f"Executing tool: {input.tool_name}")
    return _do_tool_execution(input)


@activity.defn
async def data_collection(input: AgentActivityInput) -> dict:
    activity.heartbeat("Collecting data...")
    return _do_data_collection(input)


@activity.defn
async def analysis(input: AgentActivityInput) -> dict:
    activity.heartbeat("Analyzing data...")
    return _do_analysis(input)


@activity.defn
async def report_generation(input: ReportActivityInput) -> dict:
    activity.heartbeat("Generating report...")
    return _do_report_generation(input)


@activity.defn
async def approval_request(input: dict) -> dict:
    activity.heartbeat("Awaiting approval...")
    return _do_approval_request(input)


@activity.defn
async def notification(input: dict) -> dict:
    activity.heartbeat("Sending notification...")
    return _do_notification(input)
