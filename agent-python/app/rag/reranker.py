"""Reranker — re-ranks retrieved chunks for relevance with multi-backend support.

Backends (auto-detected, in priority order):
  flagembedding — BAAI FlagEmbedding FlagReranker (local, free, best quality)
  remote        — call a separate reranker service via POST /rerank
  local         — sentence-transformers cross-encoder fallback
  cohere        — Cohere Rerank API
  jina          — Jina Reranker API
  heuristic     — TF-IDF–style word overlap (zero deps, always available)
"""

from __future__ import annotations

import math
import os
import re
from typing import Any

logger = __import__("logging").getLogger(__name__)


class Reranker:
    """Re-ranks search result chunks by computing true query–chunk relevance.

    Uses FlagEmbedding (BAAI/bge-reranker-v2-m3) by default for free local
    reranking. Falls back through sentence-transformers → API backends →
    heuristic word-overlap scorer.
    """

    BACKEND_FLAGEMBEDDING = "flagembedding"
    BACKEND_REMOTE = "remote"
    BACKEND_LOCAL = "local"
    BACKEND_COHERE = "cohere"
    BACKEND_JINA = "jina"
    BACKEND_HEURISTIC = "heuristic"

    DEFAULT_MODEL = "BAAI/bge-reranker-v2-m3"

    def __init__(
        self,
        model: str | None = None,
        backend: str | None = None,
        top_k: int = 5,
        remote_url: str | None = None,
    ):
        self.model = model or self.DEFAULT_MODEL
        self._top_k = top_k
        self._backend: str | None = backend
        self._remote_url: str | None = remote_url
        self._reranker: Any = None  # lazy-loaded FlagReranker or CrossEncoder

    # ── Public API ────────────────────────────────────────────

    async def rerank(
        self,
        query: str,
        chunks: list[dict],
        top_k: int | None = None,
    ) -> list[dict]:
        """Re-rank *chunks* by relevance to *query* and return top_k results.

        Each chunk must have at least a ``"content"`` key. Existing
        ``vector_score`` or ``rerank_score`` values are blended into the
        final score so semantic signal from earlier pipeline stages is
        never discarded.
        """
        if not chunks:
            return []

        backend = self._resolve_backend()
        k = top_k if top_k is not None else self._top_k

        if backend == self.BACKEND_FLAGEMBEDDING:
            try:
                scores = self._rerank_flagembedding(query, chunks)
            except Exception as e:
                logger.warning("FlagEmbedding failed, falling back to heuristic: %s", e)
                scores = self._rerank_heuristic(query, chunks)
        elif backend == self.BACKEND_REMOTE:
            scores = await self._rerank_remote(query, chunks)
        elif backend == self.BACKEND_LOCAL:
            scores = await self._rerank_local(query, chunks)
        elif backend == self.BACKEND_COHERE:
            scores = await self._rerank_cohere(query, chunks)
        elif backend == self.BACKEND_JINA:
            scores = await self._rerank_jina(query, chunks)
        else:
            scores = self._rerank_heuristic(query, chunks)

        # Attach scores — blend with prior signal
        for i, chunk in enumerate(chunks):
            prior = chunk.get("rerank_score", chunk.get("vector_score", 0))
            chunk["rerank_score"] = round(0.7 * scores[i] + 0.3 * prior, 6)
            chunk["_relevance_raw"] = round(scores[i], 6)

        ranked = sorted(chunks, key=lambda c: c["rerank_score"], reverse=True)
        return ranked[:k]

    # ── Backend resolution ────────────────────────────────────

    def _resolve_backend(self) -> str:
        """Pick the best available backend: explicit > env > auto-detect."""
        if self._backend:
            return self._backend

        # Check env-driven backends
        if self._remote_url or os.getenv("RERANKER_REMOTE_URL"):
            return self.BACKEND_REMOTE
        if os.getenv("COHERE_API_KEY"):
            return self.BACKEND_COHERE
        if os.getenv("JINA_API_KEY"):
            return self.BACKEND_JINA

        # Prefer FlagEmbedding (free, local, best BGE quality)
        if self._can_use_flagembedding():
            return self.BACKEND_FLAGEMBEDDING

        # Fall back to sentence-transformers
        if self._can_use_local():
            return self.BACKEND_LOCAL

        return self.BACKEND_HEURISTIC

    def _can_use_flagembedding(self) -> bool:
        try:
            from FlagEmbedding import FlagReranker  # noqa: F401
            return True
        except ImportError:
            return False

    def _can_use_local(self) -> bool:
        try:
            import sentence_transformers  # noqa: F401
            return True
        except ImportError:
            return False

    # ── FlagEmbedding (primary local) ──────────────────────────

    def _rerank_flagembedding(self, query: str, chunks: list[dict]) -> list[float]:
        """Use BAAI FlagEmbedding FlagReranker — free, local, state-of-the-art."""
        if self._reranker is None:
            from FlagEmbedding import FlagReranker
            try:
                self._reranker = FlagReranker(
                    self.model,
                    use_fp16=True,
                )
            except Exception as e:
                logger.warning(
                    "FlagEmbedding model '%s' failed to load (huggingface.co may be unreachable): %s. "
                    "Falling back to heuristic reranker.",
                    self.model, e,
                )
                raise RuntimeError(f"FlagEmbedding unavailable: {e}") from e

        documents = [c.get("content", "") for c in chunks]
        pairs = [[query, doc] for doc in documents]
        raw = self._reranker.compute_score(pairs, normalize=True)

        scores = [float(s) for s in raw] if isinstance(raw, list) else [float(raw)]
        return scores

    # ── Local cross-encoder (fallback) ─────────────────────────

    async def _rerank_local(self, query: str, chunks: list[dict]) -> list[float]:
        """Use a sentence-transformers CrossEncoder model locally."""
        if self._reranker is None:
            from sentence_transformers import CrossEncoder
            self._reranker = CrossEncoder(
                self.model,
                trust_remote_code=True,
            )

        documents = [c.get("content", "") for c in chunks]
        pairs = [[query, doc] for doc in documents]

        raw_scores = self._reranker.predict(
            pairs,
            batch_size=min(32, len(pairs)),
            show_progress_bar=False,
        )

        scores = [float(s) for s in raw_scores]
        if scores:
            min_s, max_s = min(scores), max(scores)
            if max_s > min_s:
                scores = [(s - min_s) / (max_s - min_s) for s in scores]
            else:
                scores = [0.5] * len(scores)

        return scores

    # ── Remote reranker service ─────────────────────────────────

    async def _rerank_remote(self, query: str, chunks: list[dict]) -> list[float]:
        """Call an external reranker service via POST /rerank."""
        import httpx

        url = self._remote_url or os.getenv("RERANKER_REMOTE_URL", "http://localhost:5000")
        documents = [c.get("content", "") for c in chunks]

        async with httpx.AsyncClient(timeout=httpx.Timeout(30.0)) as client:
            response = await client.post(
                f"{url.rstrip('/')}/rerank",
                headers={"Content-Type": "application/json"},
                json={"query": query, "documents": documents},
            )
            response.raise_for_status()
            data = response.json()

        scores: list[float] = data.get("scores", [])
        if len(scores) != len(chunks):
            logger.warning(
                "Remote reranker returned %d scores for %d chunks; padding with 0.0",
                len(scores), len(chunks),
            )
            scores += [0.0] * (len(chunks) - len(scores))
        return [float(s) for s in scores]

    # ── Heuristic (zero-dependency) ────────────────────────────

    def _rerank_heuristic(self, query: str, chunks: list[dict]) -> list[float]:
        """TF-IDF–inspired word-overlap scorer. Always available."""
        query_terms = self._tokenize(query)
        if not query_terms:
            return [0.0] * len(chunks)

        idf = self._compute_idf(query_terms, chunks)
        scores: list[float] = []

        for chunk in chunks:
            content = chunk.get("content", "")
            content_terms = self._tokenize(content)
            if not content_terms:
                scores.append(0.0)
                continue

            score = 0.0
            for term in query_terms:
                tf = content_terms.count(term) / len(content_terms)
                score += tf * idf.get(term, 0.0)
            scores.append(score)

        max_s = max(scores) if scores else 1.0
        if max_s > 0:
            scores = [s / max_s for s in scores]

        return scores

    @staticmethod
    def _tokenize(text: str) -> list[str]:
        tokens = re.split(r"[^a-zA-Z0-9一-鿿]+", text.lower())
        return [t for t in tokens if len(t) > 1]

    @staticmethod
    def _compute_idf(query_terms: list[str], chunks: list[dict]) -> dict[str, float]:
        N = len(chunks)
        idf: dict[str, float] = {}
        for term in set(query_terms):
            df = sum(1 for c in chunks if term in c.get("content", "").lower())
            idf[term] = math.log((N + 1) / (df + 1)) + 1.0
        return idf

    # ── Cohere API ─────────────────────────────────────────────

    async def _rerank_cohere(self, query: str, chunks: list[dict]) -> list[float]:
        import httpx

        api_key = os.getenv("COHERE_API_KEY", "")
        documents = [c.get("content", "") for c in chunks]

        async with httpx.AsyncClient(timeout=httpx.Timeout(30.0)) as client:
            response = await client.post(
                "https://api.cohere.com/v2/rerank",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model if self.model != self.DEFAULT_MODEL else "rerank-v3.5",
                    "query": query,
                    "documents": documents,
                    "top_n": len(chunks),
                },
            )
            response.raise_for_status()
            data = response.json()

        score_map: dict[int, float] = {}
        for item in data.get("results", []):
            score_map[item["index"]] = item.get("relevance_score", 0.0)
        return [score_map.get(i, 0.0) for i in range(len(chunks))]

    # ── Jina API ───────────────────────────────────────────────

    async def _rerank_jina(self, query: str, chunks: list[dict]) -> list[float]:
        import httpx

        api_key = os.getenv("JINA_API_KEY", "")
        documents = [c.get("content", "") for c in chunks]

        async with httpx.AsyncClient(timeout=httpx.Timeout(30.0)) as client:
            response = await client.post(
                "https://api.jina.ai/v1/rerank",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "query": query,
                    "documents": documents,
                    "top_n": len(chunks),
                },
            )
            response.raise_for_status()
            data = response.json()

        score_map: dict[int, float] = {}
        for item in data.get("results", []):
            score_map[item["index"]] = item.get("relevance_score", 0.0)
        return [score_map.get(i, 0.0) for i in range(len(chunks))]
