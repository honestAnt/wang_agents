# Python Agent Runtime

基于 AgentScope 的 Agent 执行引擎，FastAPI + LlamaIndex。

## 结构

```
agent-python/
├── pyproject.toml
├── app/
│   ├── main.py                      # FastAPI 入口 + 健康检查
│   ├── api/
│   │   ├── chat.py                  # POST /chat (SSE 流式)
│   │   └── workflow_api.py          # Workflow CRUD API
│   ├── core/
│   │   ├── agent_engine/engine.py   # AgentScope 执行循环 (reason→act→observe)
│   │   ├── context/builder.py       # Context Builder (System Prompt + Memory + RAG)
│   │   ├── router/
│   │   │   ├── intent_router.py     # 意图识别
│   │   │   └── skill_router.py      # Skill 路由 + 链式调用
│   │   ├── executor/tool_executor.py # Tool 执行器
│   │   ├── workflow/                # Temporal 工作流引擎
│   │   │   ├── activities.py        # 8 个 Temporal Activity
│   │   │   ├── workflows.py         # 4 个 Workflow 定义
│   │   │   ├── saga.py              # Saga 补偿模式
│   │   │   ├── worker.py            # Temporal Worker
│   │   │   └── client.py            # WorkflowClient 生命周期管理
│   │   └── security/prompt_guard.py # Prompt Injection 检测 + 数据脱敏
│   ├── llm/
│   │   ├── model_wrapper.py         # AgentScope 模型包装器
│   │   ├── model_router.py          # 智能路由 (cost/fallback/stats)
│   │   └── task_classifier.py       # 任务分类器 (10 种类型)
│   ├── rag/
│   │   ├── retriever.py             # Hybrid Search 检索
│   │   ├── reranker.py              # BGE/CrossEncoder Rerank
│   │   ├── embedding.py             # Embedding 生成
│   │   └── permission_rag.py        # 权限感知 RAG 过滤
│   ├── memory/
│   │   ├── short_term.py            # Redis Session 短期记忆
│   │   ├── long_term.py             # PostgreSQL 长期记忆
│   │   ├── vector_memory.py         # 向量语义记忆
│   │   └── memory_injector.py       # Memory 注入 Prompt
│   ├── agents/multi_agent/
│   │   └── coordinator.py           # Multi-Agent Coordinator
│   ├── skills/skill_loader.py       # Skill 加载 + 热更新
│   └── trace/tracer.py              # 全链路 Trace 打点
└── tests/                           # 143 个测试
```

## 核心模块说明

### Agent Engine (`core/agent_engine/`)
AgentScope 执行循环：构建上下文 → 意图路由 → 选择模型 → 调用 LLM → 保存记忆。

### 智能路由 (`llm/`)
- **TaskClassifier**: 10 种任务类型识别 + 复杂度评估
- **ModelRouter**: 按 (task_type, complexity) 路由，成本优化，多级 Fallback，路由统计

### 工作流引擎 (`core/workflow/`)
基于 Temporal 的持久化工作流：
- 4 个 Workflow: DataCollection / AgentPipeline / Approval (人工审批) / Saga
- 8 个 Activity: RAG 检索 / LLM 调用 / Tool 执行 / 数据采集 / 分析 / 报告 / 审批 / 通知
- Saga 补偿：失败时反向执行补偿操作

### 安全 (`core/security/`)
- **PromptGuard**: 检测 Jailbreak / Prompt Leak / Code Injection / SQL Injection
- **DataMasker**: 脱敏 手机/邮箱/身份证/银行卡/IP/信用卡
