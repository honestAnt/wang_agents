"""AI Workflow Engine — Temporal-based orchestration for long-running agent pipelines.

Provides:
- activities: Agent execution steps wrapped as Temporal Activities
- workflows: Multi-step agent pipeline definitions (Data → Analyze → Report → Approve)
- saga: Compensation workflow for distributed rollback
- worker: Temporal Worker that polls task queues
- client: Temporal Client for workflow lifecycle management
"""
