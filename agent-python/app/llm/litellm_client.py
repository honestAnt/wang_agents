"""LLM client — thin compatibility wrapper delegating to AgentScope ModelWrapper."""

from collections.abc import AsyncIterator

from app.llm.model_wrapper import ModelWrapper


class LiteLLMClient:
    """Unified LLM client wrapping AgentScope ModelWrapper for multi-provider access.

    Supports: OpenAI, Anthropic, Google, DeepSeek, Qwen, vLLM, Ollama.
    Maintained for backward compatibility — prefer ModelWrapper directly.
    """

    def __init__(self):
        self._wrapper = ModelWrapper()

    async def chat(
        self,
        messages: list[dict],
        model: str = "gpt-4.1",
        temperature: float = 0.7,
        max_tokens: int = 4096,
        stream: bool = True,
    ) -> AsyncIterator[str]:
        async for chunk in self._wrapper.chat(
            messages=messages, model=model,
            temperature=temperature, max_tokens=max_tokens, stream=stream,
        ):
            yield chunk

    def get_cost_estimate(self, model: str, prompt_tokens: int, completion_tokens: int) -> float:
        return self._wrapper.get_cost_estimate(model, prompt_tokens, completion_tokens)
