"""Lightweight module for calling a remote reranker service.

Provides a simple RerankerClient and a recall() helper that works with
the POST /rerank API. Designed for external callers that only need
reranking without the full RAG pipeline.
"""

from __future__ import annotations

import logging
from typing import Callable

import httpx

logger = logging.getLogger(__name__)


class RerankerClient:
    """Thin HTTP client for a remote reranker service (POST /rerank).

    Usage::

        client = RerankerClient("http://localhost:6000")
        scores = await client.rerank("query", ["doc1", "doc2"])
        # -> [3.48, -7.10]
    """

    def __init__(self, base_url: str, timeout: float = 30.0):
        self._base = base_url.rstrip("/")
        self._timeout = timeout

    async def rerank(self, query: str, documents: list[str]) -> list[float]:
        """Call POST /rerank and return relevance scores (higher = better)."""
        async with httpx.AsyncClient(timeout=httpx.Timeout(self._timeout)) as client:
            response = await client.post(
                f"{self._base}/rerank",
                headers={"Content-Type": "application/json"},
                json={"query": query, "documents": documents},
            )
            response.raise_for_status()
            data = response.json()
        return [float(s) for s in data.get("scores", [])]

    def rerank_sync(self, query: str, documents: list[str]) -> list[float]:
        """Synchronous wrapper for non-async callers."""
        with httpx.Client(timeout=self._timeout) as client:
            response = client.post(
                f"{self._base}/rerank",
                headers={"Content-Type": "application/json"},
                json={"query": query, "documents": documents},
            )
            response.raise_for_status()
            data = response.json()
        return [float(s) for s in data.get("scores", [])]


async def recall(
    query: str,
    candidates: list[dict],
    client: RerankerClient,
    llm_rerank: Callable[..., list[float]] | None = None,
    top_k: int = 5,
) -> list[dict]:
    """Re-rank *candidates* against *query* using a remote reranker.

    Tries the remote service first. If it fails or returns empty scores,
    falls back to *llm_rerank* (when provided).

    Each candidate is a dict with at least a ``"content"`` key. Results
    are returned sorted by relevance score, truncated to *top_k*.

    Example::

        from caller import recall, RerankerClient

        client = RerankerClient("http://localhost:5000")

        def my_llm_rerank(query, docs):
            # fallback: call an LLM to rank
            ...

        result = await recall("Python web framework", candidates, client, my_llm_rerank)
    """
    if not candidates:
        return []

    documents = [c.get("content", "") for c in candidates]
    scores: list[float] = []

    try:
        scores = await client.rerank(query, documents)
    except Exception as exc:
        logger.warning("Remote reranker unavailable: %s", exc)

    if not scores and llm_rerank is not None:
        try:
            scores = llm_rerank(query, documents)
        except Exception as exc:
            logger.warning("LLM rerank fallback also failed: %s", exc)

    if not scores:
        # No scores available — return top_k unsorted candidates
        return candidates[:top_k]

    # Attach scores and sort
    for i, c in enumerate(candidates):
        c["rerank_score"] = round(scores[i] if i < len(scores) else 0.0, 6)

    ranked = sorted(candidates, key=lambda c: c.get("rerank_score", 0), reverse=True)
    return ranked[:top_k]
