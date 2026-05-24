"""Temporal Workflows — multi-step agent pipelines.

Defines reusable workflow templates:
- DataCollectionWorkflow: Collect → Analyze → Report
- AgentPipelineWorkflow: Generic multi-step agent pipeline
- ApprovalWorkflow: Pipeline with human-in-the-loop approval
"""

from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from app.core.workflow.activities import (
        AgentActivityInput,
        RAGActivityInput,
        ReportActivityInput,
        ToolActivityInput,
        analysis,
        approval_request,
        data_collection,
        llm_call,
        notification,
        rag_retrieval,
        report_generation,
        tool_execution,
    )


@workflow.defn
class DataCollectionWorkflow:
    """Data → RAG → Tool → Analysis → Report pipeline.

    Example: "Analyze market trends and generate a report"
    Step 1: Collect data from knowledge base and external tools
    Step 2: Retrieve relevant RAG documents
    Step 3: Execute analysis tools
    Step 4: LLM analysis
    Step 5: Generate report
    """

    @workflow.run
    async def run(self, input: AgentActivityInput) -> dict:
        workflow.logger.info(f"Starting DataCollectionWorkflow for session={input.session_id}")

        # Step 1: Collect data
        collect_result = await workflow.execute_activity(
            data_collection,
            input,
            start_to_close_timeout=timedelta(minutes=5),
            retry_policy={"maximum_attempts": 3, "initial_interval": timedelta(seconds=1)},
        )

        # Step 2: RAG retrieval
        rag_input = RAGActivityInput(tenant_id=input.tenant_id, query=input.user_message)
        rag_result = await workflow.execute_activity(
            rag_retrieval,
            rag_input,
            start_to_close_timeout=timedelta(minutes=2),
            retry_policy={"maximum_attempts": 2},
        )

        # Step 3: Analysis
        analysis_result = await workflow.execute_activity(
            analysis,
            input,
            start_to_close_timeout=timedelta(minutes=5),
            retry_policy={"maximum_attempts": 3},
        )

        # Step 4: Generate report
        report_input = ReportActivityInput(
            tenant_id=input.tenant_id,
            title=f"Analysis Report: {input.user_message[:80]}",
            sections=[collect_result, rag_result, analysis_result],
        )
        report_result = await workflow.execute_activity(
            report_generation,
            report_input,
            start_to_close_timeout=timedelta(minutes=3),
        )

        workflow.logger.info(f"DataCollectionWorkflow completed for session={input.session_id}")
        return {
            "session_id": input.session_id,
            "status": "completed",
            "steps": {
                "data_collection": collect_result,
                "rag_retrieval": rag_result,
                "analysis": analysis_result,
                "report": report_result,
            },
        }


@workflow.defn
class AgentPipelineWorkflow:
    """Generic multi-step agent pipeline.

    Accepts a list of steps (with type and config) and executes them sequentially.
    Each step can be: llm_call, tool_exec, rag_retrieval, or condition (branching).

    Supports dynamic step configuration at runtime.
    """

    @workflow.run
    async def run(self, input: AgentActivityInput, steps: list[dict]) -> dict:
        workflow.logger.info(f"Starting AgentPipelineWorkflow with {len(steps)} steps")

        results = []
        for i, step in enumerate(steps):
            step_type = step.get("type", "llm_call")
            step_name = step.get("name", f"step-{i}")

            workflow.logger.info(f"Executing step {i}: {step_name} ({step_type})")

            try:
                if step_type == "llm_call":
                    step_input = AgentActivityInput(
                        tenant_id=input.tenant_id,
                        session_id=input.session_id,
                        user_message=step.get("prompt", input.user_message),
                        model=step.get("model"),
                    )
                    result = await workflow.execute_activity(
                        llm_call, step_input,
                        start_to_close_timeout=timedelta(minutes=3),
                        retry_policy={"maximum_attempts": 2},
                    )

                elif step_type == "tool_exec":
                    tool_input = ToolActivityInput(
                        tenant_id=input.tenant_id,
                        tool_name=step.get("tool_name", "unknown"),
                        parameters=step.get("parameters", {}),
                    )
                    result = await workflow.execute_activity(
                        tool_execution, tool_input,
                        start_to_close_timeout=timedelta(minutes=2),
                        retry_policy={"maximum_attempts": 2},
                    )

                elif step_type == "rag_retrieval":
                    rag_input = RAGActivityInput(
                        tenant_id=input.tenant_id,
                        query=step.get("query", input.user_message),
                        knowledge_base_ids=step.get("kb_ids", []),
                        top_k=step.get("top_k", 5),
                    )
                    result = await workflow.execute_activity(
                        rag_retrieval, rag_input,
                        start_to_close_timeout=timedelta(minutes=2),
                        retry_policy={"maximum_attempts": 2},
                    )

                else:
                    result = {"error": f"Unknown step type: {step_type}"}

                results.append({"step": step_name, "type": step_type, "result": result, "status": "completed"})

            except Exception as e:
                workflow.logger.error(f"Step {step_name} failed: {e}")
                results.append({"step": step_name, "type": step_type, "result": str(e), "status": "failed"})
                raise

        return {
            "session_id": input.session_id,
            "status": "completed",
            "total_steps": len(steps),
            "results": results,
        }


@workflow.defn
class ApprovalWorkflow:
    """Workflow with human-in-the-loop approval step.

    Use case: Generate content → Request approval → Publish or Revise.
    Supports signals for approve/reject/revision decisions.
    """

    def __init__(self):
        self._approval_result: str | None = None
        self._revision_feedback: str | None = None

    @workflow.signal
    async def approve(self) -> None:
        self._approval_result = "approved"

    @workflow.signal
    async def reject(self, reason: str = "") -> None:
        self._approval_result = "rejected"
        self._revision_feedback = reason

    @workflow.signal
    async def request_revision(self, feedback: str) -> None:
        self._approval_result = "revision"
        self._revision_feedback = feedback

    @workflow.run
    async def run(self, input: AgentActivityInput) -> dict:
        workflow.logger.info(f"Starting ApprovalWorkflow for session={input.session_id}")

        # Step 1: Generate content (LLM)
        gen_result = await workflow.execute_activity(
            llm_call, input,
            start_to_close_timeout=timedelta(minutes=3),
        )

        # Step 2: Wait for human approval
        await workflow.wait_condition(lambda: self._approval_result is not None, timeout=timedelta(hours=72))

        workflow.logger.info(f"Approval decision: {self._approval_result}")

        if self._approval_result == "approved":
            # Step 3: Notify on approval
            await workflow.execute_activity(
                notification,
                {"channel": "slack", "recipient": input.tenant_id, "message": "Content approved and published"},
                start_to_close_timeout=timedelta(seconds=30),
            )
            return {"status": "approved", "content": gen_result, "session_id": input.session_id}

        elif self._approval_result == "revision":
            # Step 3b: Revise based on feedback
            revision_input = AgentActivityInput(
                tenant_id=input.tenant_id,
                session_id=input.session_id,
                user_message=f"Revise based on feedback: {self._revision_feedback}",
                agent_id=input.agent_id,
                model=input.model,
            )
            revised = await workflow.execute_activity(
                llm_call, revision_input,
                start_to_close_timeout=timedelta(minutes=3),
            )
            return {"status": "revised", "original": gen_result, "revised": revised, "session_id": input.session_id}

        else:  # rejected
            return {"status": "rejected", "reason": self._revision_feedback, "session_id": input.session_id}
