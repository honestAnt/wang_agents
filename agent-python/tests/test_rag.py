"""Test RAG components — embedding, reranker, permission-aware RAG, retriever."""

import pytest
from app.rag.embedding import EmbeddingClient
from app.rag.reranker import Reranker
from app.rag.permission_rag import PermissionAwareRAG
from app.rag.retriever import Retriever


class TestEmbeddingClient:

    @pytest.mark.asyncio
    async def test_embed_returns_correct_dimensions(self):
        client = EmbeddingClient()
        embeddings = await client.embed(["hello world", "test"])
        assert len(embeddings) == 2
        assert len(embeddings[0]) == 1536

    @pytest.mark.asyncio
    async def test_embed_empty_list(self):
        client = EmbeddingClient()
        embeddings = await client.embed([])
        assert len(embeddings) == 0


class TestReranker:

    @pytest.mark.asyncio
    async def test_rerank_sorts_by_score(self):
        reranker = Reranker()
        chunks = [
            {"content": "low", "vector_score": 0.3},
            {"content": "high", "vector_score": 0.9},
            {"content": "mid", "vector_score": 0.6},
        ]
        result = await reranker.rerank("query", chunks, top_k=2)
        assert len(result) == 2
        assert result[0]["vector_score"] == 0.9
        assert result[1]["vector_score"] == 0.6

    @pytest.mark.asyncio
    async def test_rerank_uses_rerank_score_when_available(self):
        reranker = Reranker()
        chunks = [
            {"content": "a", "rerank_score": 0.5, "vector_score": 0.9},
            {"content": "b", "rerank_score": 0.8, "vector_score": 0.1},
        ]
        result = await reranker.rerank("query", chunks, top_k=2)
        assert result[0]["rerank_score"] == 0.8


class TestPermissionAwareRAG:

    def test_build_basic_filters(self):
        rag = PermissionAwareRAG()
        filters = rag.build_permission_filters("tenant-1")
        assert filters["tenant_id"] == "tenant-1"
        assert "department" not in filters

    def test_build_full_filters(self):
        rag = PermissionAwareRAG()
        filters = rag.build_permission_filters(
            "t1", user_id="u1", department="engineering", role="developer"
        )
        assert filters["tenant_id"] == "t1"
        assert filters["department"] == "engineering"
        assert filters["allowed_roles"] == "developer"


class TestRetriever:

    @pytest.mark.asyncio
    async def test_search_returns_list(self):
        retriever = Retriever()
        result = await retriever.search("t1", "kb1", "test query")
        assert isinstance(result, list)
        assert len(result) == 0  # placeholder returns empty
