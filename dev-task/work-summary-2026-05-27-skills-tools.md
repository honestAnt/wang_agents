# 工作总结 — Skills 和 Tools 管理功能完善

**日期**: 2026-05-27

## 背景

skills 和 tools 管理页面前端使用 mock 数据，后端缺少多个关键 API 端点，前后端未打通。

## 完成的工作

### 后端增强

**Skill Service (skill-service)**:
- SkillRepository 新增 `findByTenantIdAndName` 和多字段模糊搜索方法
- SkillService 新增方法: `matchByIntent`（意图匹配）、`execute`（技能执行）、`listCategories`（分类列表）、`createFull`（完整创建）、`updateFull`（完整更新）、`install`（安装计数+1）
- SkillController 新增端点:
  - `GET /api/skills/match?intent=&tenantId=` — Python runtime 意图匹配
  - `GET /api/skills/categories?tenantId=` — 获取所有分类
  - `POST /api/skills/{id}/install` — 安装技能
  - `POST /api/skills/{id}/execute` — 执行技能
  - 改造 POST/PUT 使用 `@RequestBody` 支持完整 JSON 字段
- 合并到 agent-application 单体应用（ComponentScan 自动发现）

**Tool Service (tool-service)**:
- ToolRepository 新增: `findByTenantIdAndName`, `findByTenantIdAndStatus`
- ToolService 新增方法: `update`（完整更新）、`resolveByName`（按名称查找）、`execute`（工具测试执行）、`getStats`（统计数据）
- ToolController 新增端点:
  - `PUT /api/tools/{id}` — 更新工具
  - `GET /api/tools/resolve?name=&tenantId=` — Python runtime/SDK 解析工具
  - `POST /api/tools/execute` — 执行工具测试
  - `GET /api/tools/stats?tenantId=` — 统计数据

**Gateway (gateway-service)**:
- 新增 `/api/skills/**` → skill-service 路由

### 前端增强

**Skills 页面 (agent-studio)**:
- 替换 mock 数据为真实 API 调用 (`apiClient` → `/api/skills`)
- 完整 CRUD: 创建 Skill Modal (支持 name/displayName/description/category/promptTemplate/schema/icon)
- 编辑 Skill Modal (含 status 修改)
- 删除 Skill (Popconfirm + API)
- 发布 Skill (POST /publish)
- 安装 Skill (POST /install, 下载数+1)
- Skill 详情 Drawer
- 动态分类筛选 (从 API 获取分类列表)

**Tools 页面 (admin-console)**:
- 替换 mock 数据为真实 API 调用 (`apiClient` → `/api/tools`)
- 完整 CRUD: 注册 Tool Modal (支持 name/type/endpoint/method/schema/timeout/retry/status)
- 编辑 Tool Modal (所有字段可更新)
- 删除 Tool (Popconfirm + API)
- 工具测试功能 (Test Modal + JSON 参数输入 + 执行结果显示)
- 统计卡片从 API 获取真实数据
- Tool 详情 Drawer

**导航菜单**:
- Agent Studio: 新增侧边栏导航 (Agents/Marketplace/Skills/Prompts/Multi-Agent)
- Admin Console: 新增侧边栏导航 (Dashboard/Users/Models/Tools/Analytics)
- 使用 server component layout + client component 分离模式，保留 metadata 导出

## 遇到的问题和解决方案

1. **layout.tsx `"use client"` 与 metadata 冲突**: Next.js server component 中 metadata 导出在 client component 中不可用。解决方案: 将 layout 拆分为 server component (metadata + providers) + client component (导航 + 路由)。

2. **单体应用架构理解**: 原以为各服务独立运行在不同端口，实际已合并到 `agent-application` (9090 端口) 通过 `@ComponentScan("com.enterpriseai")` 自动发现所有服务模块。

3. **JPA 方法名过长**: 模糊搜索方法名 `findByTenantIdAndNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase` 过长但符合 Spring Data JPA 规范。

## 技术要点

- 前端 API 调用统一使用 `@enterprise-ai/api-client` 的 `apiClient`（自动附带 JWT token + tenant_id）
- 后端所有端点使用 `@PreAuthorize` 进行角色控制
- 技能/工具的 execute 端点为同步占位实现，后续可接入 AgentScope Runtime 做真实执行

## 编译和测试结果

- Java 后端编译: 通过
- Java 单元测试 (skill-service + tool-service): 全部通过
- TypeScript 类型检查: 新增文件无错误（2 个 pre-existing 错误在 rag-studio/trace-console）
