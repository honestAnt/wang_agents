"""Embedding client — generates embeddings via AgentScope ModelWrapper."""

from app.llm.model_wrapper import ModelWrapper


class EmbeddingClient:
    """Generates vector embeddings for text input."""

    DEFAULT_MODEL = "text-embedding-3-small"
    DEFAULT_DIMENSIONS = 1536

    def __init__(self):
        self._wrapper = ModelWrapper()

    async def embed(self, texts: list[str], model: str = DEFAULT_MODEL) -> list[list[float]]:
        """Generate embeddings for a batch of texts via AgentScope ModelWrapper."""
        return await self._wrapper.embed(texts, model)
