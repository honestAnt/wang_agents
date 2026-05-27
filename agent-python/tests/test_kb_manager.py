"""Test KBManager unified facade."""

import os
import tempfile
import pytest
from app.rag.kb_manager import KBManager


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
            result = await mgr.ingest(kb_id="kb-search-test", tenant_id="t1", source=path)
            assert result.document_count == 1
            assert result.chunk_count >= 1

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
