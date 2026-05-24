"""Saga orchestration — compensating transactions for distributed workflows.

When a multi-step workflow fails partway through, the Saga pattern ensures
that already-completed steps are rolled back via compensating activities.

Pattern:
  Step A → Step B → Step C (fails)
  Compensate C → Compensate B → Compensate A (reverse order)
"""

from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from app.core.workflow.activities import (
        AgentActivityInput,
        RAGActivityInput,
        ToolActivityInput,
        data_collection,
        notification,
        rag_retrieval,
        tool_execution,
    )


@workflow.defn
class SagaAgentWorkflow:
    """Agent workflow with Saga compensation.

    Steps are executed sequentially. If any step fails, all previously
    completed steps are compensated (rolled back) in reverse order.

    Each step has:
    - action: the forward activity to execute
    - compensate: the compensating activity (can be None if no rollback needed)
    - compensation_input: input for the compensating activity
    """

    @workflow.run
    async def run(self, input: AgentActivityInput, saga_steps: list[dict]) -> dict:
        workflow.logger.info(f"Starting SagaAgentWorkflow with {len(saga_steps)} steps")

        completed_steps: list[dict] = []

        try:
            for i, step in enumerate(saga_steps):
                step_name = step.get("name", f"step-{i}")
                action_type = step.get("action_type", "tool_exec")
                action_input = step.get("action_input", {})

                workflow.logger.info(f"Saga step {i}: executing {step_name} ({action_type})")

                # Execute the forward action
                result = await self._execute_action(action_type, input, action_input)
                completed_steps.append({
                    "step": step_name,
                    "type": action_type,
                    "result": result,
                    "compensate_type": step.get("compensate_type"),
                    "compensate_input": step.get("compensate_input", {}),
                })

        except Exception as e:
            workflow.logger.error(f"Saga failed at step {len(completed_steps)}: {e}")

            # Compensate in reverse order
            compensation_results = await self._compensate(completed_steps)

            return {
                "session_id": input.session_id,
                "status": "compensated",
                "failed_at_step": len(completed_steps),
                "error": str(e),
                "completed_steps": completed_steps,
                "compensations": compensation_results,
            }

        return {
            "session_id": input.session_id,
            "status": "completed",
            "completed_steps": completed_steps,
        }

    async def _execute_action(self, action_type: str, input: AgentActivityInput, action_input: dict) -> dict:
        """Execute a single saga action."""
        if action_type == "tool_exec":
            tool_input = ToolActivityInput(
                tenant_id=input.tenant_id,
                tool_name=action_input.get("tool_name", "unknown"),
                parameters=action_input.get("parameters", {}),
            )
            return await workflow.execute_activity(
                tool_execution, tool_input,
                start_to_close_timeout=timedelta(minutes=2),
                retry_policy={"maximum_attempts": 3},
            )

        elif action_type == "rag_retrieval":
            rag_input = RAGActivityInput(
                tenant_id=input.tenant_id,
                query=action_input.get("query", input.user_message),
                knowledge_base_ids=action_input.get("kb_ids", []),
            )
            return await workflow.execute_activity(
                rag_retrieval, rag_input,
                start_to_close_timeout=timedelta(minutes=2),
            )

        elif action_type == "data_collection":
            return await workflow.execute_activity(
                data_collection, input,
                start_to_close_timeout=timedelta(minutes=5),
            )

        else:
            raise ValueError(f"Unknown action type: {action_type}")

    async def _compensate(self, completed_steps: list[dict]) -> list[dict]:
        """Execute compensating actions in reverse order."""
        compensation_results = []

        for step in reversed(completed_steps):
            compensate_type = step.get("compensate_type")
            if compensate_type is None:
                compensation_results.append({"step": step["step"], "compensated": False, "reason": "no compensation defined"})
                continue

            compensate_input = step.get("compensate_input", {})
            step_name = step["step"]

            try:
                if compensate_type == "notification":
                    await workflow.execute_activity(
                        notification,
                        {
                            "channel": compensate_input.get("channel", "slack"),
                            "recipient": compensate_input.get("recipient", ""),
                            "message": f"Compensating failed step: {step_name}",
                        },
                        start_to_close_timeout=timedelta(seconds=30),
                    )

                compensation_results.append({"step": step_name, "compensated": True, "type": compensate_type})

            except Exception as e:
                workflow.logger.error(f"Compensation failed for {step_name}: {e}")
                compensation_results.append({"step": step_name, "compensated": False, "error": str(e)})

        return compensation_results
