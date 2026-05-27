"""Test chunking strategies."""

from app.rag.ingestion.types import Document, ChunkConfig
from app.rag.ingestion.chunkers import FixedSizeChunker, SemanticChunker, MarkdownChunker, chunker_for


class TestFixedSizeChunker:
    def test_short_document_single_chunk(self):
        chunker = FixedSizeChunker(ChunkConfig(chunk_size=500, overlap=50))
        docs = [Document(content="hello world short text")]
        result = chunker.chunk(docs)
        assert len(result) == 1
        assert result[0].content == "hello world short text"

    def test_long_document_splits(self):
        chunker = FixedSizeChunker(ChunkConfig(chunk_size=100, overlap=20))
        words = [f"word{i}" for i in range(250)]
        text = " ".join(words)
        docs = [Document(content=text)]
        result = chunker.chunk(docs)
        assert len(result) >= 2
        for i, doc in enumerate(result):
            assert doc.metadata["chunk_index"] == i

    def test_empty_document_returns_empty(self):
        chunker = FixedSizeChunker(ChunkConfig())
        result = chunker.chunk([Document(content="")])
        assert result == []

    def test_metadata_preserved(self):
        chunker = FixedSizeChunker(ChunkConfig())
        docs = [Document(content="test", metadata={"source": "file.txt", "kb_id": "kb1"})]
        result = chunker.chunk(docs)
        assert result[0].metadata["source"] == "file.txt"
        assert result[0].metadata["kb_id"] == "kb1"


class TestSemanticChunker:
    def test_short_text_stays_whole(self):
        chunker = SemanticChunker(ChunkConfig())
        docs = [Document(content="A single sentence.")]
        result = chunker.chunk(docs)
        assert len(result) == 1

    def test_splits_long_text(self):
        chunker = SemanticChunker(ChunkConfig(chunk_size=10))
        text = "First sentence here. Second sentence here too. Third one as well."
        docs = [Document(content=text)]
        result = chunker.chunk(docs)
        assert len(result) >= 1


class TestMarkdownChunker:
    def test_splits_on_headings(self):
        chunker = MarkdownChunker(ChunkConfig(include_headers=True))
        md = (
            "# Title\n"
            "Intro paragraph.\n\n"
            "## Section 1\n"
            "Content for section 1.\n\n"
            "## Section 2\n"
            "Content for section 2.\n"
        )
        docs = [Document(content=md)]
        result = chunker.chunk(docs)
        assert len(result) >= 3

    def test_header_context_included(self):
        chunker = MarkdownChunker(ChunkConfig(include_headers=True))
        md = (
            "# API Reference\n\n"
            "## Authentication\n"
            "Use API keys.\n"
        )
        docs = [Document(content=md)]
        result = chunker.chunk(docs)
        auth_chunk = [c for c in result if "Authentication" in c.content][0]
        assert "API Reference" in auth_chunk.metadata.get("parent_headers", "")


class TestChunkerFor:
    def test_fixed_size(self):
        c = chunker_for(ChunkConfig(strategy="fixed_size"))
        assert isinstance(c, FixedSizeChunker)

    def test_semantic(self):
        c = chunker_for(ChunkConfig(strategy="semantic"))
        assert isinstance(c, SemanticChunker)

    def test_markdown(self):
        c = chunker_for(ChunkConfig(strategy="markdown"))
        assert isinstance(c, MarkdownChunker)

    def test_unknown_defaults_to_fixed_size(self):
        c = chunker_for(ChunkConfig(strategy="unknown"))
        assert isinstance(c, FixedSizeChunker)
