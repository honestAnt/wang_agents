"""Test ModelWrapper cost estimation and provider registry."""

import pytest
from app.llm.model_wrapper import ModelWrapper


class TestModelWrapper:

    def test_cost_estimate_gpt4(self):
        wrapper = ModelWrapper()
        cost = wrapper.get_cost_estimate("gpt-4.1", 1000, 500)
        # 1000 * 0.003/1000 + 500 * 0.006/1000 = 0.003 + 0.003 = 0.006
        assert cost == pytest.approx(0.006, rel=0.01)

    def test_cost_estimate_claude(self):
        wrapper = ModelWrapper()
        cost = wrapper.get_cost_estimate("claude-sonnet-4-6", 2000, 1000)
        # 2000 * 0.003/1000 + 1000 * 0.015/1000 = 0.006 + 0.015 = 0.021
        assert cost == pytest.approx(0.021, rel=0.01)

    def test_cost_estimate_deepseek(self):
        wrapper = ModelWrapper()
        cost = wrapper.get_cost_estimate("deepseek-v3", 1000, 1000)
        # 1000 * 0.00027 + 1000 * 0.0011 = 0.00027 + 0.0011 = 0.00137
        assert cost == pytest.approx(0.00137, rel=0.01)

    def test_cost_estimate_unknown_model(self):
        wrapper = ModelWrapper()
        cost = wrapper.get_cost_estimate("unknown-model", 1000, 1000)
        # falls back to default pricing (0.001, 0.002)
        assert cost == pytest.approx(0.003, rel=0.01)

    def test_provider_registry_coverage(self):
        """Verify all model_router models have provider configs."""
        from app.llm.model_router import ModelRouter
        wrapper = ModelWrapper()
        for model_name in ModelRouter.MODELS:
            config = wrapper._get_provider_config(model_name)
            assert config is not None, f"Missing provider config for {model_name}"
            assert config.provider in ("openai", "anthropic", "google", "openai_compatible")

    def test_provider_config_fallback(self):
        """Verify unknown models fall back to OpenAI-compatible."""
        wrapper = ModelWrapper()
        config = wrapper._get_provider_config("some-new-model")
        assert config.provider == "openai_compatible"
        assert config.api_key_env == "OPENAI_API_KEY"
