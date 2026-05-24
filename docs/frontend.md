# 前端 (Frontend)

Next.js monorepo (Turborepo + pnpm workspace)，6 个子应用 + 4 个共享包。

## 结构

```
frontend/
├── apps/
│   ├── chat-ui/          # 用户聊天工作台 — 对话、文件上传、模型/Agent切换
│   ├── admin-console/    # 管理后台 — Dashboard、用户/权限/工具/运营分析
│   ├── agent-studio/     # Agent 工坊 — 创建/编辑/调试/市场
│   ├── model-center/     # 模型中心 — 注册/配额/路由/Fallback配置
│   ├── rag-studio/       # RAG 工坊 — 知识库管理、Chunk查看、召回调试
│   └── trace-console/    # Trace 控制台 — 会话列表/对话流/Span详情/回放
└── packages/
    ├── ui-components/    # 共享 UI 组件 (Card, Button, Spinner)
    ├── api-client/       # Axios 封装，JWT 自动附带
    ├── auth/             # 认证模块 (登录状态、角色判断)
    └── utils/            # 工具函数
```

## 子应用功能

### chat-ui — 用户聊天工作台
- 多轮对话 + SSE 流式输出
- Markdown / Code Highlight / Mermaid / LaTeX 渲染
- 文件上传（PDF/Excel/CSV/Word）并预览
- 模型切换 / Agent 切换
- Tool 执行状态展示 / RAG 引用展示
- 会话记忆展示

### admin-console — 管理后台
- Dashboard: Token 趋势图 / 成本趋势图 / 模型排行 / 错误率
- 用户管理：创建/导入/LDAP 同步
- 角色管理：RBAC 角色 + ABAC 规则
- Tool 管理：注册/测试台/调用日志
- 运营分析：使用分析/质量分析/成本分析/报表

### agent-studio — Agent 工坊
- Agent 创建/编辑：Prompt/Tool/Skill/Memory/模型绑定
- Prompt 编辑器：变量注入 + 版本管理
- Agent 调试：输入测试消息，查看完整 Trace
- Multi-Agent 调试：可视化 Coordinator 执行流程
- Agent Marketplace：浏览/搜索/安装/评价

### trace-console — Trace 控制台
- 会话列表：多条件筛选（用户/Agent/模型/时间/异常）
- 对话流展示：分层展示
- Span 详情面板：Intent/Skill/RAG/Tool/LLM 展开
- Prompt 查看 / Tool 链路 / RAG 链路
- Token 成本分析 / Replay 回放
