"""Test workflow data models and client API (unit tests, no Temporal server needed)."""

import pytest

from app.core.workflow.activities import AgentActivityInput
from app.core.workflow.client import (
    WorkflowClient,
    WorkflowInfo,
    WorkflowStartRequest,
)


class TestWorkflowStartRequest:

    def test_minimal_request(self):
        req = WorkflowStartRequest(
            workflow_type="data_collection",
            tenant_id="t1",
            session_id="s1",
            user_message="hello",
        )
        assert req.workflow_type == "data_collection"
        assert req.tenant_id == "t1"
        assert req.session_id == "s1"
        assert req.user_message == "hello"
        assert req.agent_id is None
        assert req.model is None
        assert req.steps is None
        assert req.task_queue == "agent-task-queue"

    def test_full_request(self):
        req = WorkflowStartRequest(
            workflow_type="agent_pipeline",
            tenant_id="t1",
            session_id="s1",
            user_message="complex task",
            agent_id="agent-1",
            model="gpt-4",
            steps=[{"type": "llm_call", "prompt": "analyze"}],
            workflow_id="wf-custom",
            task_queue="high-priority",
        )
        assert req.agent_id == "agent-1"
        assert req.model == "gpt-4"
        assert len(req.steps) == 1
        assert req.workflow_id == "wf-custom"
        assert req.task_queue == "high-priority"

    def test_saga_request_with_steps(self):
        req = WorkflowStartRequest(
            workflow_type="saga",
            tenant_id="t1",
            session_id="s1",
            user_message="multi-step saga",
            steps=[
                {
                    "name": "step-1",
                    "action_type": "tool_exec",
                    "action_input": {"tool_name": "api_call"},
                    "compensate_type": "notification",
                    "compensate_input": {"channel": "slack"},
                },
                {
                    "name": "step-2",
                    "action_type": "rag_retrieval",
                    "action_input": {"query": "test"},
                },
            ],
        )
        assert req.workflow_type == "saga"
        assert len(req.steps) == 2
        assert req.steps[0]["compensate_type"] == "notification"


class TestWorkflowInfo:

    def test_workflow_info_creation(self):
        info = WorkflowInfo(
            workflow_id="wf-1",
            run_id="run-1",
            workflow_type="data_collection",
            status="RUNNING",
        )
        assert info.workflow_id == "wf-1"
        assert info.status == "RUNNING"

    def test_workflow_info_completed(self):
        info = WorkflowInfo(
            workflow_id="wf-2",
            run_id="run-2",
            workflow_type="agent_pipeline",
            status="COMPLETED",
            result={"status": "done"},
        )
        assert info.status == "COMPLETED"
        assert info.result == {"status": "done"}


class TestAgentActivityInput:

    def test_activity_input_defaults(self):
        inp = AgentActivityInput(tenant_id="t1", session_id="s1", user_message="hi")
        assert inp.agent_id is None
        assert inp.model is None

    def test_activity_input_with_agent(self):
        inp = AgentActivityInput(tenant_id="t1", session_id="s1", user_message="hi",
                                 agent_id="agent-1", model="claude-3")
        assert inp.agent_id == "agent-1"
        assert inp.model == "claude-3"


class TestWorkflowClientInit:

    def test_client_default_host(self):
        client = WorkflowClient()
        assert client._host == "localhost:7233"

    def test_client_custom_host(self):
        client = WorkflowClient(host="temporal.prod:7233")
        assert client._host == "temporal.prod:7233"
