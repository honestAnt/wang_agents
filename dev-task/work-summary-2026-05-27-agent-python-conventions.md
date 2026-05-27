# Work Summary — 2026-05-27: agent-python 开发规范文档

## 任务
为 `docs/agent-python.md` 添加项目开发规范章节。

## 过程
1. 通过 Explore Agent 全面扫描 agent-python/ 代码库，提取现有模式：API 层、服务层、数据模型、配置、测试等。
2. 读取关键源文件确认具体实现细节（chat.py, workflow_api.py, engine.py, client.py, builder.py, main.py, pyproject.toml, config.py, test_main.py）。
3. 基于代码库实际模式编写规范，所有条目有代码示例支撑，不凭空编造规则。

## 新增内容（`docs/agent-python.md` 末尾）

### API 层规范
- 路由定义方式（APIRouter + include_router）
- 请求用 pydantic.BaseModel，内部用 @dataclass
- 响应直接返回 dict
- 命名规范：PascalCase + Input/Result/Config/Info 后缀，禁止 Schema/DTO
- 错误处理：ValueError→400, Exception→500, raise...from e 保留异常链

### 服务层规范
- 类结构：普通类 + 延迟初始化外部依赖
- 弹性降级：服务不可用时返回默认值而非抛异常
- 依赖注入：直接构造函数组合，跨请求组件用模块级单例
- 异步规范：全 async def，httpx.AsyncClient 短生命周期

### 日志规范
- logging.getLogger(__name__)，%s 占位符

### 配置规范
- env var 集中在 app/config.py

### Trace 规范
- 所有 AI 调用包裹 tracer.start_span()，必须设置核心属性

### 模块文档规范
- Google 风格 docstring，类型注解已覆盖无需重复声明参数类型

### 代码风格
- Ruff / 行长120 / py312 / 现代类型注解语法 / 导入分组

### 测试规范
- 文件/类/方法命名，fixture 模式，优先真实对象+降级测试而非 mock
- 原生 assert，pytest.approx() 浮点数
