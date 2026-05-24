"""Workflow API — manage AI workflow lifecycle.

Endpoints:
- POST /api/workflows — Start a new workflow
- GET /api/workflows/{id} — Get workflow status/results
- DELETE /api/workflows/{id} — Cancel a workflow
- POST /api/workflows/{id}/retry — Retry a failed workflow
- POST /api/workflows/{id}/signal — Send signal (approve/reject)
- GET /api/workflows — List workflows
"""

import json

from fastapi import APIRouter, HTTPException, Query

from app.core.workflow.client import WorkflowStartRequest, get_workflow_client

router = APIRouter(prefix="/api/workflows", tags=["workflows"])


@router.post("")
async def start_workflow(request: WorkflowStartRequest):
    """Start a new workflow execution."""
    try:
        client = await get_workflow_client()
        info = await client.start_workflow(request)
        return {
            "workflow_id": info.workflow_id,
            "run_id": info.run_id,
            "workflow_type": info.workflow_type,
            "status": info.status,
        }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to start workflow: {e}") from e


@router.get("/{workflow_id}")
async def get_workflow(workflow_id: str):
    """Get workflow status and results."""
    try:
        client = await get_workflow_client()
        info = await client.get_workflow(workflow_id)
        return {
            "workflow_id": info.workflow_id,
            "run_id": info.run_id,
            "workflow_type": info.workflow_type,
            "status": info.status,
            "start_time": info.start_time,
            "result": info.result,
            "error": info.error,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get workflow: {e}") from e


@router.delete("/{workflow_id}")
async def cancel_workflow(workflow_id: str, reason: str = ""):
    """Cancel a running workflow."""
    try:
        client = await get_workflow_client()
        result = await client.cancel_workflow(workflow_id, reason)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to cancel workflow: {e}") from e


@router.post("/{workflow_id}/retry")
async def retry_workflow(workflow_id: str):
    """Retry a failed workflow."""
    try:
        client = await get_workflow_client()
        info = await client.retry_workflow(workflow_id)
        return {
            "workflow_id": info.workflow_id,
            "status": info.status,
        }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to retry workflow: {e}") from e


@router.post("/{workflow_id}/signal")
async def send_signal(workflow_id: str, signal_name: str = Query(...), payload: str = Query(default="")):
    """Send a signal to a running workflow (approve, reject, request_revision)."""
    try:
        client = await get_workflow_client()
        result = await client.send_signal(workflow_id, signal_name, payload if payload else None)
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send signal: {e}") from e


@router.get("")
async def list_workflows(
    status: str | None = Query(default=None, description="Filter by status: RUNNING, COMPLETED, FAILED, CANCELLED"),
    limit: int = Query(default=20, ge=1, le=100),
):
    """List workflows, optionally filtered by status."""
    try:
        client = await get_workflow_client()
        workflows = await client.list_workflows(status_filter=status, limit=limit)
        return {
            "total": len(workflows),
            "workflows": [
                {
                    "workflow_id": wf.workflow_id,
                    "run_id": wf.run_id,
                    "workflow_type": wf.workflow_type,
                    "status": wf.status,
                    "start_time": wf.start_time,
                }
                for wf in workflows
            ],
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list workflows: {e}") from e
