"""Test LiteLLM client pricing and model routing."""

import pytest
from app.llm.litellm_client import LiteLLMClient


class TestLiteLLMClient:

    def test_cost_estimate_gpt4(self):
        client = LiteLLMClient()
        cost = client.get_cost_estimate("gpt-4.1", 1000, 500)
        # 1000 input tokens * 0.03/1000 + 500 output * 0.06/1000
        assert cost == pytest.approx(0.06, rel=0.1)

    def test_cost_estimate_claude(self):
        client = LiteLLMClient()
        cost = client.get_cost_estimate("claude-sonnet-4-6", 2000, 1000)
        # 2000 * 0.003/1000 + 1000 * 0.015/1000 = 0.006 + 0.015
        assert cost == pytest.approx(0.021, rel=0.1)

    def test_cost_estimate_deepseek(self):
        client = LiteLLMClient()
        cost = client.get_cost_estimate("deepseek-v3", 1000, 1000)
        # 1000 * 0.00027 + 1000 * 0.0011 = 0.00027 + 0.0011
        assert cost == pytest.approx(0.00137, rel=0.1)

    def test_cost_estimate_unknown_model(self):
        client = LiteLLMClient()
        cost = client.get_cost_estimate("unknown-model", 1000, 1000)
        # falls back to default pricing (0.001, 0.002)
        assert cost == pytest.approx(0.003, rel=0.1)
