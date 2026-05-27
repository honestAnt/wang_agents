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


# ── Loader tests ──────────────────────────────────────────────

import io
import os
import tempfile
from app.rag.ingestion.loaders import TextLoader, MarkdownLoader, HTMLLoader, loader_for


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
