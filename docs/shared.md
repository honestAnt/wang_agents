# 共享协议 (Shared Protocols)

微服务间通信契约定义。

## 结构

```
shared/
├── openapi/         # REST API 契约 (chat / agent / tool / model / rag / skill / trace)
├── proto/           # gRPC 接口定义 (Java ↔ Python)
├── event-schema/    # Kafka 事件结构 (agent.step / tool.call / rag.retrieval / llm.call)
└── trace-model/     # 统一 Trace Span 模型
```

## Trace Span 标准

所有服务必须输出的 Span 字段：

```json
{
  "trace_id": "uuid",
  "span_id": "uuid",
  "parent_id": "uuid|null",
  "type": "llm|tool|rag|memory|workflow",
  "latency": 123.4,
  "cost": 0.003,
  "tenant_id": "t-xxx",
  "attributes": {}
}
```

## Kafka 事件

| 事件 | 说明 |
|------|------|
| `agent.step` | Agent 执行步骤 (reason/act/observe) |
| `tool.call` | 工具调用记录 (入参/出参/latency/error) |
| `rag.retrieval` | RAG 检索记录 (query/results/rerank) |
| `llm.call` | LLM 调用记录 (model/tokens/cost/latency) |
