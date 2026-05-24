"""LiteLLM client — unified interface for all LLM providers."""

from collections.abc import AsyncIterator


class LiteLLMClient:
    """Unified LLM client wrapping LiteLLM for multi-provider access.

    Supports: OpenAI, Anthropic, Google, DeepSeek, Qwen, vLLM, Ollama.
    """

    async def chat(
        self,
        messages: list[dict],
        model: str = "gpt-4.1",
        temperature: float = 0.7,
        max_tokens: int = 4096,
        stream: bool = True,
    ) -> AsyncIterator[str]:
        """Send a chat completion request and stream the response.

        In production, this calls liteLLM:
            import litellm
            response = await litellm.acompletion(
                model=model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
                stream=stream,
            )
            async for chunk in response:
                yield chunk.choices[0].delta.content or ""
        """
        yield f"[LiteLLM: {model}] response placeholder"

    def get_cost_estimate(self, model: str, prompt_tokens: int, completion_tokens: int) -> float:
        """Estimate cost for a model based on token counts."""
        pricing = {
            "gpt-4.1": (0.03, 0.06),
            "gpt-4.1-mini": (0.00015, 0.0006),
            "claude-sonnet-4-6": (0.003, 0.015),
            "claude-haiku-4-5-20251001": (0.0008, 0.004),
            "deepseek-v3": (0.00027, 0.0011),
            "qwen-max": (0.002, 0.006),
        }
        input_price, output_price = pricing.get(model, (0.001, 0.002))
        return (prompt_tokens / 1000) * input_price + (completion_tokens / 1000) * output_price
