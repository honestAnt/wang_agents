# Platform SDK

企业内部系统集成平台能力的 SDK。

## Python SDK

```
sdk/python-sdk/enterprise_ai/
├── __init__.py
├── chat.py      # ChatClient — 流式聊天 (SSE)
├── tool.py      # ToolClient — 工具执行
├── agent.py     # AgentClient — Agent 管理
└── trace.py     # TraceClient — 追踪查询
```

```python
from enterprise_ai import ChatClient, AgentClient

client = ChatClient(base_url="http://localhost:8080", api_key="...")
async for chunk in client.chat("tenant-1", "Hello"):
    print(chunk)

agent = AgentClient()
agents = await agent.list("tenant-1")
```

## Java SDK

```
sdk/java-sdk/src/main/java/com/enterpriseai/sdk/
├── ChatClient.java     # SSE 流式聊天
├── ToolClient.java     # 工具执行 (java.net.http)
├── AgentClient.java    # Agent CRUD
└── TraceClient.java    # Trace 会话查询 + 成本分析
```

```java
ChatClient client = new ChatClient("http://localhost:8080", "api-key");
client.chat("tenant-1", "Hello", null, null, null, chunk -> {
    System.out.println(chunk);
});
```
