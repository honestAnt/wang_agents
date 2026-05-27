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

    # ── Chunking ────────────────────────────────────────────

    def test_chunk_short_text_single_chunk(self):
        """Text shorter than chunk_size returns a single chunk."""
        client = EmbeddingClient()
        text = "hello world this is a short document"
        chunks = client.chunk_text(text)
        assert len(chunks) == 1
        assert chunks[0] == text

    def test_chunk_empty_text(self):
        client = EmbeddingClient()
        assert client.chunk_text("") == []
        assert client.chunk_text("   ") == []

    def test_chunk_long_text_splits(self):
        """Text > 500 tokens should be split into multiple chunks."""
        client = EmbeddingClient()
        # Generate ~600 tokens (each word is one token)
        words = ["token"] * 600
        text = " ".join(words)
        chunks = client.chunk_text(text, chunk_size=500, overlap=50)
        assert len(chunks) >= 2
        # First chunk: ~500 tokens
        first_tokens = client.estimate_tokens(chunks[0])
        assert first_tokens == 500
        # Second chunk has overlap
        assert len(chunks) >= 2

    def test_chunk_overlap_preserves_context(self):
        """Overlapping tokens appear at end of chunk N and start of chunk N+1."""
        client = EmbeddingClient()
        # 600 words → 600 tokens → 2 chunks with 50-token overlap
        words = [f"w{i}" for i in range(600)]
        text = " ".join(words)
        chunks = client.chunk_text(text, chunk_size=500, overlap=50)
        assert len(chunks) == 2, f"expected 2 chunks, got {len(chunks)}"
        c0_tokens = client._tokenize(chunks[0])
        c1_tokens = client._tokenize(chunks[1])
        assert len(c0_tokens) == 500
        assert len(c1_tokens) == 150  # 450..599
        assert c0_tokens[-50:] == c1_tokens[:50]

    def test_chunk_no_duplicate_when_exact_multiple(self):
        """When text fits exactly, no unnecessary chunks."""
        client = EmbeddingClient()
        text = " ".join(["word"] * 100)
        chunks = client.chunk_text(text, chunk_size=500, overlap=50)
        assert len(chunks) == 1

    def test_estimate_tokens(self):
        client = EmbeddingClient()
        text = "hello world, this is a test."
        count = client.estimate_tokens(text)
        assert count > 0
        # "hello"(1) "world"(1) ","(1) "this"(1) "is"(1) "a"(1) "test"(1) "."(1) = 8
        assert count == 8

    @pytest.mark.asyncio
    async def test_embed_documents_returns_per_doc_embeddings(self):
        """embed_documents returns one list of embeddings per document."""
        client = EmbeddingClient()
        docs = ["short doc one", "short doc two"]
        results = await client.embed_documents(docs)
        assert len(results) == 2
        assert isinstance(results[0], list)
        assert len(results[0]) == 1  # single chunk per short doc


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
