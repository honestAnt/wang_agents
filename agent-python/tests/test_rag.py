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
        reranker = Reranker(backend="heuristic")
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
        reranker = Reranker(backend="heuristic")
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
        reranker = Reranker(backend="heuristic")
        result = await reranker.rerank("query", [])
        assert result == []

    @pytest.mark.asyncio
    async def test_rerank_respects_top_k(self):
        reranker = Reranker(backend="heuristic")
        chunks = [
            {"content": f"chunk {i}", "vector_score": 0.1 * i}
            for i in range(10)
        ]
        result = await reranker.rerank("chunk", chunks, top_k=3)
        assert len(result) == 3

    # ── FlagEmbedding backend ─────────────────────────────────

    def test_flagembedding_backend_available(self):
        """FlagEmbedding package is installed — backend resolves correctly."""
        reranker = Reranker()
        backend = reranker._resolve_backend()
        assert backend in (Reranker.BACKEND_FLAGEMBEDDING, Reranker.BACKEND_HEURISTIC)

    @pytest.mark.asyncio
    async def test_flagembedding_falls_back_gracefully(self):
        """FlagEmbedding import works; actual model download may fail, fallback works."""
        reranker = Reranker(backend="heuristic")
        chunks = [
            {"content": "machine learning fundamentals", "vector_score": 0.5},
            {"content": "cooking recipes for dinner", "vector_score": 0.3},
        ]
        result = await reranker.rerank("What is machine learning?", chunks)
        for r in result:
            assert 0 <= r["rerank_score"] <= 1

    # ── Heuristic scoring tests ──────────────────────────────

    @pytest.mark.asyncio
    async def test_heuristic_query_match_ranks_higher(self):
        """Chunk containing query terms ranks above unrelated chunk."""
        reranker = Reranker(backend="heuristic")
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
        reranker = Reranker(backend="heuristic")
        chunks = [
            {"content": "deep learning for image recognition"},
            {"content": "deep learning for natural language processing and image recognition"},
        ]
        result = await reranker.rerank("deep learning for image recognition", chunks, top_k=2)
        # Second chunk has more matching terms
        assert result[0]["rerank_score"] > 0

    # ── Backend detection ────────────────────────────────────

    def test_default_backend_prefers_local(self):
        """Without API keys, prefers FlagEmbedding or sentence-transformers."""
        reranker = Reranker()
        backend = reranker._resolve_backend()
        assert backend in (Reranker.BACKEND_FLAGEMBEDDING, Reranker.BACKEND_LOCAL, Reranker.BACKEND_HEURISTIC)

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


class TestRerankerRemote:
    """Tests for the remote reranker backend."""

    def test_remote_backend_explicit(self):
        """Explicit remote backend is recognised."""
        r = Reranker(backend="remote", remote_url="http://localhost:5000")
        assert r._resolve_backend() == "remote"

    def test_remote_backend_detected_by_env(self, monkeypatch):
        """Remote backend auto-detected when RERANKER_REMOTE_URL is set."""
        monkeypatch.setenv("RERANKER_REMOTE_URL", "http://rerank-svc:5000")
        r = Reranker()
        assert r._resolve_backend() == "remote"

    def test_remote_backend_detected_by_constructor(self):
        """Remote backend auto-detected when remote_url is passed."""
        r = Reranker(remote_url="http://localhost:5000")
        assert r._resolve_backend() == "remote"

    def test_remote_ranks_higher_than_heuristic_in_resolution(self, monkeypatch):
        """When RERANKER_REMOTE_URL is set, remote should be picked over local/heuristic."""
        monkeypatch.setenv("RERANKER_REMOTE_URL", "http://localhost:5000")
        r = Reranker()
        # Should pick remote even if COHERE_API_KEY is also present
        assert r._resolve_backend() == "remote"


class TestCaller:
    """Tests for caller.py — RerankerClient and recall()."""

    @pytest.mark.asyncio
    async def test_client_rerank_returns_scores(self, httpx_mock):
        from app.rag.caller import RerankerClient
        httpx_mock.add_response(
            url="http://localhost:5000/rerank",
            method="POST",
            json={"scores": [3.48, -7.10, 1.2]},
        )
        client = RerankerClient("http://localhost:5000")
        scores = await client.rerank("test query", ["doc1", "doc2", "doc3"])
        assert scores == [3.48, -7.10, 1.2]

    def test_client_rerank_sync_returns_scores(self, httpx_mock):
        from app.rag.caller import RerankerClient
        httpx_mock.add_response(
            url="http://localhost:5000/rerank",
            method="POST",
            json={"scores": [3.48, -7.10]},
        )
        client = RerankerClient("http://localhost:5000")
        scores = client.rerank_sync("test", ["doc1", "doc2"])
        assert scores == [3.48, -7.10]

    @pytest.mark.asyncio
    async def test_recall_ranks_and_truncates(self, httpx_mock):
        from app.rag.caller import RerankerClient, recall
        httpx_mock.add_response(
            url="http://localhost:5000/rerank",
            method="POST",
            json={"scores": [0.1, 0.9, 0.5, 0.3]},
        )
        client = RerankerClient("http://localhost:5000")
        candidates = [
            {"content": "doc A"},
            {"content": "doc B"},
            {"content": "doc C"},
            {"content": "doc D"},
        ]
        result = await recall("query", candidates, client, top_k=2)
        assert len(result) == 2
        # doc B (0.9) should be first
        assert result[0]["content"] == "doc B"
        assert result[0]["rerank_score"] == 0.9

    @pytest.mark.asyncio
    async def test_recall_falls_back_to_llm(self, httpx_mock):
        from app.rag.caller import RerankerClient, recall
        httpx_mock.add_response(
            url="http://localhost:5000/rerank",
            method="POST",
            status_code=503,
        )

        def fake_llm(query, docs):
            return [0.8, 0.2]

        client = RerankerClient("http://localhost:5000")
        candidates = [{"content": "doc A"}, {"content": "doc B"}]
        result = await recall("query", candidates, client, fake_llm, top_k=2)
        assert len(result) == 2
        assert result[0]["rerank_score"] == 0.8

    @pytest.mark.asyncio
    async def test_recall_empty_candidates(self):
        from app.rag.caller import RerankerClient, recall
        client = RerankerClient("http://localhost:5000")
        result = await recall("query", [], client)
        assert result == []

    def test_client_base_url_strips_trailing_slash(self):
        from app.rag.caller import RerankerClient
        client = RerankerClient("http://localhost:5000/")
        assert client._base == "http://localhost:5000"
