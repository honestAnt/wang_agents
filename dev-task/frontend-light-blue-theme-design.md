# 前端淡蓝色主题改造 — 设计文档

**日期**: 2026-05-27  
**状态**: 设计中

## 目标

将全部 6 个前端子应用改造为统一淡蓝色主题，使用 antd 原生组件替代 `@enterprise-ai/ui` 自定义组件，样式用 Tailwind CSS 管理。

## 技术方案

- **组件库**: antd 5.x，通过 `ConfigProvider` 全局注入主题 token
- **样式框架**: Tailwind CSS，antd 样式由 antd 自身管理，Tailwind 负责布局/间距/自定义样式
- **主题色**: `#4096FF`（柔和蓝）
- **包变更**: 废弃 `@enterprise-ai/ui` 中的 Button/Card/Spinner（antd 都有对应组件）

## Ant Design ConfigProvider 主题配置

```ts
// colorPrimary: #4096FF
// borderRadius: 6
// colorBgLayout: #F0F5FF
```

## Tailwind 扩展色板

| Token | 色值 | 用途 |
|-------|------|------|
| primary-50 | #EFF6FF | 页面底色 |
| primary-100 | #DBEAFE | hover 浅底 |
| primary-500 | #4096FF | 主色 |
| primary-600 | #1677FF | hover/active |

## 共享根布局

每个 app 的 layout.tsx 统一包裹 `AntdRegistry` + `ConfigProvider`，需要导航的页面使用 antd `Layout`（Sider + Header + Content）。

## 各应用页面改造清单

### 1. 登录页（admin-console + chat-ui）
- antd: Card, Input, Input.Password, Button, Form, message
- 居中卡片式，背景 primary-50

### 2. Chat UI（chat-ui /）
- antd: Layout.Sider, Menu, Select, Input.TextArea, Button, Upload, Spin, Collapse
- 侧边栏会话列表 + 聊天消息区 + 底部输入区

### 3. Dashboard（admin-console /）
- antd: Card, Statistic, Table, Select, Spin
- 指标卡片 + 趋势图 + 最近会话表

### 4. Admin Console 管理页（/users, /models, /tools, /analytics）
- antd: Table, Input.Search, Select, Button, Modal, Form, Tag, Popconfirm, Pagination
- 标准 CRUD：搜索栏 + 表格 + 新建/编辑弹窗

### 5. Agent Studio（/agents, /marketplace, /skills, /prompts, /multi-agent）
- antd: Card, Card.Meta, Tag, Segmented, Empty
- 卡片列表 + 视图切换

### 6. Trace Console + RAG Studio
- antd: Table, Collapse, Statistic, Input.Search, List
- 数据表格 + 链路详情 + 召回结果
