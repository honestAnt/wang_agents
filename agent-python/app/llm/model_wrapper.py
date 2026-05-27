"""Model wrapper — unified LLM interface via AgentScope ModelWrapper pattern.

Supports: OpenAI, Anthropic, Google, DeepSeek, Qwen, vLLM, Ollama.
Each provider is accessed through its native REST API using httpx,
following AgentScope's ModelWrapper provider-registry architecture.
"""

from __future__ import annotations

import json
import os
from collections.abc import AsyncIterator
from dataclasses import dataclass
from typing import Any

import httpx


@dataclass
class ProviderConfig:
    """Configuration for a model provider."""

    provider: str  # "openai" | "anthropic" | "google" | "openai_compatible"
    api_key_env: str
    base_url_env: str | None = None
    default_base_url: str = ""
    api_version: str | None = None  # only for Azure / some providers


class ModelWrapper:
    """Unified LLM client using AgentScope's provider-registry pattern.

    Maps model name strings (from ModelRouter) to provider API calls
    with a consistent chat/embed/cost interface — no external gateway needed.
    """

    # ── Provider Registry ────────────────────────────────────

    PROVIDER_REGISTRY: dict[str, ProviderConfig] = {
        # OpenAI
        "gpt-4.1": ProviderConfig(
            provider="openai", api_key_env="OPENAI_API_KEY",
            default_base_url="https://api.openai.com/v1",
        ),
        "gpt-4.1-mini": ProviderConfig(
            provider="openai", api_key_env="OPENAI_API_KEY",
            default_base_url="https://api.openai.com/v1",
        ),
        "gpt-4.1-nano": ProviderConfig(
            provider="openai", api_key_env="OPENAI_API_KEY",
            default_base_url="https://api.openai.com/v1",
        ),
        # Anthropic
        "claude-sonnet-4-6": ProviderConfig(
            provider="anthropic", api_key_env="ANTHROPIC_API_KEY",
            default_base_url="https://api.anthropic.com/v1",
        ),
        "claude-haiku-4-5-20251001": ProviderConfig(
            provider="anthropic", api_key_env="ANTHROPIC_API_KEY",
            default_base_url="https://api.anthropic.com/v1",
        ),
        # Google
        "gemini-2.0-flash": ProviderConfig(
            provider="google", api_key_env="GOOGLE_API_KEY",
            default_base_url="https://generativelanguage.googleapis.com/v1beta",
        ),
        # DeepSeek (OpenAI-compatible)
        "deepseek-v3": ProviderConfig(
            provider="openai_compatible", api_key_env="DEEPSEEK_API_KEY",
            base_url_env="DEEPSEEK_BASE_URL",
            default_base_url="https://api.deepseek.com/v1",
        ),
        # Qwen (OpenAI-compatible)
        "qwen-max": ProviderConfig(
            provider="openai_compatible", api_key_env="QWEN_API_KEY",
            base_url_env="QWEN_BASE_URL",
            default_base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        ),
        "qwen-turbo": ProviderConfig(
            provider="openai_compatible", api_key_env="QWEN_API_KEY",
            base_url_env="QWEN_BASE_URL",
            default_base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        ),
    }

    # ── Cost Table (USD per 1K tokens) ────────────────────────

    COST_TABLE: dict[str, tuple[float, float]] = {
        "gpt-4.1": (0.003, 0.006),
        "gpt-4.1-mini": (0.00015, 0.0006),
        "gpt-4.1-nano": (0.000075, 0.0003),
        "claude-sonnet-4-6": (0.003, 0.015),
        "claude-haiku-4-5-20251001": (0.0008, 0.004),
        "deepseek-v3": (0.00027, 0.0011),
        "qwen-max": (0.002, 0.006),
        "qwen-turbo": (0.0003, 0.0006),
        "gemini-2.0-flash": (0.0001, 0.0004),
    }

    DEFAULT_COST = (0.001, 0.002)
    DEFAULT_DIMENSIONS = 1536

    def __init__(self):
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=httpx.Timeout(120.0))
        return self._client

    async def close(self):
        if self._client:
            await self._client.aclose()
            self._client = None

    # ── Chat API ─────────────────────────────────────────────

    async def chat(
        self,
        messages: list[dict],
        model: str = "gpt-4.1",
        temperature: float = 0.7,
        max_tokens: int = 4096,
        stream: bool = True,
    ) -> AsyncIterator[str]:
        """Send a chat completion request and stream text deltas.

        Routes to the correct provider API based on model name.
        """
        config = self._get_provider_config(model)
        provider = config.provider

        if provider == "openai" or provider == "openai_compatible":
            async for chunk in self._chat_openai(config, model, messages, temperature, max_tokens, stream):
                yield chunk
        elif provider == "anthropic":
            async for chunk in self._chat_anthropic(config, model, messages, temperature, max_tokens, stream):
                yield chunk
        elif provider == "google":
            async for chunk in self._chat_google(config, model, messages, temperature, max_tokens, stream):
                yield chunk
        else:
            # Fallback: treat as OpenAI-compatible
            async for chunk in self._chat_openai(config, model, messages, temperature, max_tokens, stream):
                yield chunk

    # ── OpenAI / OpenAI-compatible Chat ──────────────────────

    async def _chat_openai(
        self, config: ProviderConfig, model: str,
        messages: list[dict], temperature: float, max_tokens: int, stream: bool,
    ) -> AsyncIterator[str]:
        client = await self._get_client()
        base_url = os.getenv(config.base_url_env or "", "") or config.default_base_url
        api_key = os.getenv(config.api_key_env, "")

        body = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": stream,
        }

        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }

        if stream:
            async with client.stream("POST", f"{base_url}/chat/completions", headers=headers, json=body) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: ") and line != "data: [DONE]":
                        try:
                            chunk = json.loads(line[6:])
                            delta = chunk["choices"][0].get("delta", {}).get("content", "")
                            if delta:
                                yield delta
                        except (json.JSONDecodeError, KeyError, IndexError):
                            continue
        else:
            response = await client.post(f"{base_url}/chat/completions", headers=headers, json=body)
            response.raise_for_status()
            data = response.json()
            yield data["choices"][0]["message"]["content"]

    # ── Anthropic Chat ───────────────────────────────────────

    async def _chat_anthropic(
        self, config: ProviderConfig, model: str,
        messages: list[dict], temperature: float, max_tokens: int, stream: bool,
    ) -> AsyncIterator[str]:
        client = await self._get_client()
        base_url = config.default_base_url
        api_key = os.getenv(config.api_key_env, "")

        # Separate system message from chat messages for Anthropic API
        system_msg = None
        chat_messages = messages
        if messages and messages[0].get("role") == "system":
            system_msg = messages[0]["content"]
            chat_messages = messages[1:]

        body: dict[str, Any] = {
            "model": model,
            "messages": chat_messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": stream,
        }
        if system_msg:
            body["system"] = system_msg

        headers = {
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json",
        }

        if stream:
            async with client.stream("POST", f"{base_url}/messages", headers=headers, json=body) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        try:
                            chunk = json.loads(line[6:])
                            if chunk.get("type") == "content_block_delta":
                                delta = chunk.get("delta", {}).get("text", "")
                                if delta:
                                    yield delta
                        except (json.JSONDecodeError, KeyError):
                            continue
        else:
            response = await client.post(f"{base_url}/messages", headers=headers, json=body)
            response.raise_for_status()
            data = response.json()
            text_blocks = [b["text"] for b in data.get("content", []) if b.get("type") == "text"]
            yield "".join(text_blocks)

    # ── Google Gemini Chat ───────────────────────────────────

    async def _chat_google(
        self, config: ProviderConfig, model: str,
        messages: list[dict], temperature: float, max_tokens: int, stream: bool,
    ) -> AsyncIterator[str]:
        client = await self._get_client()
        base_url = config.default_base_url
        api_key = os.getenv(config.api_key_env, "")

        # Convert OpenAI-style messages to Gemini contents format
        contents = []
        system_instruction = None
        for msg in messages:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            if role == "system":
                system_instruction = content
            elif role == "user":
                contents.append({"role": "user", "parts": [{"text": content}]})
            elif role == "assistant":
                contents.append({"role": "model", "parts": [{"text": content}]})

        body: dict[str, Any] = {
            "contents": contents,
            "generationConfig": {
                "temperature": temperature,
                "maxOutputTokens": max_tokens,
            },
        }
        if system_instruction:
            body["systemInstruction"] = {"parts": [{"text": system_instruction}]}

        url = f"{base_url}/models/{model}:{'streamGenerateContent' if stream else 'generateContent'}?alt=sse&key={api_key}"

        if stream:
            async with client.stream("POST", url, json=body) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        try:
                            chunk = json.loads(line[6:])
                            candidates = chunk.get("candidates", [])
                            for candidate in candidates:
                                parts = candidate.get("content", {}).get("parts", [])
                                for part in parts:
                                    delta = part.get("text", "")
                                    if delta:
                                        yield delta
                        except (json.JSONDecodeError, KeyError):
                            continue
        else:
            url = f"{base_url}/models/{model}:generateContent?key={api_key}"
            response = await client.post(url, json=body)
            response.raise_for_status()
            data = response.json()
            candidates = data.get("candidates", [])
            for candidate in candidates:
                parts = candidate.get("content", {}).get("parts", [])
                for part in parts:
                    yield part.get("text", "")

    # ── Embedding API ────────────────────────────────────────

    async def embed(self, texts: list[str], model: str = "text-embedding-3-small") -> list[list[float]]:
        """Generate embeddings via OpenAI-compatible embedding API.

        Returns zero-vector placeholders when no API key is configured.
        """
        api_key = os.getenv("OPENAI_API_KEY", "")
        if not api_key:
            return [[0.0] * self.DEFAULT_DIMENSIONS for _ in texts]

        client = await self._get_client()
        base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")

        body = {
            "model": model,
            "input": texts,
        }
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }

        response = await client.post(f"{base_url}/embeddings", headers=headers, json=body)
        response.raise_for_status()
        data = response.json()
        return [d["embedding"] for d in data["data"]]

    # ── Cost Estimation ──────────────────────────────────────

    def get_cost_estimate(self, model: str, prompt_tokens: int, completion_tokens: int) -> float:
        """Estimate cost for a model based on token counts (USD)."""
        input_price, output_price = self.COST_TABLE.get(model, self.DEFAULT_COST)
        return (prompt_tokens / 1000) * input_price + (completion_tokens / 1000) * output_price

    # ── Internal ─────────────────────────────────────────────

    def _get_provider_config(self, model_name: str) -> ProviderConfig:
        """Look up provider config; fall back to OpenAI-compatible for unknown models."""
        if model_name in self.PROVIDER_REGISTRY:
            return self.PROVIDER_REGISTRY[model_name]
        return ProviderConfig(
            provider="openai_compatible",
            api_key_env="OPENAI_API_KEY",
            default_base_url="https://api.openai.com/v1",
        )
