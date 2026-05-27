# LlamaIndex + Qdrant 企业知识库引擎

Date: 2026-05-27 | Status: approved

## 概述

在 agent-python 中构建完整的 LlamaIndex + Qdrant 知识库引擎，覆盖文档摄取、索引管理、本地向量检索全流程。索引管理在 Python 侧独立维护，Java rag-service 作为可选的下游同步消费方。

## 架构

```
KBManager (统一门面)
├── IngestionPipeline (文档摄取)
│   ├── Loaders (PDF/Word/MD/HTML/Text + S3/MinIO)
│   ├── Chunkers (FixedSize/Semantic/Markdown)
│   ├── EmbeddingClient (复用)
│   └── Qdrant VectorStore (写入)
├── IndexStore (索引 CRUD)
│   └── Qdrant VectorStore (管理)
└── Retriever (检索)
    ├── Qdrant VectorStore (读取)
    ├── Reranker (复用)
    └── PermissionAwareRAG (复用)
```

### 数据流

```
摄取: S3/MinIO/Local → Loader → Chunker → EmbeddingClient → Qdrant
检索: Query → EmbeddingClient → Qdrant ANN → Reranker → Permission Filter → Results
```

## 新增/改造文件

### 新增 — `app/rag/ingestion/__init__.py`
模块标记文件。

### 新增 — `app/rag/ingestion/loaders.py`
文档加载器，支持 6 种格式 + S3/MinIO：

| Loader | 格式 | 实现 |
|--------|------|------|
| PDFLoader | .pdf | PyPDF2 提取文本 |
| DocxLoader | .docx | python-docx 提取段落 |
| MarkdownLoader | .md | 内置解析，保留 frontmatter |
| HTMLLoader | .html | BeautifulSoup 提取文本 |
| TextLoader | .txt/.csv/.json | 内置解析 |
| S3Loader | 任意格式 | boto3/minio 拉取文件后委托对应 Loader |

所有 Loader 返回统一的 `Document` 数据结构：`{content, metadata{source, page, format, tenant_id, kb_id}}`

### 新增 — `app/rag/ingestion/chunkers.py`
分块策略：

| Chunker | 策略 | 参数 |
|---------|------|------|
| FixedSizeChunker | 固定 token 数 + overlap 滑动窗口 | chunk_size=500, overlap=50 |
| SemanticChunker | 按句子边界分割，相邻句子相似度低于阈值时分块 | threshold=0.6 |
| MarkdownChunker | 按 ## 标题层级分块，保留父标题作为上下文 | include_headers=True |

统一返回 `list[Document]`，继承原 metadata 并添加 `chunk_index`。

### 新增 — `app/rag/ingestion/pipeline.py`
`IngestionPipeline` 编排完整摄取流程：

```
async def run(source, kb_id, tenant_id, chunker_type, embedding_model) → IngestResult
```

- 接收 S3 URI / 本地路径 / file bytes
- 自动选择 Loader（按扩展名或 MIME type）
- 执行 load → chunk → embed → index
- 返回 `{document_count, chunk_count, vector_count, errors}`

### 新增 — `app/rag/index_store.py`
Qdrant 索引 CRUD，管理 tenant 级别的 collection 隔离：

- `create(kb_id, tenant_id)` → `QdrantClient.create_collection("{tenant_id}_{kb_id}")`
- `delete(kb_id, tenant_id)` → `QdrantClient.delete_collection(...)`
- `add_points(collection, points)` → `QdrantClient.upsert(...)`
- `delete_points(collection, doc_ids)` → `QdrantClient.delete(...)` by filter
- `stats(collection)` → `QdrantClient.count(...)` + collection info

### 新增 — `app/rag/kb_manager.py`
知识库统一门面，对外暴露：

| 方法 | 说明 |
|------|------|
| `create_kb(kb_id, tenant_id, config)` | 创建知识库（初始化 collection + 元数据） |
| `delete_kb(kb_id, tenant_id)` | 删除知识库及其所有向量 |
| `ingest(kb_id, source)` | 摄取文档到知识库，委托 IngestionPipeline |
| `search(kb_id, query, top_k, filters)` | 检索，委托 Retriever |
| `get_stats(kb_id)` | 统计信息 |

### 改造 — `app/rag/retriever.py`
从纯 HTTP 代理改为本地 Qdrant 优先 + Java 降级：

1. 用 `EmbeddingClient` 将 query 转为向量
2. 调 `QdrantClient.search()` 做 ANN 检索（带 tenant/kb_id filter）
3. 调 `Reranker.rerank()` 重排
4. 调 `PermissionAwareRAG` 注入权限过滤
5. Qdrant 不可用时自动降级为 HTTP 调 Java rag-service

返回格式保持兼容：`list[dict]`，包含 `{content, score, metadata, chunk_id}`。

## 依赖

新增 Python 依赖（添加到 requirements.txt）：
- `PyPDF2>=3.0` — PDF 解析
- `python-docx>=1.1` — Word 解析
- `beautifulsoup4>=4.12` — HTML 解析
- `minio>=7.2` — S3/MinIO 客户端
- `llama-index-core>=0.10` — LlamaIndex 核心（已有）
- `llama-index-vector-stores-qdrant` — LlamaIndex Qdrant 集成（已有）

## 错误处理

- 所有组件在外部服务不可用时降级而非崩溃
- Loader 不支持的文件格式返回明确错误信息
- Chunker 空文档返回空列表
- Qdrant 不可用时 Retriever 自动切换 Java 回退
- IngestionPipeline 部分文档失败不影响其他文档

## 测试策略

- **loaders.py**: 每种 loader 测试正确解析样本文件
- **chunkers.py**: 测试分块大小/overlap/边界处理
- **pipeline.py**: 端到端摄取测试（内存 Qdrant）
- **index_store.py**: CRUD 操作测试（mock Qdrant）
- **kb_manager.py**: 门面集成测试
- **retriever.py**: 本地检索 + 降级测试
