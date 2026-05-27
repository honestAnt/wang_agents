"""Shared types for the ingestion pipeline."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class Document:
    """A document loaded from a source, before chunking."""
    content: str
    metadata: dict = field(default_factory=dict)
    doc_id: str = ""

    def __post_init__(self):
        if not self.doc_id:
            import uuid
            self.doc_id = str(uuid.uuid4())


@dataclass
class ChunkConfig:
    """Configuration for a chunking strategy."""
    strategy: str = "fixed_size"  # "fixed_size" | "semantic" | "markdown"
    chunk_size: int = 500
    overlap: int = 50
    similarity_threshold: float = 0.6
    include_headers: bool = True


@dataclass
class IngestResult:
    """Result of an ingestion pipeline run."""
    kb_id: str
    document_count: int = 0
    chunk_count: int = 0
    vector_count: int = 0
    errors: list[str] = field(default_factory=list)
    doc_ids: list[str] = field(default_factory=list)
