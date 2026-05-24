"""Temporal Worker — polls task queues and executes activities/workflows.

Usage:
    python -m app.core.workflow.worker

The worker connects to the Temporal server and registers all activities
and workflows. In production, workers are deployed as separate processes
or containers that scale independently.
"""

import asyncio
import os
import sys

from temporalio.client import Client
from temporalio.worker import Worker as TemporalWorker


async def create_worker(task_queue: str = "agent-task-queue") -> TemporalWorker:
    """Create and configure a Temporal worker with all registered workflows and activities."""

    temporal_host = os.getenv("TEMPORAL_HOST", "localhost:7233")
    client = await Client.connect(temporal_host)

    # Lazy imports to avoid loading heavy deps at module level
    from app.core.workflow.activities import (
        analysis,
        approval_request,
        data_collection,
        llm_call,
        notification,
        rag_retrieval,
        report_generation,
        tool_execution,
    )
    from app.core.workflow.saga import SagaAgentWorkflow
    from app.core.workflow.workflows import (
        AgentPipelineWorkflow,
        ApprovalWorkflow,
        DataCollectionWorkflow,
    )

    worker = TemporalWorker(
        client,
        task_queue=task_queue,
        workflows=[DataCollectionWorkflow, AgentPipelineWorkflow, ApprovalWorkflow, SagaAgentWorkflow],
        activities=[
            rag_retrieval,
            llm_call,
            tool_execution,
            data_collection,
            analysis,
            report_generation,
            approval_request,
            notification,
        ],
    )

    return worker


async def run_worker(task_queue: str = "agent-task-queue") -> None:
    """Run the Temporal worker, blocking until interrupted."""
    worker = await create_worker(task_queue)
    print(f"Temporal worker started on task queue: {task_queue}")
    try:
        await worker.run()
    except asyncio.CancelledError:
        print("Worker shutting down...")


if __name__ == "__main__":
    task_queue = sys.argv[1] if len(sys.argv) > 1 else "agent-task-queue"
    asyncio.run(run_worker(task_queue))
