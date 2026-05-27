# Work Summary — 2026-05-27 — Document Loaders (Task 3)

## Task
Implement Task 3 of the LlamaIndex+Qdrant KB engine: document loaders for the ingestion pipeline.

## What was done

### Created `agent-python/app/rag/ingestion/loaders.py`

Six loader classes + a factory function:

- **TextLoader** — loads `.txt`, `.csv`, `.json`, `.log`, `.xml`, `.yaml`, `.yml`. CSV files are reformatted into a `key: value` row format for better downstream semantic understanding.
- **PDFLoader** — extracts text via PyPDF2 with page markers `[Page N]`.
- **DocxLoader** — extracts paragraphs via `python-docx`.
- **MarkdownLoader** — parses YAML frontmatter into metadata, strips `---` delimiters, extracts content.
- **HTMLLoader** — strips `<script>`, `<style>`, `<nav>`, `<footer>`, `<header>` tags via BeautifulSoup, extracts clean text. Supports both file path and `bytes` input via `load_bytes()`.
- **S3Loader** — downloads from S3/MinIO, delegates to the format-specific loader, injects `s3_uri` and `s3_bucket` metadata.
- **loader_for()** — factory function that returns the correct loader based on file extension.

### Updated `agent-python/tests/test_ingestion_loaders.py`

Added 8 loader tests following TDD:
- `TestTextLoader::test_loads_txt` / `test_loads_csv`
- `TestMarkdownLoader::test_loads_markdown` (frontmatter parsing)
- `TestHTMLLoader::test_loads_html`
- `TestLoaderFor::test_txt_returns_text_loader` / `test_md_returns_markdown_loader` / `test_html_returns_html_loader` / `test_unknown_returns_text_loader`

### Test results
All 15 tests in the file pass. Full suite: 174 passed, 0 failures.

## Problems encountered

None. The task specification provided clear class designs and test cases, and the existing `types.Document` dataclass from Task 2 was a clean dependency.

## Key decisions
- External library imports (PyPDF2, python-docx, beautifulsoup4, minio) use lazy imports inside the `load()` methods to avoid hard dependencies at module import time.
- CSV files are reformatted from tabular to a `key: value | key: value` per-row format to improve semantic understanding by LLMs during RAG.
