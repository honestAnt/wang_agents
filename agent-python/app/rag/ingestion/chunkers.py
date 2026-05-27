"""Chunking strategies — split documents into indexable chunks."""

from __future__ import annotations

import re

from app.rag.ingestion.types import Document, ChunkConfig


class FixedSizeChunker:
    """Split documents into fixed-token-size chunks with overlap."""

    _TOKEN_PATTERN = re.compile(
        r"\w+(?:[-']\w+)*|[一-鿿]|[^\w\s]|\n+", re.UNICODE
    )

    def __init__(self, config: ChunkConfig):
        self._chunk_size = config.chunk_size
        self._overlap = config.overlap

    def chunk(self, documents: list[Document]) -> list[Document]:
        result: list[Document] = []
        for doc in documents:
            if not doc.content or not doc.content.strip():
                continue
            tokens = self._tokenize(doc.content)
            if len(tokens) <= self._chunk_size:
                doc.metadata["chunk_index"] = 0
                result.append(doc)
                continue

            idx = 0
            start = 0
            while start < len(tokens):
                end = min(start + self._chunk_size, len(tokens))
                chunk_tokens = tokens[start:end]
                chunk_text = self._detokenize(chunk_tokens)

                meta = {**doc.metadata, "chunk_index": idx, "chunk_total_tokens": len(chunk_tokens)}
                chunk_doc = Document(
                    content=chunk_text, metadata=meta,
                    doc_id=f"{doc.doc_id}_chunk_{idx}"
                )
                result.append(chunk_doc)

                if end >= len(tokens):
                    break
                start = end - self._overlap
                idx += 1

        return result

    def _tokenize(self, text: str) -> list[str]:
        return self._TOKEN_PATTERN.findall(text)

    def _detokenize(self, tokens: list[str]) -> str:
        parts: list[str] = []
        for t in tokens:
            if parts and not t.startswith(("\n", ",", ".", "!", "?", ":", ";", ")", "]", "}")):
                parts.append(" ")
            parts.append(t)
        return "".join(parts)


class SemanticChunker:
    """Split documents at sentence boundaries based on chunk size."""

    _SENTENCE_PATTERN = re.compile(r"(?<=[.!?。！？\n])\s+")

    def __init__(self, config: ChunkConfig):
        self._chunk_size = config.chunk_size

    def chunk(self, documents: list[Document]) -> list[Document]:
        result: list[Document] = []
        for doc in documents:
            if not doc.content or not doc.content.strip():
                continue
            sentences = self._SENTENCE_PATTERN.split(doc.content)
            sentences = [s.strip() for s in sentences if s.strip()]

            current_chunk: list[str] = []
            current_len = 0
            chunk_idx = 0

            for sentence in sentences:
                sent_len = len(sentence.split())
                if current_len + sent_len > self._chunk_size and current_chunk:
                    chunk_text = " ".join(current_chunk)
                    meta = {**doc.metadata, "chunk_index": chunk_idx}
                    result.append(Document(
                        content=chunk_text, metadata=meta,
                        doc_id=f"{doc.doc_id}_chunk_{chunk_idx}"
                    ))
                    current_chunk = []
                    current_len = 0
                    chunk_idx += 1

                current_chunk.append(sentence)
                current_len += sent_len

            if current_chunk:
                chunk_text = " ".join(current_chunk)
                meta = {**doc.metadata, "chunk_index": chunk_idx}
                result.append(Document(
                    content=chunk_text, metadata=meta,
                    doc_id=f"{doc.doc_id}_chunk_{chunk_idx}"
                ))

        return result


class MarkdownChunker:
    """Split Markdown documents at heading boundaries, preserving header hierarchy."""

    _HEADING_PATTERN = re.compile(r"^(#{1,6})\s+(.+)$", re.MULTILINE)

    def __init__(self, config: ChunkConfig):
        self._include_headers = config.include_headers
        self._chunk_size = config.chunk_size

    def chunk(self, documents: list[Document]) -> list[Document]:
        result: list[Document] = []
        for doc in documents:
            if not doc.content or not doc.content.strip():
                continue
            sections = self._split_on_headings(doc.content)
            parent_headers: list[str] = []

            for idx, (level, title, body) in enumerate(sections):
                if not body.strip():
                    while parent_headers and len(parent_headers) >= level:
                        parent_headers.pop()
                    parent_headers.append(title)
                    continue

                meta = {
                    **doc.metadata,
                    "chunk_index": idx,
                    "heading": title,
                    "heading_level": level,
                }
                if self._include_headers and parent_headers:
                    meta["parent_headers"] = " > ".join(parent_headers)

                # Prepend heading hierarchy to body for context
                prefix_parts = parent_headers + [title] if self._include_headers else [title]
                chunk_text = f"{' > '.join(prefix_parts)}\n\n{body}"
                if len(chunk_text.split()) > self._chunk_size:
                    sub = FixedSizeChunker(ChunkConfig(chunk_size=self._chunk_size, overlap=50))
                    for sd in sub.chunk([Document(content=chunk_text, metadata=meta, doc_id=doc.doc_id)]):
                        sd.metadata.update(meta)
                        result.append(sd)
                else:
                    result.append(Document(
                        content=chunk_text, metadata=meta,
                        doc_id=f"{doc.doc_id}_chunk_{idx}"
                    ))

                while parent_headers and len(parent_headers) >= level:
                    parent_headers.pop()
                parent_headers.append(title)

        return result

    def _split_on_headings(self, text: str) -> list[tuple[int, str, str]]:
        matches = list(self._HEADING_PATTERN.finditer(text))
        if not matches:
            return [(1, "", text)]

        sections: list[tuple[int, str, str]] = []
        for i, match in enumerate(matches):
            level = len(match.group(1))
            title = match.group(2).strip()
            start = match.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
            body = text[start:end].strip()
            sections.append((level, title, body))

        return sections


def chunker_for(config: ChunkConfig) -> FixedSizeChunker | SemanticChunker | MarkdownChunker:
    """Return the appropriate chunker for a given configuration."""
    if config.strategy == "semantic":
        return SemanticChunker(config)
    if config.strategy == "markdown":
        return MarkdownChunker(config)
    return FixedSizeChunker(config)
