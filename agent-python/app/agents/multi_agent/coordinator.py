"""Multi-Agent Coordinator — decomposes tasks via LLM and dispatches to sub-agents."""

import json
import logging
import os

logger = logging.getLogger(__name__)


class CoordinatorAgent:
    """Orchestrates multi-agent execution: decompose → dispatch → aggregate.

    Coordinates SearchAgent, AnalysisAgent, and ReportAgent to complete
    complex tasks that require multiple AI capabilities.
    """

    AGENT_TYPES = ["search", "analysis", "report"]

    DECOMPOSITION_PROMPT = (
        "Decompose the following task into sub-tasks for specialized agents.\n"
        "Available agent types: search, analysis, report.\n"
        "Respond with a JSON array of objects: [{\"agent\": \"<type>\", \"task\": \"<sub-task description>\", \"priority\": <int>}].\n"
        "Order by priority (1 = first). If a task doesn't need a certain agent type, skip it.\n\n"
        "Task: {task}\n\nSub-tasks (JSON only):"
    )

    async def decompose(self, task: str) -> list[dict]:
        """Decompose a complex task into sub-tasks using LLM classification."""
        # Fast path: simple keyword-based heuristic
        task_lower = task.lower()
        sub_tasks = []
        if any(kw in task_lower for kw in ["find", "search", "lookup", "retrieve"]):
            sub_tasks.append({"agent": "search", "task": task, "priority": 1})
        if any(kw in task_lower for kw in ["analyze", "analysis", "calculate", "compare"]):
            sub_tasks.append({"agent": "analysis", "task": task, "priority": 2})
        if any(kw in task_lower for kw in ["report", "summarize", "present", "write"]):
            sub_tasks.append({"agent": "report", "task": task, "priority": 3})

        # If heuristics found multiple sub-tasks, return them
        if len(sub_tasks) >= 2:
            return sorted(sub_tasks, key=lambda s: s["priority"])

        # For complex/ambiguous tasks, use LLM decomposition
        try:
            from app.llm.model_wrapper import ModelWrapper

            wrapper = ModelWrapper()
            prompt = self.DECOMPOSITION_PROMPT.format(task=task[:2000])
            result = ""
            async for chunk in wrapper.chat(
                messages=[{"role": "user", "content": prompt}],
                model=os.getenv("COORDINATOR_MODEL", "claude-haiku-4-5-20251001"),
                temperature=0.0,
                max_tokens=500,
                stream=False,
            ):
                result += chunk

            # Extract JSON from response
            json_start = result.find("[")
            json_end = result.rfind("]") + 1
            if json_start >= 0 and json_end > json_start:
                parsed = json.loads(result[json_start:json_end])
                if isinstance(parsed, list) and parsed:
                    return sorted(parsed, key=lambda s: s.get("priority", 99))
        except Exception as e:
            logger.warning("LLM decomposition failed, using fallback: %s", e)

        # Fallback: default to search + report
        if not sub_tasks:
            sub_tasks = [
                {"agent": "search", "task": task, "priority": 1},
                {"agent": "report", "task": task, "priority": 2},
            ]
        return sorted(sub_tasks, key=lambda s: s["priority"])

    async def dispatch(self, sub_tasks: list[dict]) -> list[dict]:
        """Dispatch sub-tasks to specialized agents and collect results."""
        results = []
        for sub in sub_tasks:
            result = await self._run_agent(sub["agent"], sub["task"])
            results.append({"agent": sub["agent"], "result": result})
        return results

    async def aggregate(self, results: list[dict]) -> str:
        """Aggregate results from all sub-agents into a unified response."""
        parts = [f"[{r['agent']}] {r['result']}" for r in results]
        return "\n".join(parts)

    async def run(self, task: str) -> str:
        """Execute the full multi-agent pipeline."""
        sub_tasks = await self.decompose(task)
        results = await self.dispatch(sub_tasks)
        return await self.aggregate(results)

    async def _run_agent(self, agent_type: str, task: str) -> str:
        """Execute a single specialized agent via the Agent Engine."""
        try:
            from app.core.agent_engine.engine import AgentEngine

            engine = AgentEngine()
            full_response = ""
            async for event in engine.run(
                tenant_id="default",
                user_message=task,
            ):
                if event.get("event") == "delta":
                    data = json.loads(event.get("data", "{}"))
                    full_response += data.get("content", "")
            return full_response if full_response else f"[{agent_type}] completed: {task[:100]}"
        except Exception as e:
            logger.error("Agent '%s' execution failed: %s", agent_type, e)
            return f"[{agent_type}] error: {e}"
