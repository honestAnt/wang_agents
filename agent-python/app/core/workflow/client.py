"""Temporal Client — manages workflow lifecycle.

Provides a high-level API for:
- Starting workflows
- Querying workflow status
- Cancelling workflows
- Retrying failed workflows
- Sending signals (approve/reject) to running workflows
"""

import os
from dataclasses import dataclass
from datetime import timedelta
from typing import Any
from uuid import uuid4

from temporalio.client import Client as TemporalClient
from temporalio.client import WorkflowExecutionStatus, WorkflowHandle
from temporalio.common import RetryPolicy


@dataclass
class WorkflowStartRequest:
    workflow_type: str  # "data_collection", "agent_pipeline", "approval", "saga"
    tenant_id: str
    session_id: str
    user_message: str
    agent_id: str | None = None
    model: str | None = None
    steps: list[dict] | None = None  # For agent_pipeline and saga workflows
    workflow_id: str | None = None
    task_queue: str = "agent-task-queue"


@dataclass
class WorkflowInfo:
    workflow_id: str
    run_id: str
    workflow_type: str
    status: str
    start_time: str | None = None
    result: dict | None = None
    error: str | None = None


class WorkflowClient:
    """Manages Temporal workflow lifecycle for the agent platform."""

    def __init__(self, host: str | None = None):
        self._host = host or os.getenv("TEMPORAL_HOST", "localhost:7233")
        self._client: TemporalClient | None = None

    async def _get_client(self) -> TemporalClient:
        if self._client is None:
            self._client = await TemporalClient.connect(self._host)
        return self._client

    async def start_workflow(self, request: WorkflowStartRequest) -> WorkflowInfo:
        """Start a new workflow execution."""
        client = await self._get_client()
        workflow_id = request.workflow_id or f"wf-{uuid4().hex[:12]}"

        from app.core.workflow.activities import AgentActivityInput
        from app.core.workflow.saga import SagaAgentWorkflow
        from app.core.workflow.workflows import (
            AgentPipelineWorkflow,
            ApprovalWorkflow,
            DataCollectionWorkflow,
        )

        agent_input = AgentActivityInput(
            tenant_id=request.tenant_id,
            session_id=request.session_id,
            user_message=request.user_message,
            agent_id=request.agent_id,
            model=request.model,
        )

        if request.workflow_type == "data_collection":
            handle = await client.start_workflow(
                DataCollectionWorkflow.run,
                agent_input,
                id=workflow_id,
                task_queue=request.task_queue,
                execution_timeout=timedelta(hours=1),
                retry_policy=RetryPolicy(maximum_attempts=1),
            )

        elif request.workflow_type == "agent_pipeline":
            steps = request.steps or []
            handle = await client.start_workflow(
                AgentPipelineWorkflow.run,
                agent_input,
                steps,
                id=workflow_id,
                task_queue=request.task_queue,
                execution_timeout=timedelta(hours=2),
                retry_policy=RetryPolicy(maximum_attempts=1),
            )

        elif request.workflow_type == "approval":
            handle = await client.start_workflow(
                ApprovalWorkflow.run,
                agent_input,
                id=workflow_id,
                task_queue=request.task_queue,
                execution_timeout=timedelta(hours=72),
                retry_policy=RetryPolicy(maximum_attempts=1),
            )

        elif request.workflow_type == "saga":
            steps = request.steps or []
            handle = await client.start_workflow(
                SagaAgentWorkflow.run,
                agent_input,
                steps,
                id=workflow_id,
                task_queue=request.task_queue,
                execution_timeout=timedelta(hours=2),
                retry_policy=RetryPolicy(maximum_attempts=1),
            )

        else:
            raise ValueError(f"Unknown workflow type: {request.workflow_type}")

        return WorkflowInfo(
            workflow_id=workflow_id,
            run_id=handle.result_run_id,
            workflow_type=request.workflow_type,
            status="RUNNING",
        )

    async def get_workflow(self, workflow_id: str) -> WorkflowInfo:
        """Get the current status and result of a workflow."""
        client = await self._get_client()
        handle = client.get_workflow_handle(workflow_id)

        desc = await handle.describe()
        status = desc.status.name

        result = None
        error = None

        if status == "COMPLETED":
            try:
                result = await handle.result()
            except Exception as e:
                error = str(e)
        elif status == "FAILED":
            error = "Workflow execution failed"

        return WorkflowInfo(
            workflow_id=workflow_id,
            run_id=desc.run_id,
            workflow_type=desc.workflow_type,
            status=status,
            start_time=str(desc.start_time) if desc.start_time else None,
            result=result,
            error=error,
        )

    async def cancel_workflow(self, workflow_id: str, reason: str = "") -> dict:
        """Cancel a running workflow."""
        client = await self._get_client()
        handle = client.get_workflow_handle(workflow_id)
        await handle.cancel()
        return {"workflow_id": workflow_id, "status": "CANCELLED", "reason": reason}

    async def retry_workflow(self, workflow_id: str) -> WorkflowInfo:
        """Re-submit a failed workflow with the same parameters."""
        client = await self._get_client()
        handle = client.get_workflow_handle(workflow_id)

        desc = await handle.describe()
        if desc.status != WorkflowExecutionStatus.FAILED:
            raise ValueError(f"Workflow {workflow_id} is not in FAILED state: {desc.status.name}")

        # Query the original run for input
        history = await handle.fetch_history()
        # Re-submit with the same workflow ID (Temporal allows reset)
        await handle.reset(desc.run_id)

        return await self.get_workflow(workflow_id)

    async def send_signal(self, workflow_id: str, signal_name: str, arg: Any = None) -> dict:
        """Send a signal to a running workflow (e.g., approve/reject)."""
        client = await self._get_client()
        handle = client.get_workflow_handle(workflow_id)

        if signal_name == "approve":
            await handle.signal("approve")
        elif signal_name == "reject":
            await handle.signal("reject", arg or "")
        elif signal_name == "request_revision":
            await handle.signal("request_revision", arg or "")
        else:
            raise ValueError(f"Unknown signal: {signal_name}")

        return {"workflow_id": workflow_id, "signal": signal_name, "status": "sent"}

    async def list_workflows(
        self, status_filter: str | None = None, limit: int = 20
    ) -> list[WorkflowInfo]:
        """List recent workflows, optionally filtered by status."""
        client = await self._get_client()

        query = ""
        if status_filter:
            query = f'ExecutionStatus="{status_filter}"'

        workflows = []
        async for wf in client.list_workflows(query=query, page_size=limit):
            workflows.append(WorkflowInfo(
                workflow_id=wf.id,
                run_id=wf.run_id,
                workflow_type=wf.type.name if wf.type else "unknown",
                status=wf.status.name if wf.status else "UNKNOWN",
                start_time=str(wf.start_time) if wf.start_time else None,
            ))

        return workflows

    async def close(self) -> None:
        if self._client:
            await self._client.close()
            self._client = None


# Module-level singleton
_workflow_client: WorkflowClient | None = None


async def get_workflow_client() -> WorkflowClient:
    global _workflow_client
    if _workflow_client is None:
        _workflow_client = WorkflowClient()
    return _workflow_client
