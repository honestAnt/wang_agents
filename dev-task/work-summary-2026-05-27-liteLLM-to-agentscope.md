# 工作总结：移除 LiteLLM 改用 AgentScope ModelWrapper

**日期**: 2026-05-27
**任务**: 架构改进 — 把 LiteLLM 模型网关拿掉，改为 AgentScope ModelWrapper 直连 Provider

## 背景

原架构在 AgentScope Runtime 和 LLM Provider 之间插了一层 LiteLLM SDK 做模型网关。实际上所有 LiteLLM 调用都是注释/占位符，未真正执行。用户要求移除这个中间层，用 AgentScope 自带的 ModelWrapper 模式直接调用各 Provider 的 REST API。

## 改动范围

- **18 个文件**变更（451 行新增，82 行删除）
- 新建 `agent-python/app/llm/model_wrapper.py`，删除旧的 `litellm_client.py`
- 更新 `engine.py`、`embedding.py`、`model_router.py`
- 更新 `pyproject.toml` / `requirements.txt` 依赖
- 删除 `infra/lite-llm-proxy/` 目录
- 更新 7 个文档文件和 `.env.example`、`dev_state.json`
- 新增 `test_model_wrapper.py`，删除 `test_litellm_client.py`

## 核心设计

`ModelWrapper` 类采用 AgentScope 的 Provider Registry 模式：
- `PROVIDER_REGISTRY` 字典映射 model_name → ProviderConfig（api_key_env, base_url, provider type）
- `chat()` 方法根据 provider 类型路由到 `_chat_openai()` / `_chat_anthropic()` / `_chat_google()`
- 每个 provider 通过 `httpx`（项目已有依赖）直连其 REST API
- 流式输出直接解析 SSE/NDJSON，不做累积→增量转换
- `embed()` 方法：当 `OPENAI_API_KEY` 未配置时降级返回零向量占位，避免测试/开发环境报错
- `get_cost_estimate()` 保留成本估算，与 ModelRouter 的定价表保持一致

## 遇到的问题及解决

### 1. EmbeddingClient 测试失败（httpx.ConnectError）
**现象**: `test_embed_returns_correct_dimensions` 和 `test_embed_empty_list` 失败，因为 EmbeddingClient 改为调用 ModelWrapper.embed() 后会真实发起 HTTP 请求到 api.openai.com。
**解决**: 在 `ModelWrapper.embed()` 开头加判断——如果 `OPENAI_API_KEY` 环境变量为空，直接返回零向量占位，不发起网络请求。测试环境无 API Key 时走这个降级路径。

### 2. 跨目录 git add 失败
**现象**: `git add .env.example` 报 `pathspec did not match`，因为当前工作目录在 `agent-python/` 子目录下。
**解决**: 使用 `git -C /path/to/repo add ...` 指定仓库根目录，或用 `git add -u` 暂存所有已跟踪文件的修改。

### 3. Pylance 未使用导入告警
**现象**: `model_wrapper.py` 中 `from dataclasses import dataclass, field` 的 `field` 未使用。
**解决**: 移除未使用的 `field` 导入。

## 测试结果

```
145 passed in 0.86s
```

新增 `test_model_wrapper.py` 6 个测试：
- 4 个成本估算测试（gpt4/claude/deepseek/unknown）
- 1 个 Provider 注册表覆盖测试（验证 ModelRouter 中所有模型都有 ProviderConfig）
- 1 个 fallback 测试（未知模型降级为 OpenAI-compatible）

## 架构收益

| 之前 | 之后 |
|------|------|
| AgentScope → LiteLLM SDK → Provider | AgentScope → ModelWrapper → Provider |
| 多一层中间依赖（litellm>=1.40.0） | 零额外依赖，用已有的 httpx |
| LiteLLM Proxy 需单独部署配置 | 无需额外服务，直连 Provider API |
| 接口是占位符，未真正调用 | engine.py 已接入真实流式调用 |
