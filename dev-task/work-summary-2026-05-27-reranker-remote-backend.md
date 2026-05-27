# Work Summary — Reranker Remote Backend + Caller Module

**Date**: 2026-05-27
**Task**: 将 Reranker 改为支持调用远程 reranker 服务，新增 caller.py 模块

## 做了什么

1. **Reranker 新增 `remote` 后端** — `_rerank_remote()` 方法通过 `POST /rerank` 调用远程服务
2. **自动检测** — 设置 `RERANKER_REMOTE_URL` 环境变量 或 传入 `remote_url` 参数即可自动切换到 remote 后端
3. **caller.py 模块** — `RerankerClient` 类（同步/异步）和 `recall()` 函数（带 LLM fallback）

## 使用方式

### 方式一：Reranker 集成（全流水线自动使用）
```python
# 设置环境变量，RAG 流水线自动使用远程 reranker
import os
os.environ["RERANKER_REMOTE_URL"] = "http://localhost:5000"

# Retriever / KBManager / ContextBuilder 无需改动
```

### 方式二：独立调用
```python
from app.rag.caller import recall, RerankerClient

client = RerankerClient('http://localhost:5000')

def my_llm_rerank(query, docs):
    ...

result = await recall('Python web framework', candidates, client, my_llm_rerank)
```

## 遇到什么问题

- 无明显问题，改动范围小且向后兼容

## 测试结果
- 10 个新测试全部通过（4 个 remote backend + 6 个 caller 模块）
- 全量 219 个测试通过，无回归
