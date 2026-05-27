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
