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
    ├── ui-components/    # 共享 UI 组件 + antd 主题配置 (ThemeProvider)
    ├── api-client/       # Axios 封装，JWT 自动附带
    ├── auth/             # 认证模块 (登录状态、角色判断)
    └── utils/            # 工具函数
```

## 组件库 & 样式体系

### 组件库：Ant Design 6.x

全部 6 个子应用统一使用 **antd 6.x** 作为 UI 组件库，禁止使用原生 HTML 表单元素或旧的 `@enterprise-ai/ui` 自定义组件。

**核心原则：**
- 所有表单控件用 antd：`Input` / `Input.TextArea` / `Input.Password` / `Select` / `DatePicker` 等，禁止原生 `<input>` / `<textarea>` / `<select>`
- 所有按钮用 antd `Button`，禁止原生 `<button>` 或旧 `@enterprise-ai/ui` Button
- 容器/卡片用 antd `Card`，禁止旧 `@enterprise-ai/ui` Card
- 消息提示用 `App.useApp().message`（静态方法在 antd 6 已废弃）
- 图标统一用 `@ant-design/icons`

### 样式工具：Tailwind CSS 4

**Tailwind 4** 通过 `@tailwindcss/postcss` PostCSS 插件管理布局和间距，与 antd 分工如下：

| 关注点 | 工具 |
|--------|------|
| 组件交互（表单/表格/弹窗/下拉） | antd |
| 颜色/圆角/字体/组件级 token | antd ConfigProvider theme |
| 布局/间距/网格/弹性布局 | Tailwind |
| 响应式断点 | Tailwind (`xs:` / `sm:` / `lg:` 等) |
| 背景色（页面级） | Tailwind `bg-primary-50` |
| Hover/过渡/阴影 | Tailwind |

**Tailwind 4 配置：** 在每个 app 的 `globals.css` 中通过 `@theme inline` 扩展 primary 色板，与 antd `colorPrimary: "#4096FF"` 保持一致：

```css
@import "tailwindcss";

@theme inline {
  --color-primary-50: #EFF6FF;
  --color-primary-100: #DBEAFE;
  --color-primary-200: #BFDBFE;
  --color-primary-300: #93C5FD;
  --color-primary-400: #60A5FA;
  --color-primary-500: #4096FF;
  --color-primary-600: #1677FF;
  --color-primary-700: #1D4ED8;
  --color-primary-800: #1E40AF;
  --color-primary-900: #1E3A8A;
}

body {
  background: #F0F5FF;
}
```

### 统一主题配置

主题在 `packages/ui-components/src/theme.tsx` 中定义，所有 app 的 `layout.tsx` 通过 `ThemeProvider` 引入：

```tsx
// packages/ui-components/src/theme.tsx
const themeConfig = {
  token: {
    colorPrimary: "#4096FF",       // 主色 — 淡蓝
    borderRadius: 6,
    colorBgLayout: "#F0F5FF",     // 布局背景
    colorBgContainer: "#FFFFFF",  // 容器背景
  },
  components: {
    Layout: {
      siderBg: "#FFFFFF",
      headerBg: "#FFFFFF",
      bodyBg: "#F0F5FF",
    },
    Menu: {
      itemSelectedBg: "#EFF6FF",
      itemSelectedColor: "#4096FF",
      itemActiveBg: "#DBEAFE",
    },
    Card: { borderRadiusLG: 8 },
    Button: { borderRadius: 6 },
    Table: { headerBg: "#FAFAFA" },
  },
};
```

### 每个 app 的 layout.tsx 标准结构

```tsx
import { ThemeProvider } from "@enterprise-ai/ui";
import { AntdRegistry } from "@ant-design/nextjs-registry";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen">
        <AntdRegistry>
          <ThemeProvider>
            {children}
          </ThemeProvider>
        </AntdRegistry>
      </body>
    </html>
  );
}
```

`AntdRegistry` 解决 antd 6 在 Next.js SSR 中的样式闪烁问题。`ThemeProvider` 内部包含 `ConfigProvider` + `AntApp`（提供 `App.useApp()` 消息/通知/模态框）。

### 颜色体系

| Token | 色值 | 用途 |
|-------|------|------|
| `colorPrimary` | `#4096FF` | 主操作按钮、选中态、链接 |
| `colorBgLayout` | `#F0F5FF` | 页面底色 |
| `primary-50` | `#EFF6FF` | Tailwind 最浅蓝背景 |
| `primary-100` | `#DBEAFE` | 悬浮态背景 |
| `primary-500` | `#4096FF` | 图表/图标强调色 |
| `primary-600` | `#1677FF` | 渐变终点、深色强调 |

### 迁移检查清单

改造/新增页面时，确认以下项：

- [ ] 未引用 `@enterprise-ai/ui` 的 Button / Card / Spinner / notification
- [ ] 无原生 `<input>` / `<textarea>` / `<select>` / `<button>` 元素
- [ ] 表单使用 antd `Form` + `Form.Item` 管理校验
- [ ] 消息提示使用 `App.useApp().message`，非静态 `message.error()`
- [ ] 图标来自 `@ant-design/icons`，非 emoji 或裸文字
- [ ] 页面底色使用 `#F0F5FF` 或 Tailwind `bg-primary-50`
- [ ] TypeScript 编译零错误

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
