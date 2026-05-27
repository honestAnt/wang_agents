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

## 开发规范

### API 层规范

**路由定义**
- 每个 API 模块创建 `router = APIRouter()` 实例，在 `main.py` 中通过 `app.include_router()` 注册。
- 带独立前缀的路由在模块内声明：`APIRouter(prefix="/api/workflows", tags=["workflows"])`。
- `main.py` 中为 chat 路由统一加 `/api/v1` 前缀，workflow 路由自带前缀。
- 路由函数为模块级 `async def`，不放在类中。

**请求/响应模型**
- API 请求体使用 `pydantic.BaseModel`（FastAPI 自动校验），定义在对应 API 模块顶部。
- 内部数据传输（Activity Input、路由决策、配置等）使用 `@dataclass`，不用 Pydantic。
- 响应体直接返回 `dict`，不做 Pydantic 序列化。
- 模型命名：PascalCase，常用后缀 `Input` / `Result` / `Config` / `Info` / `Decision`。禁止 `*Schema` / `*DTO` 命名。

```python
# ✅ 请求模型 — pydantic.BaseModel
class ChatRequest(BaseModel):
    session_id: str | None = None
    agent_id: str | None = None
    message: str
    temperature: float = 0.7
    max_tokens: int = 4096

# ✅ 内部数据 — @dataclass
@dataclass
class WorkflowStartRequest:
    workflow_type: str
    tenant_id: str
    session_id: str
    user_message: str
    agent_id: str | None = None
```

**请求头/查询参数**
- 租户 ID 和 Trace ID 通过 Header 注入：`x_tenant_id: str = Header("", alias="X-Tenant-Id")`。
- 可选过滤/分页参数使用 `Query(default=..., description=..., ge=..., le=...)`。

**错误处理**
- 每个路由函数体必须包裹 `try/except`。
- `ValueError` → `HTTPException(status_code=400)`。
- 通用 `Exception` → `HTTPException(status_code=500)`。
- 必须使用 `raise ... from e` 保留异常链。

```python
@router.post("")
async def start_workflow(request: WorkflowStartRequest):
    try:
        client = await get_workflow_client()
        info = await client.start_workflow(request)
        return {"workflow_id": info.workflow_id, "status": info.status}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to start workflow: {e}") from e
```

### 服务层规范

**类结构**
- 服务为普通类，不继承抽象基类或 Protocol。
- 构造函数接收可选配置参数，默认值从环境变量读取。
- 外部依赖（Redis、Qdrant、Temporal Client）延迟初始化，在首次使用时通过 `_get_client()` / `_init_*()` 私有方法创建。
- 服务不可用时降级为内存实现或空结果，不抛异常。

```python
class WorkflowClient:
    def __init__(self, host: str | None = None):
        self._host = host or os.getenv("TEMPORAL_HOST", "localhost:7233")
        self._client: TemporalClient | None = None

    async def _get_client(self) -> TemporalClient:
        if self._client is None:
            self._client = await TemporalClient.connect(self._host)
        return self._client
```

**依赖注入**
- 不使用 DI 框架。服务在构造函数中直接实例化所需依赖。
- `AgentEngine` 在 `__init__` 中创建 `ContextBuilder`、`IntentRouter`、`ModelRouter`、`ModelWrapper`、`ShortTermMemory`。
- 跨请求复用的组件（如 WorkflowClient）使用模块级单例 + 异步工厂函数。

```python
# 模块级单例
_workflow_client: WorkflowClient | None = None

async def get_workflow_client() -> WorkflowClient:
    global _workflow_client
    if _workflow_client is None:
        _workflow_client = WorkflowClient()
    return _workflow_client
```

**错误处理**
- 服务层方法捕获异常后 `logger.warning(...)` 记录日志，返回合理默认值（空列表、空字符串、原始输入），不向上抛异常。
- 这是"弹性降级"模式：外部服务故障时系统仍可运行。

**异步规范**
- 所有操作为 `async def`。
- HTTP 调用使用 `httpx.AsyncClient`，每次调用创建短生命周期 client（`async with` 块内）。
- Redis 使用 `redis.asyncio`。

### 日志规范

- 每个模块顶部定义 `logger = logging.getLogger(__name__)`。
- 使用 `%s` 占位符而非 f-string，以支持惰性求值。

```python
import logging
logger = logging.getLogger(__name__)

logger.warning("Memory injection failed: %s", e)
```

### 配置规范

- 所有环境变量集中在 `app/config.py` 读取，提供默认值。
- 外部服务 URL 提供统一入口（如 `BACKEND_SERVICE_URL`），同时保留独立变量以备将来拆分。

### Trace 规范

- 所有 AI 调用（LLM、Tool、RAG、Memory）必须包裹在 `tracer.start_span()` 上下文中。
- Span 必须设置属性：`tenant_id`、`agent_id`、`model`、`intent`。
- Trace ID 从请求头 `X-Trace-Id` 透传，无则自动生成。

### 模块文档规范

- 每个 `.py` 文件顶部写一行模块级 docstring，说明模块用途。
- 每个公开类和方法写 Google 风格 docstring，描述用途、参数和返回值。
- 不需要长段落或参数类型声明（类型注解已覆盖）。

```python
"""Context builder — assembles system prompt with Memory, RAG, Skills, and Tools."""

class ContextBuilder:
    """Assembles the full context for an LLM call from all available sources.

    Injects:
      - Memory: short-term conversation + long-term preferences + vector similarity
      - RAG: relevant knowledge base chunks via KBManager
      - Skills: matched skills from skill-service
    """

    async def build(self, tenant_id: str, user_message: str, session_id: str, agent_id: str | None = None) -> dict:
        """Build the complete prompt context.

        Returns a dict with:
          - system_prompt: final augmented system prompt
          - messages: conversation history
          - rag_context: retrieved knowledge base chunks
          - available_skills: matched skills
        """
```

### 代码风格

- **格式化**: Ruff，行长 120，Python 3.12 目标。
- **Python 版本**: 开发 >= 3.11，Docker 镜像 3.12-slim。
- **类型注解**: 所有函数参数和返回值必须标注类型。使用 `str | None` 而非 `Optional[str]`。
- **mutable 默认值**: `@dataclass` 中列表/字典默认值使用 `field(default_factory=list)`。
- **导入风格**: 标准库 → 第三方库 → 内部模块，每组之间空一行。
- **禁止通配符导入** (`from module import *`)。

### 测试规范

**文件组织**
- 测试文件放在 `tests/` 目录，命名 `test_<module_name>.py`。
- 测试类命名 `Test<ComponentName>`，测试方法命名 `test_<scenario>`。
- 按功能分组到不同测试类（如 `TestRoute`、`TestCostEstimation`、`TestFallback`）。

```python
class TestCostEstimation:
    def test_estimates_token_count(self):
        ...

    def test_returns_zero_for_empty_input(self):
        ...
```

**Fixture 和 Mock**
- 使用 `@pytest.fixture` 创建测试依赖，作用域默认 function。
- 优先使用真实对象 + 降级行为测试，而非 mock。服务在 Redis/Qdrant 不可用时自动降级到内存实现，测试利用这一点避免 mock。
- 需要清理的 fixture 使用 `yield` 模式。

**异步测试**
- `pytest-asyncio` 配置 `asyncio_mode = "auto"`，无需手动 `@pytest.mark.asyncio`。
- FastAPI 路由测试使用 `from fastapi.testclient import TestClient`。

**断言**
- 统一使用原生 `assert` 语句，不使用 `self.assertEqual()` 等 unittest 风格。
- 浮点数比较使用 `pytest.approx()`。

**运行命令**
```bash
.venv/bin/python -m pytest tests/ -v
```
