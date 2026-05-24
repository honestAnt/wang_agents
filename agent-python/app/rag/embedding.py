"""Embedding client — generates embeddings via LiteLLM or direct API."""


class EmbeddingClient:
    """Generates vector embeddings for text input."""

    DEFAULT_MODEL = "text-embedding-3-small"
    DEFAULT_DIMENSIONS = 1536

    async def embed(self, texts: list[str], model: str = DEFAULT_MODEL) -> list[list[float]]:
        """Generate embeddings for a batch of texts.

        In production, calls LiteLLM embedding API or directly calls
        OpenAI / Cohere / local embedding service.
        """
        # Placeholder — production implementation:
        # import litellm
        # response = await litellm.aembedding(model=model, input=texts)
        # return [d["embedding"] for d in response.data]
        return [[0.0] * self.DEFAULT_DIMENSIONS for _ in texts]
