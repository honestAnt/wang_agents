# Work Summary — 2026-05-27: LlamaIndex + Qdrant 企业知识库引擎

## 背景
架构设计文档明确要求 "RAG Engine (LlamaIndex + Qdrant)"，但 agent-python 代码中缺少完整的 LlamaIndex 知识库管道——只有基础的 retriever（HTTP 代理到 Java）、embedding、reranker。

## 新增模块（7 个文件）

```
agent-python/app/rag/
├── ingestion/
│   ├── __init__.py          # 包标记
│   ├── types.py             # Document, ChunkConfig, IngestResult 数据类
│   ├── loaders.py           # 6 种加载器: Text/PDF/Docx/MD/HTML/S3
│   ├── chunkers.py          # 3 种分块: FixedSize/Semantic/Markdown
│   └── pipeline.py          # 摄取编排: load→chunk→embed→index
├── index_store.py           # Qdrant 集合 CRUD（租户隔离）
├── kb_manager.py            # 统一门面：create/delete/ingest/search/stats
└── retriever.py             # [改造] 本地 Qdrant 优先 + Java 降级
```

## 能力矩阵

| 能力 | 实现 |
|------|------|
| 文档加载 | PDF (PyPDF2), Word (python-docx), Markdown (frontmatter), HTML (BeautifulSoup), Text/CSV, S3/MinIO |
| 分块策略 | FixedSize (token + overlap), Semantic (句子边界), Markdown (标题层级 + 父标题保持) |
| 嵌入 | 复用 EmbeddingClient → text-embedding-3-small (1536d) |
| 索引 | Qdrant 集合 CRUD, 租户级隔离: `{tenant_id}_{kb_id}` |
| 检索 | 本地 Qdrant ANN → Reranker 重排 → 权限过滤 → Java rag-service 降级 |
| 统一入口 | KBManager facade: create_kb / delete_kb / ingest / search / get_stats |

## 测试

- 新增 6 个测试文件，64 个测试用例
- 全量回归：**202 passed, 0 failed**
- 覆盖：types, loaders, chunkers, pipeline, index_store, kb_manager, retriever

## 关键设计决策
1. **本地优先 + Java 降级**：Retriever 先查本地 Qdrant，不可用时回退 HTTP 到 Java rag-service
2. **租户隔离**：Qdrant collection 命名 `{tenant_id}_{kb_id}`，所有搜索自动注入 tenant filter
3. **渐进式依赖**：Loader/Chunker 中的外部库（PyPDF2/docx/bs4/minio）使用 lazy import
4. **容错设计**：Qdrant 不可用时所有操作返回安全默认值，不抛异常
