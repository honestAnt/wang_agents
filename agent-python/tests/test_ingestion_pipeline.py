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
    async def test_nonexistent_file_reports_error(self):
        pipeline = IngestionPipeline()
        result = await pipeline.run(source="/nonexistent/path.txt", kb_id="kb1", tenant_id="t1")
        assert isinstance(result, IngestResult)
        assert len(result.errors) > 0
