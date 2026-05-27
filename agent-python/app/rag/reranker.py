"""Reranker — re-ranks retrieved chunks for relevance with multi-backend support.

Backends (auto-detected, in priority order):
  local     — sentence-transformers cross-encoder (BGE, etc.)
  cohere    — Cohere Rerank API
  jina      — Jina Reranker API
  heuristic — TF-IDF–style word overlap with existing-score blending (no deps)
"""

from __future__ import annotations

import math
import os
import re
from typing import Any


class Reranker:
    """Re-ranks search result chunks by computing true query–chunk relevance.

    Uses cross-encoder models or external rerank APIs. Falls back to a
    heuristic word-overlap scorer when no model or API key is configured.
    """

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
    ):
        self.model = model or self.DEFAULT_MODEL
        self._top_k = top_k
        self._backend: str | None = backend
        self._cross_encoder: Any = None  # lazy-loaded CrossEncoder instance

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

        if backend == self.BACKEND_LOCAL:
            scores = await self._rerank_local(query, chunks)
        elif backend == self.BACKEND_COHERE:
            scores = await self._rerank_cohere(query, chunks)
        elif backend == self.BACKEND_JINA:
            scores = await self._rerank_jina(query, chunks)
        else:
            scores = self._rerank_heuristic(query, chunks)

        # Attach scores
        for i, chunk in enumerate(chunks):
            prior = chunk.get("rerank_score", chunk.get("vector_score", 0))
            chunk["rerank_score"] = round(0.7 * scores[i] + 0.3 * prior, 6)
            chunk["_relevance_raw"] = round(scores[i], 6)

        # Sort a copy so the caller's list order is preserved
        ranked = sorted(chunks, key=lambda c: c["rerank_score"], reverse=True)
        return ranked[:k]

    # ── Backend resolution ────────────────────────────────────

    def _resolve_backend(self) -> str:
        """Pick the best available backend: explicit > env > auto-detect."""
        if self._backend:
            return self._backend

        # Check env-driven backends first
        if os.getenv("COHERE_API_KEY"):
            return self.BACKEND_COHERE
        if os.getenv("JINA_API_KEY"):
            return self.BACKEND_JINA

        # Try local cross-encoder
        if self._can_use_local():
            return self.BACKEND_LOCAL

        return self.BACKEND_HEURISTIC

    def _can_use_local(self) -> bool:
        try:
            import sentence_transformers  # noqa: F401
            return True
        except ImportError:
            return False

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

            # TF × IDF sum over query terms
            score = 0.0
            for term in query_terms:
                tf = content_terms.count(term) / len(content_terms)
                score += tf * idf.get(term, 0.0)
            scores.append(score)

        # Normalize to [0, 1]
        max_s = max(scores) if scores else 1.0
        if max_s > 0:
            scores = [s / max_s for s in scores]

        return scores

    @staticmethod
    def _tokenize(text: str) -> list[str]:
        """Lowercase + split on non-alphanumeric, drop short tokens."""
        tokens = re.split(r"[^a-zA-Z0-9一-鿿]+", text.lower())
        return [t for t in tokens if len(t) > 1]

    @staticmethod
    def _compute_idf(query_terms: list[str], chunks: list[dict]) -> dict[str, float]:
        """IDF = log((N + 1) / (df + 1)) + 1 smoothing."""
        N = len(chunks)
        idf: dict[str, float] = {}
        for term in set(query_terms):
            df = sum(1 for c in chunks if term in c.get("content", "").lower())
            idf[term] = math.log((N + 1) / (df + 1)) + 1.0
        return idf

    # ── Cohere API ─────────────────────────────────────────────

    async def _rerank_cohere(self, query: str, chunks: list[dict]) -> list[float]:
        """Call Cohere Rerank API (https://docs.cohere.com/reference/rerank)."""
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

        # Build score array preserving original order
        score_map: dict[int, float] = {}
        for item in data.get("results", []):
            score_map[item["index"]] = item.get("relevance_score", 0.0)
        return [score_map.get(i, 0.0) for i in range(len(chunks))]

    # ── Jina API ───────────────────────────────────────────────

    async def _rerank_jina(self, query: str, chunks: list[dict]) -> list[float]:
        """Call Jina Reranker API (https://jina.ai/reranker)."""
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

    # ── Local cross-encoder ────────────────────────────────────

    async def _rerank_local(self, query: str, chunks: list[dict]) -> list[float]:
        """Use a sentence-transformers CrossEncoder model locally."""
        if self._cross_encoder is None:
            from sentence_transformers import CrossEncoder
            self._cross_encoder = CrossEncoder(
                self.model,
                trust_remote_code=True,
            )

        documents = [c.get("content", "") for c in chunks]
        pairs = [[query, doc] for doc in documents]

        # CrossEncoder.predict runs in a thread pool; call synchronously for simplicity
        raw_scores = self._cross_encoder.predict(
            pairs,
            batch_size=min(32, len(pairs)),
            show_progress_bar=False,
        )

        # Normalize sigmoid scores to [0, 1]
        scores = [float(s) for s in raw_scores]
        if scores:
            min_s, max_s = min(scores), max(scores)
            if max_s > min_s:
                scores = [(s - min_s) / (max_s - min_s) for s in scores]
            else:
                scores = [0.5] * len(scores)

        return scores
