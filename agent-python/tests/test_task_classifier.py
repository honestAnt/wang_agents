"""Test task classifier."""

import pytest
from app.llm.task_classifier import TaskClassifier, ClassificationResult


@pytest.fixture
def classifier():
    return TaskClassifier()


class TestTaskClassifier:

    def test_classify_code_task(self, classifier):
        result = classifier.classify("write a Python function to sort a list")
        assert result.task_type == "code"
        assert result.confidence > 0

    def test_classify_code_with_error(self, classifier):
        result = classifier.classify("debug this NullPointerException in Java")
        assert result.task_type == "code"

    def test_classify_analysis_task(self, classifier):
        result = classifier.classify("analyze the sales data and find trends")
        assert result.task_type == "analysis"

    def test_classify_search_task(self, classifier):
        result = classifier.classify("find documents about quarterly reports and compliance policies")
        assert result.task_type == "search"

    def test_classify_chat_task(self, classifier):
        result = classifier.classify("hello, can you help me with something?")
        assert result.task_type == "chat"

    def test_classify_translation_task(self, classifier):
        result = classifier.classify("translate this document to Chinese")
        assert result.task_type == "translation"

    def test_classify_summarization_task(self, classifier):
        result = classifier.classify("summarize the key points from this report")
        assert result.task_type == "summarization"

    def test_classify_creative_task(self, classifier):
        result = classifier.classify("write a blog post about AI trends")
        assert result.task_type == "creative"

    def test_classify_math_task(self, classifier):
        result = classifier.classify("solve this equation: 2x + 5 = 15")
        assert result.task_type == "math"

    def test_classify_data_science_task(self, classifier):
        result = classifier.classify("train a neural network with PyTorch")
        assert result.task_type == "data_science"

    def test_classify_unknown(self, classifier):
        result = classifier.classify("xyzzy plugh")
        assert result.task_type == "unknown"
        assert result.confidence == 0.0

    def test_complexity_high_for_long_prompt(self, classifier):
        result = classifier.classify("This is a complex enterprise production distributed microservice orchestration " * 10)
        assert result.complexity == "high"

    def test_complexity_low_for_simple_prompt(self, classifier):
        result = classifier.classify("quick simple question")
        assert result.complexity == "low"

    def test_estimated_tokens(self, classifier):
        result = classifier.classify("hello " * 100)
        assert result.estimated_tokens > 0

    def test_keywords_matched(self, classifier):
        result = classifier.classify("write a function to debug the api endpoint")
        assert len(result.keywords_matched) > 0
        assert any("function" in kw or "debug" in kw or "api" in kw for kw in result.keywords_matched)

    def test_classification_result_dataclass(self):
        result = ClassificationResult(task_type="code", confidence=0.9, complexity="high", estimated_tokens=500)
        assert result.task_type == "code"
        assert result.confidence == 0.9
        assert result.complexity == "high"
        assert result.estimated_tokens == 500
