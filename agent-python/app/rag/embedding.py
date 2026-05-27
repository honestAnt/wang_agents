"""Embedding client — generates embeddings via AgentScope ModelWrapper.

Supports text chunking (500 token chunks with 50 token overlap) for
embedding long documents that exceed the model's context window.
"""

from __future__ import annotations

import re

from app.llm.model_wrapper import ModelWrapper


class EmbeddingClient:
    """Generates vector embeddings for text, with built-in chunking.

    Long texts are automatically split into overlapping chunks of
    ~500 tokens to stay within embedding model input limits.
    """

    DEFAULT_MODEL = "text-embedding-3-small"
    DEFAULT_DIMENSIONS = 1536

    # Chunking defaults
    CHUNK_SIZE = 500   # target tokens per chunk
    CHUNK_OVERLAP = 50  # token overlap between adjacent chunks

    def __init__(self):
        self._wrapper = ModelWrapper()

    # ── Public API ────────────────────────────────────────────

    async def embed(
        self,
        texts: list[str],
        model: str = DEFAULT_MODEL,
    ) -> list[list[float]]:
        """Embed pre-chunked texts directly. No further splitting.

        Use this when texts are already chunked to the right size.
        For raw documents, use :meth:`embed_documents` instead.
        """
        return await self._wrapper.embed(texts, model)

    async def embed_documents(
        self,
        documents: list[str],
        model: str = DEFAULT_MODEL,
        chunk_size: int = CHUNK_SIZE,
        overlap: int = CHUNK_OVERLAP,
    ) -> list[list[list[float]]]:
        """Chunk long documents, then embed each chunk.

        Returns a list-of-lists: one list of embedding vectors per document.
        """
        results: list[list[list[float]]] = []
        for doc in documents:
            chunks = self.chunk_text(doc, chunk_size, overlap)
            if not chunks:
                results.append([])
            else:
                embeddings = await self._wrapper.embed(chunks, model)
                results.append(embeddings)
        return results

    def chunk_text(
        self,
        text: str,
        chunk_size: int = CHUNK_SIZE,
        overlap: int = CHUNK_OVERLAP,
    ) -> list[str]:
        """Split *text* into overlapping chunks of ~*chunk_size* tokens.

        Overlap of *overlap* tokens between consecutive chunks preserves
        context across chunk boundaries.

        Returns a single-element list if the text fits in one chunk.
        """
        if not text or not text.strip():
            return []

        tokens = self._tokenize(text)
        total = len(tokens)

        if total <= chunk_size:
            return [text]

        chunks: list[str] = []
        start = 0
        while start < total:
            end = min(start + chunk_size, total)
            chunk_tokens = tokens[start:end]
            chunks.append(self._detokenize(chunk_tokens))
            if end >= total:
                break
            start = end - overlap

        return chunks

    # ── Tokenization ─────────────────────────────────────────

    _TOKEN_PATTERN = re.compile(
        r"\w+(?:[-']\w+)*"         # words with hyphens/apostrophes
        r"|[一-鿿]"           # CJK characters
        r"|[^\w\s]"                 # standalone punctuation
        r"|\n+",                   # newlines
        re.UNICODE,
    )

    def _tokenize(self, text: str) -> list[str]:
        """Split text into approximate tokens for chunk-size estimation.

        Whitespace is not tokenised separately; it is re-joined between
        tokens when chunks are reassembled so the original spacing is
        preserved.
        """
        return self._TOKEN_PATTERN.findall(text)

    def _detokenize(self, tokens: list[str]) -> str:
        """Join tokens back into text, approximating original whitespace."""
        result: list[str] = []
        for t in tokens:
            if result and not t.startswith(("\n", ",", ".", "!", "?", ":", ";", ")", "]", "}")):
                result.append(" ")
            result.append(t)
        return "".join(result)

    def estimate_tokens(self, text: str) -> int:
        """Return approximate token count for *text*."""
        return len(self._tokenize(text))
