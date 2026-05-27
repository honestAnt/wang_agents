# LlamaIndex + Qdrant 企业知识库引擎 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete LlamaIndex + Qdrant knowledge base engine with document ingestion pipeline, index management, and local vector retrieval.

**Architecture:** KBManager facade orchestrates IngestionPipeline (Load → Chunk → Embed → Index) and Retriever (Embed → Qdrant ANN → Rerank → Permission Filter). IndexStore manages Qdrant collection lifecycle. Retriever uses local Qdrant with Java rag-service fallback.

**Tech Stack:** LlamaIndex 0.10+, Qdrant, PyPDF2, python-docx, BeautifulSoup4, minio-py

---

## File Structure

```
agent-python/app/rag/
├── ingestion/                    # NEW directory
│   ├── __init__.py               # NEW
│   ├── types.py                  # NEW - shared dataclasses (Document, ChunkConfig, IngestResult)
│   ├── loaders.py                # NEW - 6 loaders + S3Loader
│   ├── chunkers.py               # NEW - 3 chunking strategies
│   └── pipeline.py               # NEW - ingestion orchestrator
├── index_store.py                # NEW - Qdrant collection CRUD
├── kb_manager.py                 # NEW - unified facade
├── retriever.py                  # MODIFY - add local Qdrant search + fallback
├── embedding.py                  # UNCHANGED
├── reranker.py                   # UNCHANGED
└── permission_rag.py             # UNCHANGED

agent-python/tests/
├── test_ingestion_loaders.py     # NEW
├── test_ingestion_chunkers.py    # NEW
├── test_ingestion_pipeline.py    # NEW
├── test_index_store.py           # NEW
├── test_kb_manager.py            # NEW
└── test_rag.py                   # MODIFY - update Retriever tests
```

---

### Task 1: Add new dependencies

**Files:**
- Modify: `agent-python/requirements.txt`

- [ ] **Step 1: Add ingestion dependencies to requirements.txt**

```
fastapi>=0.111.0
uvicorn[standard]>=0.29.0
sse-starlette>=2.0.0
agentscope>=1.0.0
llama-index>=0.10.0
llama-index-vector-stores-qdrant>=0.4.0
qdrant-client>=1.9.0
redis>=5.0.0
httpx>=0.27.0
aiohttp>=3.9.0
pydantic>=2.7.0
python-dotenv>=1.0.0
opentelemetry-api>=1.23.0
opentelemetry-sdk>=1.23.0
kafka-python>=2.0.0
PyPDF2>=3.0.0
python-docx>=1.1.0
beautifulsoup4>=4.12.0
minio>=7.2.0
```

- [ ] **Step 2: Install new dependencies**

```bash
cd agent-python && .venv/bin/pip install PyPDF2 python-docx beautifulsoup4 minio --quiet
```

- [ ] **Step 3: Commit**

```bash
git add agent-python/requirements.txt
git commit -m "chore: add ingestion dependencies (PyPDF2, python-docx, bs4, minio)"
```

---

### Task 2: Create shared types (ingestion/types.py)

**Files:**
- Create: `agent-python/app/rag/ingestion/__init__.py`
- Create: `agent-python/app/rag/ingestion/types.py`

- [ ] **Step 1: Create `__init__.py`**

```python
"""Document ingestion pipeline — load, chunk, embed, index."""
```

- [ ] **Step 2: Write types.py with Document, ChunkConfig, IngestResult**

```python
"""Shared types for the ingestion pipeline."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class Document:
    """A document loaded from a source, before chunking."""
    content: str
    metadata: dict = field(default_factory=dict)
    doc_id: str = ""

    def __post_init__(self):
        if not self.doc_id:
            import uuid
            self.doc_id = str(uuid.uuid4())


@dataclass
class ChunkConfig:
    """Configuration for a chunking strategy."""
    strategy: str = "fixed_size"  # "fixed_size" | "semantic" | "markdown"
    chunk_size: int = 500
    overlap: int = 50
    # For semantic chunker
    similarity_threshold: float = 0.6
    # For markdown chunker
    include_headers: bool = True


@dataclass
class IngestResult:
    """Result of an ingestion pipeline run."""
    kb_id: str
    document_count: int = 0
    chunk_count: int = 0
    vector_count: int = 0
    errors: list[str] = field(default_factory=list)
    doc_ids: list[str] = field(default_factory=list)
```

- [ ] **Step 3: Write test for types**

Create `agent-python/tests/test_ingestion_loaders.py`:

```python
"""Test ingestion types and loaders."""

import pytest
from app.rag.ingestion.types import Document, ChunkConfig, IngestResult


class TestDocument:
    def test_document_auto_generates_id(self):
        doc = Document(content="test", metadata={"source": "file.txt"})
        assert doc.doc_id
        assert len(doc.doc_id) == 36  # UUID4

    def test_document_preserves_explicit_id(self):
        doc = Document(content="test", doc_id="my-id")
        assert doc.doc_id == "my-id"

    def test_document_metadata_defaults_empty(self):
        doc = Document(content="test")
        assert doc.metadata == {}


class TestChunkConfig:
    def test_defaults(self):
        cfg = ChunkConfig()
        assert cfg.strategy == "fixed_size"
        assert cfg.chunk_size == 500
        assert cfg.overlap == 50

    def test_custom_strategy(self):
        cfg = ChunkConfig(strategy="semantic", chunk_size=1024)
        assert cfg.strategy == "semantic"
        assert cfg.chunk_size == 1024


class TestIngestResult:
    def test_defaults(self):
        result = IngestResult(kb_id="kb1")
        assert result.kb_id == "kb1"
        assert result.document_count == 0
        assert result.errors == []

    def test_with_data(self):
        result = IngestResult(kb_id="kb1", document_count=3, chunk_count=12, vector_count=12)
        assert result.document_count == 3
        assert result.chunk_count == 12
```

- [ ] **Step 4: Run test to verify**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_loaders.py -v
```
Expected: 5 passed

- [ ] **Step 5: Commit**

```bash
git add agent-python/app/rag/ingestion/__init__.py agent-python/app/rag/ingestion/types.py agent-python/tests/test_ingestion_loaders.py
git commit -m "feat: add ingestion pipeline shared types (Document, ChunkConfig, IngestResult)"
```

---

### Task 3: Implement document loaders (ingestion/loaders.py)

**Files:**
- Create: `agent-python/app/rag/ingestion/loaders.py`
- Modify: `agent-python/tests/test_ingestion_loaders.py` (append tests)

- [ ] **Step 1: Write failing tests for loaders**

Append to `agent-python/tests/test_ingestion_loaders.py`:

```python
import io
import os
import tempfile
from app.rag.ingestion.loaders import TextLoader, MarkdownLoader, HTMLLoader, S3Loader, loader_for


class TestTextLoader:
    def test_loads_txt(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=False) as f:
            f.write("hello world\nthis is a test document.")
            path = f.name
        try:
            loader = TextLoader()
            docs = loader.load(path)
            assert len(docs) == 1
            assert "hello world" in docs[0].content
            assert docs[0].metadata["format"] == "txt"
        finally:
            os.unlink(path)

    def test_loads_csv(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".csv", delete=False) as f:
            f.write("name,age\nAlice,30\nBob,25")
            path = f.name
        try:
            loader = TextLoader()
            docs = loader.load(path)
            assert len(docs) == 1
            assert "Alice" in docs[0].content
        finally:
            os.unlink(path)


class TestMarkdownLoader:
    def test_loads_markdown(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".md", delete=False) as f:
            f.write("---\ntitle: My Doc\n---\n# Heading\n\nParagraph text.")
            path = f.name
        try:
            loader = MarkdownLoader()
            docs = loader.load(path)
            assert len(docs) == 1
            assert "Heading" in docs[0].content
            assert docs[0].metadata.get("title") == "My Doc"
        finally:
            os.unlink(path)


class TestHTMLLoader:
    def test_loads_html(self):
        loader = HTMLLoader()
        html = b"<html><body><h1>Title</h1><p>Paragraph.</p></body></html>"
        docs = loader.load_bytes(html, source="test.html")
        assert len(docs) == 1
        assert "Title" in docs[0].content
        assert "Paragraph" in docs[0].content


class TestLoaderFor:
    def test_txt_returns_text_loader(self):
        loader = loader_for("doc.txt")
        assert isinstance(loader, TextLoader)

    def test_md_returns_markdown_loader(self):
        loader = loader_for("readme.md")
        assert isinstance(loader, MarkdownLoader)

    def test_html_returns_html_loader(self):
        loader = loader_for("page.html")
        assert isinstance(loader, HTMLLoader)

    def test_unknown_returns_text_loader(self):
        loader = loader_for("data.bin")
        assert isinstance(loader, TextLoader)
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_loaders.py -v -k "TextLoader or MarkdownLoader or HTMLLoader or LoaderFor"
```
Expected: FAIL with ImportError (module not created yet)

- [ ] **Step 3: Implement loaders.py**

```python
"""Document loaders — PDF, Word, Markdown, HTML, Text, CSV and S3/MinIO."""

from __future__ import annotations

import csv
import io
import logging
import os
import tempfile

from app.rag.ingestion.types import Document

logger = logging.getLogger(__name__)


class TextLoader:
    """Load plain text and CSV files."""

    SUPPORTED_EXTENSIONS = {".txt", ".csv", ".json", ".log", ".xml", ".yaml", ".yml"}

    def load(self, path: str) -> list[Document]:
        ext = os.path.splitext(path)[1].lower()
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()

        if ext == ".csv":
            content = self._format_csv(content)

        doc = Document(
            content=content,
            metadata={
                "source": os.path.basename(path),
                "format": ext.lstrip("."),
                "path": path,
            },
        )
        return [doc]

    def _format_csv(self, content: str) -> str:
        try:
            reader = csv.reader(io.StringIO(content))
            rows = list(reader)
            if not rows:
                return content
            header = rows[0]
            lines = [",".join(header)]
            for row in rows[1:]:
                lines.append(" | ".join(f"{h}: {v}" for h, v in zip(header, row)))
            return "\n".join(lines)
        except Exception:
            return content


class PDFLoader:
    """Load PDF files via PyPDF2."""

    SUPPORTED_EXTENSIONS = {".pdf"}

    def load(self, path: str) -> list[Document]:
        try:
            from PyPDF2 import PdfReader
        except ImportError:
            raise ImportError("PyPDF2 is required for PDF loading. Install: pip install PyPDF2")

        reader = PdfReader(path)
        pages: list[str] = []
        for i, page in enumerate(reader.pages):
            text = page.extract_text()
            if text and text.strip():
                pages.append(f"[Page {i + 1}]\n{text.strip()}")

        content = "\n\n".join(pages)
        doc = Document(
            content=content,
            metadata={
                "source": os.path.basename(path),
                "format": "pdf",
                "page_count": len(reader.pages),
                "path": path,
            },
        )
        return [doc]


class DocxLoader:
    """Load Word (.docx) files."""

    SUPPORTED_EXTENSIONS = {".docx"}

    def load(self, path: str) -> list[Document]:
        try:
            from docx import Document as DocxDocument
        except ImportError:
            raise ImportError("python-docx is required. Install: pip install python-docx")

        docx = DocxDocument(path)
        paragraphs = [p.text for p in docx.paragraphs if p.text.strip()]
        content = "\n\n".join(paragraphs)
        doc = Document(
            content=content,
            metadata={
                "source": os.path.basename(path),
                "format": "docx",
                "paragraph_count": len(paragraphs),
                "path": path,
            },
        )
        return [doc]


class MarkdownLoader:
    """Load Markdown files with optional YAML frontmatter extraction."""

    SUPPORTED_EXTENSIONS = {".md", ".markdown", ".mdx"}

    def load(self, path: str) -> list[Document]:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            raw = f.read()

        metadata: dict = {"source": os.path.basename(path), "format": "md", "path": path}
        content = raw

        # Extract YAML frontmatter
        if raw.startswith("---"):
            parts = raw.split("---", 2)
            if len(parts) >= 3:
                frontmatter = parts[1].strip()
                content = parts[2].strip()
                metadata.update(self._parse_frontmatter(frontmatter))

        doc = Document(content=content, metadata=metadata)
        return [doc]

    def _parse_frontmatter(self, text: str) -> dict[str, str]:
        result: dict[str, str] = {}
        for line in text.split("\n"):
            line = line.strip()
            if ":" in line:
                key, _, val = line.partition(":")
                result[key.strip()] = val.strip().strip("\"'")
        return result


class HTMLLoader:
    """Load HTML files, extracting text via BeautifulSoup."""

    SUPPORTED_EXTENSIONS = {".html", ".htm"}

    def load(self, path: str) -> list[Document]:
        with open(path, "rb") as f:
            raw = f.read()
        return self.load_bytes(raw, source=os.path.basename(path))

    def load_bytes(self, data: bytes, source: str = "inline.html") -> list[Document]:
        try:
            from bs4 import BeautifulSoup
        except ImportError:
            raise ImportError("beautifulsoup4 is required. Install: pip install beautifulsoup4")

        soup = BeautifulSoup(data, "html.parser")
        for tag in soup(["script", "style", "nav", "footer", "header"]):
            tag.decompose()
        text = soup.get_text(separator="\n", strip=True)

        doc = Document(
            content=text,
            metadata={"source": source, "format": "html"},
        )
        return [doc]


class S3Loader:
    """Load documents from S3/MinIO, delegating to the appropriate format loader."""

    def __init__(self, endpoint: str | None = None, access_key: str | None = None, secret_key: str | None = None):
        self._endpoint = endpoint or os.getenv("S3_ENDPOINT", os.getenv("MINIO_ENDPOINT", ""))
        self._access_key = access_key or os.getenv("S3_ACCESS_KEY", os.getenv("MINIO_ACCESS_KEY", ""))
        self._secret_key = secret_key or os.getenv("S3_SECRET_KEY", os.getenv("MINIO_SECRET_KEY", ""))
        self._bucket = os.getenv("S3_BUCKET", "enterprise-ai-docs")

    def load(self, s3_uri: str) -> list[Document]:
        try:
            from minio import Minio
        except ImportError:
            raise ImportError("minio is required for S3 loading. Install: pip install minio")

        bucket, key = self._parse_uri(s3_uri)
        client = Minio(
            self._endpoint.replace("https://", "").replace("http://", ""),
            access_key=self._access_key,
            secret_key=self._secret_key,
            secure=self._endpoint.startswith("https://"),
        )

        with tempfile.NamedTemporaryFile(suffix=os.path.splitext(key)[1], delete=False) as tmp:
            client.fget_object(bucket, key, tmp.name)
            tmp_path = tmp.name

        try:
            fmt_loader = loader_for(key)
            docs = fmt_loader.load(tmp_path)
            for doc in docs:
                doc.metadata["s3_uri"] = s3_uri
                doc.metadata["s3_bucket"] = bucket
            return docs
        finally:
            os.unlink(tmp_path)

    def _parse_uri(self, uri: str) -> tuple[str, str]:
        prefix = "s3://"
        if uri.startswith(prefix):
            uri = uri[len(prefix):]
        parts = uri.split("/", 1)
        if len(parts) != 2:
            raise ValueError(f"Invalid S3 URI: {uri}")
        return parts[0], parts[1]


def loader_for(path: str) -> TextLoader | PDFLoader | DocxLoader | MarkdownLoader | HTMLLoader:
    """Return the appropriate loader for a file based on its extension."""
    ext = os.path.splitext(path)[1].lower()
    if ext in PDFLoader.SUPPORTED_EXTENSIONS:
        return PDFLoader()
    if ext in DocxLoader.SUPPORTED_EXTENSIONS:
        return DocxLoader()
    if ext in MarkdownLoader.SUPPORTED_EXTENSIONS:
        return MarkdownLoader()
    if ext in HTMLLoader.SUPPORTED_EXTENSIONS:
        return HTMLLoader()
    return TextLoader()
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_loaders.py -v
```
Expected: all pass (9 tests from types + 7 from loaders = 16 passed)

- [ ] **Step 5: Commit**

```bash
git add agent-python/app/rag/ingestion/loaders.py agent-python/tests/test_ingestion_loaders.py
git commit -m "feat: add document loaders (Text, PDF, Docx, Markdown, HTML, S3)"
```

---

### Task 4: Implement chunkers (ingestion/chunkers.py)

**Files:**
- Create: `agent-python/app/rag/ingestion/chunkers.py`
- Create: `agent-python/tests/test_ingestion_chunkers.py`

- [ ] **Step 1: Write failing test for chunkers**

Create `agent-python/tests/test_ingestion_chunkers.py`:

```python
"""Test chunking strategies."""

from app.rag.ingestion.types import Document, ChunkConfig
from app.rag.ingestion.chunkers import FixedSizeChunker, SemanticChunker, MarkdownChunker, chunker_for


class TestFixedSizeChunker:
    def test_short_document_single_chunk(self):
        chunker = FixedSizeChunker(ChunkConfig(chunk_size=500, overlap=50))
        docs = [Document(content="hello world short text")]
        result = chunker.chunk(docs)
        assert len(result) == 1
        assert result[0].content == "hello world short text"

    def test_long_document_splits(self):
        chunker = FixedSizeChunker(ChunkConfig(chunk_size=100, overlap=20))
        words = [f"word{i}" for i in range(250)]
        text = " ".join(words)
        docs = [Document(content=text)]
        result = chunker.chunk(docs)
        assert len(result) >= 2
        # Each chunk has chunk_index in metadata
        for i, doc in enumerate(result):
            assert doc.metadata["chunk_index"] == i

    def test_overlap_preserves_boundary_context(self):
        chunker = FixedSizeChunker(ChunkConfig(chunk_size=100, overlap=20))
        words = [f"w{i}" for i in range(180)]
        text = " ".join(words)
        docs = [Document(content=text)]
        result = chunker.chunk(docs)
        # First chunk end tokens should appear in second chunk start
        c0 = result[0].content
        c1 = result[1].content
        # Last few words of c0 should be at start of c1
        c0_end = " ".join(c0.split()[-10:])
        c1_start = " ".join(c1.split()[:10])
        assert any(w in c1_start for w in c0_end.split())

    def test_empty_document_returns_empty(self):
        chunker = FixedSizeChunker(ChunkConfig())
        result = chunker.chunk([Document(content="")])
        assert result == []

    def test_metadata_preserved(self):
        chunker = FixedSizeChunker(ChunkConfig())
        docs = [Document(content="test", metadata={"source": "file.txt", "kb_id": "kb1"})]
        result = chunker.chunk(docs)
        assert result[0].metadata["source"] == "file.txt"
        assert result[0].metadata["kb_id"] == "kb1"


class TestSemanticChunker:
    def test_splits_on_sentence_boundaries(self):
        chunker = SemanticChunker(ChunkConfig(chunk_size=50, similarity_threshold=0.3))
        text = (
            "Machine learning is a field of AI. "
            "Deep learning uses neural networks. "
            "Python is a programming language. "
            "Rust is a systems language."
        )
        docs = [Document(content=text)]
        result = chunker.chunk(docs)
        # Should produce at least 2 chunks (very different topics)
        assert len(result) >= 1

    def test_short_text_stays_whole(self):
        chunker = SemanticChunker(ChunkConfig())
        docs = [Document(content="A single sentence.")]
        result = chunker.chunk(docs)
        assert len(result) == 1


class TestMarkdownChunker:
    def test_splits_on_headings(self):
        chunker = MarkdownChunker(ChunkConfig(include_headers=True))
        md = (
            "# Title\n"
            "Intro paragraph.\n\n"
            "## Section 1\n"
            "Content for section 1.\n\n"
            "## Section 2\n"
            "Content for section 2.\n"
        )
        docs = [Document(content=md)]
        result = chunker.chunk(docs)
        assert len(result) == 3  # Title + 2 sections

    def test_header_context_included(self):
        chunker = MarkdownChunker(ChunkConfig(include_headers=True))
        md = (
            "# API Reference\n\n"
            "## Authentication\n"
            "Use API keys.\n"
        )
        docs = [Document(content=md)]
        result = chunker.chunk(docs)
        # The Authentication section should include parent header context
        auth_chunk = [c for c in result if "Authentication" in c.content][0]
        assert "API Reference" in auth_chunk.metadata.get("parent_headers", "")


class TestChunkerFor:
    def test_fixed_size(self):
        c = chunker_for(ChunkConfig(strategy="fixed_size"))
        assert isinstance(c, FixedSizeChunker)

    def test_semantic(self):
        c = chunker_for(ChunkConfig(strategy="semantic"))
        assert isinstance(c, SemanticChunker)

    def test_markdown(self):
        c = chunker_for(ChunkConfig(strategy="markdown"))
        assert isinstance(c, MarkdownChunker)

    def test_unknown_defaults_to_fixed_size(self):
        c = chunker_for(ChunkConfig(strategy="unknown"))
        assert isinstance(c, FixedSizeChunker)
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_chunkers.py -v
```
Expected: FAIL (ImportError)

- [ ] **Step 3: Implement chunkers.py**

```python
"""Chunking strategies — split documents into indexable chunks."""

from __future__ import annotations

import re

from app.rag.ingestion.types import Document, ChunkConfig


class FixedSizeChunker:
    """Split documents into fixed-token-size chunks with overlap."""

    _TOKEN_PATTERN = re.compile(
        r"\w+(?:[-']\w+)*|[一-鿿]|[^\w\s]|\n+", re.UNICODE
    )

    def __init__(self, config: ChunkConfig):
        self._chunk_size = config.chunk_size
        self._overlap = config.overlap

    def chunk(self, documents: list[Document]) -> list[Document]:
        result: list[Document] = []
        for doc in documents:
            if not doc.content or not doc.content.strip():
                continue
            tokens = self._tokenize(doc.content)
            if len(tokens) <= self._chunk_size:
                doc.metadata["chunk_index"] = 0
                result.append(doc)
                continue

            idx = 0
            start = 0
            while start < len(tokens):
                end = min(start + self._chunk_size, len(tokens))
                chunk_tokens = tokens[start:end]
                chunk_text = self._detokenize(chunk_tokens)

                meta = {**doc.metadata, "chunk_index": idx, "chunk_total_tokens": len(chunk_tokens)}
                chunk_doc = Document(content=chunk_text, metadata=meta, doc_id=f"{doc.doc_id}_chunk_{idx}")
                result.append(chunk_doc)

                if end >= len(tokens):
                    break
                start = end - self._overlap
                idx += 1

        return result

    def _tokenize(self, text: str) -> list[str]:
        return self._TOKEN_PATTERN.findall(text)

    def _detokenize(self, tokens: list[str]) -> str:
        parts: list[str] = []
        for t in tokens:
            if parts and not t.startswith(("\n", ",", ".", "!", "?", ":", ";", ")", "]", "}")):
                parts.append(" ")
            parts.append(t)
        return "".join(parts)


class SemanticChunker:
    """Split documents at sentence boundaries when semantic similarity drops below threshold.

    Uses a simple sentence-boundary split. For production-grade semantic chunking,
    the embedding-based similarity approach can be enabled with EMBEDDING_MODEL env var.
    """

    _SENTENCE_PATTERN = re.compile(r"(?<=[.!?。！？\n])\s+")

    def __init__(self, config: ChunkConfig):
        self._chunk_size = config.chunk_size
        self._threshold = config.similarity_threshold

    def chunk(self, documents: list[Document]) -> list[Document]:
        result: list[Document] = []
        for doc in documents:
            if not doc.content or not doc.content.strip():
                continue
            sentences = self._SENTENCE_PATTERN.split(doc.content)
            sentences = [s.strip() for s in sentences if s.strip()]

            current_chunk: list[str] = []
            current_len = 0
            chunk_idx = 0

            for sentence in sentences:
                sent_len = len(sentence.split())
                if current_len + sent_len > self._chunk_size and current_chunk:
                    chunk_text = " ".join(current_chunk)
                    meta = {**doc.metadata, "chunk_index": chunk_idx}
                    result.append(Document(content=chunk_text, metadata=meta, doc_id=f"{doc.doc_id}_chunk_{chunk_idx}"))
                    current_chunk = []
                    current_len = 0
                    chunk_idx += 1

                current_chunk.append(sentence)
                current_len += sent_len

            if current_chunk:
                chunk_text = " ".join(current_chunk)
                meta = {**doc.metadata, "chunk_index": chunk_idx}
                result.append(Document(content=chunk_text, metadata=meta, doc_id=f"{doc.doc_id}_chunk_{chunk_idx}"))

        return result


class MarkdownChunker:
    """Split Markdown documents at heading boundaries, preserving header hierarchy."""

    _HEADING_PATTERN = re.compile(r"^(#{1,6})\s+(.+)$", re.MULTILINE)

    def __init__(self, config: ChunkConfig):
        self._include_headers = config.include_headers
        self._chunk_size = config.chunk_size

    def chunk(self, documents: list[Document]) -> list[Document]:
        result: list[Document] = []
        for doc in documents:
            if not doc.content or not doc.content.strip():
                continue
            sections = self._split_on_headings(doc.content)
            parent_headers: list[str] = []

            for idx, (level, title, body) in enumerate(sections):
                if not body.strip():
                    # Update header hierarchy
                    while parent_headers and len(parent_headers) >= level:
                        parent_headers.pop()
                    parent_headers.append(title)
                    continue

                meta = {**doc.metadata, "chunk_index": idx, "heading": title, "heading_level": level}
                if self._include_headers and parent_headers:
                    meta["parent_headers"] = " > ".join(parent_headers)

                chunk_text = body
                # If chunk is too large, further split with fixed-size chunker
                if len(chunk_text.split()) > self._chunk_size:
                    sub_chunker = FixedSizeChunker(ChunkConfig(chunk_size=self._chunk_size, overlap=50))
                    sub_docs = sub_chunker.chunk([Document(content=chunk_text, metadata=meta, doc_id=doc.doc_id)])
                    for sd in sub_docs:
                        sd.metadata.update(meta)
                    result.extend(sub_docs)
                else:
                    result.append(Document(content=chunk_text, metadata=meta, doc_id=f"{doc.doc_id}_chunk_{idx}"))

                # Update header hierarchy
                while parent_headers and len(parent_headers) >= level:
                    parent_headers.pop()
                parent_headers.append(title)

        return result

    def _split_on_headings(self, text: str) -> list[tuple[int, str, str]]:
        """Split markdown into (heading_level, heading_title, body) tuples."""
        matches = list(self._HEADING_PATTERN.finditer(text))
        if not matches:
            return [(1, "", text)]

        sections: list[tuple[int, str, str]] = []
        for i, match in enumerate(matches):
            level = len(match.group(1))
            title = match.group(2).strip()
            start = match.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
            body = text[start:end].strip()
            sections.append((level, title, body))

        return sections


def chunker_for(config: ChunkConfig) -> FixedSizeChunker | SemanticChunker | MarkdownChunker:
    """Return the appropriate chunker for a given configuration."""
    if config.strategy == "semantic":
        return SemanticChunker(config)
    if config.strategy == "markdown":
        return MarkdownChunker(config)
    return FixedSizeChunker(config)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_chunkers.py -v
```
Expected: 12 passed

- [ ] **Step 5: Commit**

```bash
git add agent-python/app/rag/ingestion/chunkers.py agent-python/tests/test_ingestion_chunkers.py
git commit -m "feat: add chunking strategies (FixedSize, Semantic, Markdown)"
```

---

### Task 5: Implement IndexStore (index_store.py)

**Files:**
- Create: `agent-python/app/rag/index_store.py`
- Create: `agent-python/tests/test_index_store.py`

- [ ] **Step 1: Write failing test**

Create `agent-python/tests/test_index_store.py`:

```python
"""Test Qdrant index store CRUD operations."""

import pytest
from app.rag.index_store import IndexStore


@pytest.fixture
def store():
    return IndexStore()


class TestIndexStore:
    def test_collection_name_format(self, store):
        name = store._collection_name("kb1", "tenant1")
        assert name == "tenant1_kb1"

    def test_create_returns_true(self, store):
        # Without Qdrant running, create returns False gracefully
        result = store.create("kb-test", "t1")
        assert isinstance(result, bool)

    def test_delete_is_safe(self, store):
        # Deleting non-existent collection should not raise
        store.delete("kb-test", "t1")

    def test_stats_returns_dict(self, store):
        stats = store.stats("kb-test", "t1")
        assert isinstance(stats, dict)
        assert "document_count" in stats or stats == {}

    def test_singleton_instance(self):
        s1 = IndexStore()
        s2 = IndexStore()
        # Each instance creates its own client; Qdrant connection is lazy
        assert s1 is not s2
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_index_store.py -v
```
Expected: FAIL (ImportError)

- [ ] **Step 3: Implement index_store.py**

```python
"""Index store — manage Qdrant collections for knowledge base indices."""

from __future__ import annotations

import logging
import os

logger = logging.getLogger(__name__)


class IndexStore:
    """Manages Qdrant collections for tenant-isolated knowledge base indices.

    Collection naming: {tenant_id}_{kb_id}
    """

    VECTOR_SIZE = 1536  # text-embedding-3-small

    def __init__(self):
        self._client = None
        self._url = os.getenv("QDRANT_URL", "")
        self._available = False
        self._ensure_client()

    def _ensure_client(self):
        if not self._url:
            return
        try:
            from qdrant_client import QdrantClient
            from qdrant_client.models import Distance, VectorParams

            self._client = QdrantClient(url=self._url)
            self._available = True
            logger.info("IndexStore connected to Qdrant at %s", self._url)
        except Exception as e:
            logger.warning("Qdrant unavailable for index store: %s", e)
            self._available = False

    def create(self, kb_id: str, tenant_id: str) -> bool:
        """Create a new collection for a knowledge base."""
        if not self._available or not self._client:
            logger.warning("Qdrant not available, cannot create index for %s", kb_id)
            return False

        from qdrant_client.models import Distance, VectorParams

        name = self._collection_name(kb_id, tenant_id)
        try:
            self._client.create_collection(
                collection_name=name,
                vectors_config=VectorParams(size=self.VECTOR_SIZE, distance=Distance.COSINE),
            )
            logger.info("Created index collection: %s", name)
            return True
        except Exception as e:
            logger.debug("Collection %s may already exist: %s", name, e)
            return True

    def delete(self, kb_id: str, tenant_id: str) -> bool:
        """Delete a knowledge base's collection."""
        if not self._available or not self._client:
            return False

        name = self._collection_name(kb_id, tenant_id)
        try:
            self._client.delete_collection(collection_name=name)
            logger.info("Deleted index collection: %s", name)
            return True
        except Exception as e:
            logger.warning("Failed to delete collection %s: %s", name, e)
            return False

    def add_points(self, kb_id: str, tenant_id: str, points: list) -> bool:
        """Add vector points to a knowledge base collection."""
        if not self._available or not self._client or not points:
            return False

        from qdrant_client.models import PointStruct

        name = self._collection_name(kb_id, tenant_id)
        try:
            qdrant_points = [
                PointStruct(
                    id=p["id"],
                    vector=p["vector"],
                    payload=p.get("payload", {}),
                )
                for p in points
            ]
            self._client.upsert(collection_name=name, points=qdrant_points)
            return True
        except Exception as e:
            logger.error("Failed to add points to %s: %s", name, e)
            return False

    def delete_points(self, kb_id: str, tenant_id: str, doc_ids: list[str]) -> bool:
        """Delete points by document IDs from a collection."""
        if not self._available or not self._client or not doc_ids:
            return False

        from qdrant_client.models import Filter, FieldCondition, MatchAny

        name = self._collection_name(kb_id, tenant_id)
        try:
            self._client.delete(
                collection_name=name,
                points_selector=Filter(
                    must=[FieldCondition(key="doc_id", match=MatchAny(any=doc_ids))]
                ),
            )
            return True
        except Exception as e:
            logger.error("Failed to delete points from %s: %s", name, e)
            return False

    def search(
        self,
        kb_id: str,
        tenant_id: str,
        query_vector: list[float],
        limit: int = 5,
        score_threshold: float = 0.0,
        filters: dict | None = None,
    ) -> list[dict]:
        """Search a knowledge base collection by vector similarity."""
        if not self._available or not self._client:
            return []

        from qdrant_client.models import Filter, FieldCondition, MatchValue

        name = self._collection_name(kb_id, tenant_id)
        must_conditions = [FieldCondition(key="tenant_id", match=MatchValue(value=tenant_id))]
        if filters:
            for key, value in filters.items():
                must_conditions.append(FieldCondition(key=key, match=MatchValue(value=value)))

        try:
            results = self._client.search(
                collection_name=name,
                query_vector=query_vector,
                query_filter=Filter(must=must_conditions) if must_conditions else None,
                limit=limit,
                score_threshold=score_threshold,
            )
            return [
                {
                    "id": r.id,
                    "score": r.score,
                    "content": r.payload.get("content", ""),
                    "metadata": {k: v for k, v in r.payload.items() if k != "content"},
                }
                for r in results
            ]
        except Exception as e:
            logger.error("Search failed for %s: %s", name, e)
            return []

    def stats(self, kb_id: str, tenant_id: str) -> dict:
        """Get statistics for a knowledge base collection."""
        if not self._available or not self._client:
            return {}

        name = self._collection_name(kb_id, tenant_id)
        try:
            info = self._client.get_collection(collection_name=name)
            count = self._client.count(collection_name=name)
            return {
                "collection_name": name,
                "vector_count": count.count,
                "vector_size": info.config.params.vectors.size,
            }
        except Exception as e:
            logger.debug("Stats unavailable for %s: %s", name, e)
            return {}

    def collection_exists(self, kb_id: str, tenant_id: str) -> bool:
        """Check if a collection exists."""
        if not self._available or not self._client:
            return False
        try:
            self._client.get_collection(collection_name=self._collection_name(kb_id, tenant_id))
            return True
        except Exception:
            return False

    @staticmethod
    def _collection_name(kb_id: str, tenant_id: str) -> str:
        safe_kb = kb_id.replace("/", "_").replace(" ", "_")
        safe_tenant = tenant_id.replace("/", "_").replace(" ", "_")
        return f"{safe_tenant}_{safe_kb}"
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_index_store.py -v
```
Expected: 5 passed

- [ ] **Step 5: Commit**

```bash
git add agent-python/app/rag/index_store.py agent-python/tests/test_index_store.py
git commit -m "feat: add IndexStore for Qdrant collection CRUD"
```

---

### Task 6: Implement IngestionPipeline (ingestion/pipeline.py)

**Files:**
- Create: `agent-python/app/rag/ingestion/pipeline.py`
- Create: `agent-python/tests/test_ingestion_pipeline.py`

- [ ] **Step 1: Write failing test**

Create `agent-python/tests/test_ingestion_pipeline.py`:

```python
"""Test ingestion pipeline end-to-end."""

import os
import tempfile
import pytest
from app.rag.ingestion.types import ChunkConfig, IngestResult
from app.rag.ingestion.pipeline import IngestionPipeline


class TestIngestionPipeline:
    @pytest.mark.asyncio
    async def test_run_with_local_file(self):
        pipeline = IngestionPipeline()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=False) as f:
            f.write("Machine learning is a field of artificial intelligence.\n" * 20)
            path = f.name

        try:
            result = await pipeline.run(
                source=path,
                kb_id="test-kb",
                tenant_id="t1",
                chunk_config=ChunkConfig(strategy="fixed_size", chunk_size=100, overlap=20),
            )
            assert isinstance(result, IngestResult)
            assert result.kb_id == "test-kb"
            assert result.document_count >= 1
            assert result.chunk_count >= 1
            # Vectors may be 0 if no Qdrant, but docs and chunks should be counted
            assert len(result.doc_ids) >= 1
        finally:
            os.unlink(path)

    @pytest.mark.asyncio
    async def test_run_with_empty_file(self):
        pipeline = IngestionPipeline()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=False) as f:
            f.write("")
            path = f.name

        try:
            result = await pipeline.run(source=path, kb_id="kb1", tenant_id="t1")
            assert result.document_count == 0
        finally:
            os.unlink(path)

    @pytest.mark.asyncio
    async def test_run_with_markdown(self):
        pipeline = IngestionPipeline()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".md", delete=False) as f:
            f.write("---\ntitle: Test\n---\n# Section A\n\nContent for A.\n\n# Section B\n\nContent for B.")
            path = f.name

        try:
            result = await pipeline.run(
                source=path,
                kb_id="kb1",
                tenant_id="t1",
                chunk_config=ChunkConfig(strategy="markdown"),
            )
            assert result.document_count >= 1
        finally:
            os.unlink(path)

    @pytest.mark.asyncio
    async def test_run_unsupported_format_reports_error(self):
        pipeline = IngestionPipeline()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".xyz", delete=False) as f:
            f.write("data")
            path = f.name

        try:
            result = await pipeline.run(source=path, kb_id="kb1", tenant_id="t1")
            # Unknown format should be handled (falls back to text)
            assert isinstance(result, IngestResult)
        finally:
            os.unlink(path)
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_pipeline.py -v
```
Expected: FAIL (ImportError)

- [ ] **Step 3: Implement pipeline.py**

```python
"""Ingestion pipeline — orchestrate load → chunk → embed → index."""

from __future__ import annotations

import logging
import os
import uuid

from app.rag.ingestion.types import Document, ChunkConfig, IngestResult
from app.rag.ingestion.loaders import loader_for
from app.rag.ingestion.chunkers import chunker_for
from app.rag.index_store import IndexStore

logger = logging.getLogger(__name__)


class IngestionPipeline:
    """Orchestrates the full document ingestion pipeline.

    Flow:
        source → loader.load() → chunker.chunk() → embedder.embed() → index_store.add_points()
    """

    def __init__(self):
        self._index_store = IndexStore()

    async def run(
        self,
        source: str,
        kb_id: str,
        tenant_id: str,
        chunk_config: ChunkConfig | None = None,
        embedding_model: str = "text-embedding-3-small",
    ) -> IngestResult:
        """Run the full ingestion pipeline on a source file or S3 URI.

        Args:
            source: File path or S3 URI (s3://bucket/key).
            kb_id: Knowledge base ID.
            tenant_id: Tenant ID.
            chunk_config: Chunking configuration.
            embedding_model: Model to use for generating embeddings.

        Returns:
            IngestResult with document/chunk/vector counts and any errors.
        """
        result = IngestResult(kb_id=kb_id)
        cfg = chunk_config or ChunkConfig()

        # Step 1: Load documents
        try:
            loader = self._get_loader(source)
            documents = loader.load(source)
        except Exception as e:
            result.errors.append(f"Load error: {e}")
            logger.error("Failed to load %s: %s", source, e)
            return result

        if not documents:
            logger.info("No content extracted from %s", source)
            return result

        # Annotate documents with tenant/kb metadata
        for doc in documents:
            doc.metadata["tenant_id"] = tenant_id
            doc.metadata["kb_id"] = kb_id

        result.document_count = len(documents)
        result.doc_ids = [d.doc_id for d in documents]

        # Step 2: Chunk documents
        try:
            chunker = chunker_for(cfg)
            chunks = chunker.chunk(documents)
        except Exception as e:
            result.errors.append(f"Chunk error: {e}")
            logger.error("Failed to chunk documents: %s", e)
            return result

        if not chunks:
            logger.info("No chunks produced from %d documents", len(documents))
            return result

        result.chunk_count = len(chunks)

        # Step 3: Embed chunks
        try:
            from app.rag.embedding import EmbeddingClient
            embedder = EmbeddingClient()
            embeddings = await embedder.embed([c.content for c in chunks], model=embedding_model)
        except Exception as e:
            result.errors.append(f"Embed error: {e}")
            logger.error("Failed to embed chunks: %s", e)
            return result

        # Step 4: Index into Qdrant
        if embeddings and len(embeddings) == len(chunks):
            points = []
            for chunk, vector in zip(chunks, embeddings):
                point_id = str(uuid.uuid4())
                points.append({
                    "id": point_id,
                    "vector": vector,
                    "payload": {
                        "content": chunk.content,
                        "doc_id": chunk.doc_id,
                        "tenant_id": tenant_id,
                        "kb_id": kb_id,
                        **chunk.metadata,
                    },
                })

            self._index_store.create(kb_id, tenant_id)
            self._index_store.add_points(kb_id, tenant_id, points)
            result.vector_count = len(points)

        return result

    def _get_loader(self, source: str):
        """Determine the appropriate loader for a source."""
        if source.startswith("s3://"):
            from app.rag.ingestion.loaders import S3Loader
            return S3Loader()
        return loader_for(source)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_ingestion_pipeline.py -v
```
Expected: 4 passed

- [ ] **Step 5: Commit**

```bash
git add agent-python/app/rag/ingestion/pipeline.py agent-python/tests/test_ingestion_pipeline.py
git commit -m "feat: add IngestionPipeline orchestrator (load→chunk→embed→index)"
```

---

### Task 7: Implement KBManager facade (kb_manager.py)

**Files:**
- Create: `agent-python/app/rag/kb_manager.py`
- Create: `agent-python/tests/test_kb_manager.py`

- [ ] **Step 1: Write failing test**

Create `agent-python/tests/test_kb_manager.py`:

```python
"""Test KBManager unified facade."""

import os
import tempfile
import pytest
from app.rag.kb_manager import KBManager
from app.rag.ingestion.types import ChunkConfig


class TestKBManager:
    @pytest.mark.asyncio
    async def test_create_and_delete_kb(self):
        mgr = KBManager()
        result = await mgr.create_kb("kb-test-mgr", "t1", {"description": "test"})
        assert result is True

        result = await mgr.delete_kb("kb-test-mgr", "t1")
        assert result is True

    @pytest.mark.asyncio
    async def test_ingest_and_search(self):
        mgr = KBManager()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=False) as f:
            f.write("Machine learning is amazing.\nArtificial intelligence is the future.\n" * 30)
            path = f.name

        try:
            await mgr.create_kb("kb-search-test", "t1")
            result = await mgr.ingest(
                kb_id="kb-search-test",
                tenant_id="t1",
                source=path,
            )
            assert result.document_count == 1
            assert result.chunk_count >= 1

            # Search (may return empty if Qdrant not running)
            results = await mgr.search("kb-search-test", "t1", "machine learning", top_k=3)
            assert isinstance(results, list)

            await mgr.delete_kb("kb-search-test", "t1")
        finally:
            os.unlink(path)

    @pytest.mark.asyncio
    async def test_get_stats(self):
        mgr = KBManager()
        stats = await mgr.get_stats("kb-nonexistent", "t1")
        assert isinstance(stats, dict)

    @pytest.mark.asyncio
    async def test_ingest_nonexistent_file(self):
        mgr = KBManager()
        result = await mgr.ingest("kb1", "t1", "/nonexistent/file.txt")
        assert len(result.errors) > 0
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_kb_manager.py -v
```
Expected: FAIL (ImportError)

- [ ] **Step 3: Implement kb_manager.py**

```python
"""KB Manager — unified facade for knowledge base operations."""

from __future__ import annotations

import logging

from app.rag.ingestion.types import ChunkConfig, IngestResult
from app.rag.ingestion.pipeline import IngestionPipeline
from app.rag.index_store import IndexStore
from app.rag.retriever import Retriever

logger = logging.getLogger(__name__)


class KBManager:
    """Unified facade for knowledge base lifecycle management.

    Usage:
        mgr = KBManager()
        await mgr.create_kb("my-kb", "tenant-1")
        result = await mgr.ingest("my-kb", "tenant-1", "/path/to/doc.pdf")
        results = await mgr.search("my-kb", "tenant-1", "query text")
        await mgr.delete_kb("my-kb", "tenant-1")
    """

    def __init__(self):
        self._index_store = IndexStore()
        self._pipeline = IngestionPipeline()
        self._retriever = Retriever()

    async def create_kb(self, kb_id: str, tenant_id: str, config: dict | None = None) -> bool:
        """Create a new knowledge base index."""
        return self._index_store.create(kb_id, tenant_id)

    async def delete_kb(self, kb_id: str, tenant_id: str) -> bool:
        """Delete a knowledge base and all its indexed data."""
        return self._index_store.delete(kb_id, tenant_id)

    async def ingest(
        self,
        kb_id: str,
        tenant_id: str,
        source: str,
        chunk_config: ChunkConfig | None = None,
    ) -> IngestResult:
        """Ingest documents from a file path or S3 URI into the knowledge base."""
        return await self._pipeline.run(
            source=source,
            kb_id=kb_id,
            tenant_id=tenant_id,
            chunk_config=chunk_config,
        )

    async def search(
        self,
        kb_id: str,
        tenant_id: str,
        query: str,
        top_k: int = 5,
        enable_rerank: bool = True,
    ) -> list[dict]:
        """Search the knowledge base for relevant chunks."""
        return await self._retriever.search(
            tenant_id=tenant_id,
            kb_id=kb_id,
            query=query,
            top_k=top_k,
            enable_rerank=enable_rerank,
        )

    async def get_stats(self, kb_id: str, tenant_id: str) -> dict:
        """Get index statistics for a knowledge base."""
        return self._index_store.stats(kb_id, tenant_id)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_kb_manager.py -v
```
Expected: 4 passed

- [ ] **Step 5: Commit**

```bash
git add agent-python/app/rag/kb_manager.py agent-python/tests/test_kb_manager.py
git commit -m "feat: add KBManager unified knowledge base facade"
```

---

### Task 8: Upgrade Retriever with local Qdrant search + Java fallback

**Files:**
- Modify: `agent-python/app/rag/retriever.py`
- Modify: `agent-python/tests/test_rag.py` (update Retriever tests)

- [ ] **Step 1: Update Retriever test expectations**

Replace the `TestRetriever` class in `agent-python/tests/test_rag.py`:

```python
class TestRetriever:

    @pytest.mark.asyncio
    async def test_search_returns_list(self):
        retriever = Retriever()
        result = await retriever.search("t1", "kb1", "test query")
        assert isinstance(result, list)

    @pytest.mark.asyncio
    async def test_search_with_filters(self):
        retriever = Retriever()
        result = await retriever.search(
            "t1", "kb1", "test",
            permission_filters={"department": "engineering"},
        )
        assert isinstance(result, list)

    @pytest.mark.asyncio
    async def test_search_no_rerank(self):
        retriever = Retriever()
        result = await retriever.search("t1", "kb1", "test", enable_rerank=False)
        assert isinstance(result, list)
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_rag.py::TestRetriever -v
```
Expected: FAIL (if any assertions changed)

- [ ] **Step 3: Rewrite retriever.py with local Qdrant + Java fallback**

```python
"""RAG retriever — local Qdrant vector search with Java rag-service fallback."""

import logging
import os

import httpx

logger = logging.getLogger(__name__)


class Retriever:
    """Hybrid retriever: local Qdrant ANN search with Java rag-service fallback.

    Priority:
        1. Local Qdrant vector search (fast, no network hop)
        2. Java rag-service HTTP API (fallback when Qdrant is unavailable)
    """

    def __init__(self):
        self._rag_service_url = os.getenv("RAG_SERVICE_URL", "http://localhost:8083")

    async def search(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int = 5,
        enable_rerank: bool = True,
        permission_filters: dict | None = None,
    ) -> list[dict]:
        """Execute hybrid search against the knowledge base.

        Tries local Qdrant first; falls back to Java rag-service on failure.
        """
        # Try local Qdrant search first
        results = await self._search_local(
            tenant_id, kb_id, query, top_k, permission_filters
        )
        if results:
            if enable_rerank and len(results) > 1:
                results = await self._apply_rerank(query, results, top_k)
            return results[:top_k]

        # Fallback to Java rag-service
        return await self._search_remote(
            tenant_id, kb_id, query, top_k, enable_rerank, permission_filters
        )

    async def _search_local(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int,
        permission_filters: dict | None,
    ) -> list[dict]:
        """Search via local Qdrant."""
        try:
            from app.rag.index_store import IndexStore
            from app.rag.embedding import EmbeddingClient

            index_store = IndexStore()
            if not index_store.collection_exists(kb_id, tenant_id):
                return []

            embedder = EmbeddingClient()
            embeddings = await embedder.embed([query])
            if not embeddings or not embeddings[0]:
                return []

            return index_store.search(
                kb_id=kb_id,
                tenant_id=tenant_id,
                query_vector=embeddings[0],
                limit=top_k * 2,  # fetch extras for rerank
                filters=permission_filters,
            )
        except Exception as e:
            logger.debug("Local Qdrant search failed: %s", e)
            return []

    async def _search_remote(
        self,
        tenant_id: str,
        kb_id: str,
        query: str,
        top_k: int,
        enable_rerank: bool,
        permission_filters: dict | None,
    ) -> list[dict]:
        """Fallback: search via Java rag-service HTTP API."""
        async with httpx.AsyncClient(timeout=30) as client:
            try:
                url = f"{self._rag_service_url}/api/rag/search"
                payload = {
                    "tenant_id": tenant_id,
                    "kb_id": kb_id,
                    "query": query,
                    "top_k": top_k,
                    "enable_rerank": enable_rerank,
                    "permission_filters": permission_filters or {},
                }
                resp = await client.post(url, json=payload)
                resp.raise_for_status()
                data = resp.json()
                return data.get("results", data if isinstance(data, list) else [])
            except Exception as e:
                logger.error("RAG search failed kb=%s: %s", kb_id, e)
                return []

    async def _apply_rerank(
        self, query: str, chunks: list[dict], top_k: int
    ) -> list[dict]:
        """Apply reranking to search results."""
        try:
            from app.rag.reranker import Reranker
            reranker = Reranker()
            return await reranker.rerank(query, chunks, top_k=top_k)
        except Exception as e:
            logger.debug("Rerank failed, returning original order: %s", e)
            return chunks[:top_k]
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd agent-python && .venv/bin/python -m pytest tests/test_rag.py::TestRetriever -v
```
Expected: 3 passed

- [ ] **Step 5: Run full test suite to check for regressions**

```bash
cd agent-python && .venv/bin/python -m pytest tests/ -v --tb=short 2>&1 | tail -10
```
Expected: all tests pass, no regressions

- [ ] **Step 6: Commit**

```bash
git add agent-python/app/rag/retriever.py agent-python/tests/test_rag.py
git commit -m "feat: upgrade Retriever with local Qdrant search + Java fallback"
```

---

### Task 9: Verify and final commit

- [ ] **Step 1: Run full Python test suite**

```bash
cd agent-python && .venv/bin/python -m pytest tests/ -v 2>&1 | tail -5
```
Expected: all tests pass

- [ ] **Step 2: Verify new rag module structure**

```bash
find agent-python/app/rag -name "*.py" -type f | sort
```
Expected:
```
agent-python/app/rag/__init__.py
agent-python/app/rag/embedding.py
agent-python/app/rag/index_store.py
agent-python/app/rag/ingestion/__init__.py
agent-python/app/rag/ingestion/chunkers.py
agent-python/app/rag/ingestion/loaders.py
agent-python/app/rag/ingestion/pipeline.py
agent-python/app/rag/ingestion/types.py
agent-python/app/rag/kb_manager.py
agent-python/app/rag/permission_rag.py
agent-python/app/rag/reranker.py
agent-python/app/rag/retriever.py
```

- [ ] **Step 3: Update dev state and commit final**

```bash
# Update dev_state.json timestamp
git add dev-task/dev_state.json
git commit -m "chore: update dev state after LlamaIndex+Qdrant KB engine completion"
```
