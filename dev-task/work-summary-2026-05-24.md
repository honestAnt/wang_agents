# 工作总结 — 2026-05-24

## 完成内容

Phase 3 (企业级) 8 个任务全部完成，至此 39/39 任务全部完成。

1. **task-3.1** Temporal 工作流引擎 — Activities/Workflows/Saga/Worker/Client/API
2. **task-3.2** Agent 市场 — 发布/发现/安装/评价 + 前端页面
3. **task-3.3** 智能路由系统 — TaskClassifier + ModelRouter (cost/fallback/stats)
4. **task-3.4** AI 运营分析中心 — admin-service 模块 + 分析 API + Dashboard 前端
5. **task-3.5** AI 安全治理系统 — PromptGuard/DataMasker + audit-service
6. **task-3.6** 平台运营功能 — 全局限流/Provider 熔断/公告
7. **task-3.7** Java/Python SDK — Chat/Tool/Agent/Trace 四类 Client
8. **task-3.8** K8s + CI/CD — Helm Chart + GitHub Actions Pipeline

另外：新增 `docs/` 目录，按目录结构拆分 10 个文档；优化 CLAUDE.md。

## 遇到的问题及解决

1. **temporalio activity.heartbeat() 测试失败** — Activity 函数内 heartbeat() 需要 Temporal Runtime Context，单元测试中不存在。
   - 解决：将 Activity 拆分为纯业务函数 (`_do_*`) + Temporal 装饰器包装，单元测试直接测纯函数。

2. **Java Map.of() 不支持 null 值** — `Map.of("lastFailure", null)` 在 Java 21 中抛出 NPE。
   - 解决：改用 `LinkedHashMap` 并 `put` 逐个添加，或把 null 替换为 "N/A"。

3. **Mockito 严格模式 UnnecessaryStubbing** — `when().thenReturn()` 被 stub 但代码路径未走到导致异常。
   - 解决：确保 mock 数据让代码覆盖到被 stub 的调用路径（如 reviews 列表不为空才能触发 save）。

4. **Java JDK 版本不匹配** — 系统默认 Java 8，但项目需 Java 21。
   - 解决：通过 `JAVA_HOME` 环境变量指定 `jdk-21.0.2.jdk/Contents/Home`。

5. **任务分类器关键词歧义** — "find documents about machine learning" 中 "machine learning" 命中 data_science 而非 search。
   - 解决：改进 search 正则模式支持更多变体（复数、政策、报告等关键词）。

6. **XML POM 解析错误** — em dash 字符 (—) 在 XML 注释中触发解析错误。
   - 解决：替换为普通 ASCII 连字符。

7. **Prompt Leak 检测漏报** — "show me your system prompt" 中 "show me" 与 "your" 之间有词导致模式不匹配。
   - 解决：在 show/your 之间加入 `.{0,5}` 允许少量中间词。

8. **DataMasker 身份证被手机号模式误匹配** — "110101199001011234" 中部分子串被手机号正则先捕获。
   - 解决：将更长的模式（身份证、信用卡）放在列表前面优先匹配。
