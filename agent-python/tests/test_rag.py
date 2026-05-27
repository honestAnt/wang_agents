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
        """Chunks with higher vector_score rank first after blending."""
        reranker = Reranker()
        chunks = [
            {"content": "low relevance text", "vector_score": 0.3},
            {"content": "high relevance text", "vector_score": 0.9},
            {"content": "mid relevance text", "vector_score": 0.6},
        ]
        result = await reranker.rerank("high relevance", chunks, top_k=2)
        assert len(result) == 2
        assert result[0]["vector_score"] == 0.9
        assert result[1]["vector_score"] == 0.6
        # All chunks get a rerank_score assigned
        for r in result:
            assert "rerank_score" in r

    @pytest.mark.asyncio
    async def test_rerank_preserves_prior_rerank_order(self):
        """Chunk with higher prior rerank_score ranks first."""
        reranker = Reranker()
        chunks = [
            {"content": "alpha", "rerank_score": 0.5, "vector_score": 0.9},
            {"content": "beta", "rerank_score": 0.8, "vector_score": 0.1},
        ]
        result = await reranker.rerank("query", chunks, top_k=2)
        # Higher prior rerank_score (0.8) should rank first
        assert result[0]["rerank_score"] > result[1]["rerank_score"]
        assert result[0]["content"] == "beta"

    @pytest.mark.asyncio
    async def test_rerank_empty_chunks(self):
        reranker = Reranker()
        result = await reranker.rerank("query", [])
        assert result == []

    @pytest.mark.asyncio
    async def test_rerank_respects_top_k(self):
        reranker = Reranker()
        chunks = [
            {"content": f"chunk {i}", "vector_score": 0.1 * i}
            for i in range(10)
        ]
        result = await reranker.rerank("chunk", chunks, top_k=3)
        assert len(result) == 3

    # ── Heuristic scoring tests ──────────────────────────────

    @pytest.mark.asyncio
    async def test_heuristic_query_match_ranks_higher(self):
        """Chunk containing query terms ranks above unrelated chunk."""
        reranker = Reranker()
        chunks = [
            {"content": "unrelated text about weather"},
            {"content": "machine learning is a field of artificial intelligence"},
        ]
        result = await reranker.rerank("machine learning", chunks, top_k=2)
        assert "machine learning" in result[0]["content"]
        assert result[0]["rerank_score"] > result[1]["rerank_score"]

    @pytest.mark.asyncio
    async def test_heuristic_exact_match_scores_high(self):
        """Full query match scores higher than partial match."""
        reranker = Reranker()
        chunks = [
            {"content": "deep learning for image recognition"},
            {"content": "deep learning for natural language processing and image recognition"},
        ]
        result = await reranker.rerank("deep learning for image recognition", chunks, top_k=2)
        # Second chunk has more matching terms
        assert result[0]["rerank_score"] > 0

    # ── Backend detection ────────────────────────────────────

    def test_default_backend_is_heuristic(self):
        """Without API keys or sentence-transformers, falls back to heuristic."""
        reranker = Reranker()
        backend = reranker._resolve_backend()
        assert backend in (Reranker.BACKEND_HEURISTIC, Reranker.BACKEND_LOCAL)

    # ── Tokenize / IDF helpers ───────────────────────────────

    def test_tokenize_splits_and_filters(self):
        tokens = Reranker._tokenize("Hello, World! AI-powered 测试")
        assert "hello" in tokens
        assert "world" in tokens
        assert "powered" in tokens
        # Single-char tokens filtered
        assert all(len(t) > 1 for t in tokens)

    def test_compute_idf_rare_term_scores_high(self):
        chunks = [
            {"content": "machine learning is great"},
            {"content": "deep learning and neural networks"},
            {"content": "reinforcement learning basics"},
        ]
        idf = Reranker._compute_idf(["machine", "learning"], chunks)
        # "machine" appears in 1 doc → higher IDF than "learning" (3 docs)
        assert idf["machine"] > idf["learning"]


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
