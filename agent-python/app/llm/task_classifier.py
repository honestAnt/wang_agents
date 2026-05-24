"""Task classifier — recognizes task type from user prompt for intelligent routing.

Classifies prompts into: code, analysis, chat, search, translation, summarization,
creative, math, data_science, and unknown.
"""

import re
from dataclasses import dataclass, field


@dataclass
class ClassificationResult:
    task_type: str
    confidence: float  # 0.0–1.0
    complexity: str = "low"  # low, medium, high
    estimated_tokens: int = 0
    keywords_matched: list[str] = field(default_factory=list)


class TaskClassifier:
    """Heuristic + keyword-based task classifier. In production, use an LLM classifier."""

    TASK_PATTERNS: dict[str, list[str]] = {
        "code": [
            r"\b(code|program|function|class|debug|bug|compile|compile error|error|exception|api|endpoint|rest|graphql|sql|query|database|schema|migration|refactor|deploy|ci/cd|pipeline|docker|kubernetes|regex|pattern)\b",
            r"\b(write|fix|implement|create|build)\b.*\b(app|application|script|module|package|library|api|server|service|component)\b",
            r"```|import |def |class |const |let |var |function |async |await ",
        ],
        "analysis": [
            r"\b(analyze|analysis|analytics|insight|trend|pattern|correlat|statistic|metrics|kpi|dashboard|report|forecast|predict|compare|benchmark)\b",
            r"\b(data|dataset|csv|excel|spreadsheet|table)\b.*\b(analyze|analyse|explore|examine|investigate)\b",
        ],
        "chat": [
            r"\b(hello|hi|hey|help|thanks|thank you|please|what|how|who|when|where|why|can you|could you|would you|tell me|explain|describe)\b",
        ],
        "search": [
            r"\b(find|search|lookup|retrieve|locate|query|fetch|get)\b.*\b(document|documents|file|files|record|records|information|data|knowledge|article|articles|report|reports|policy|policies)\b",
            r"\b(look up|look for)\b",
        ],
        "translation": [
            r"\b(translate|translation|convert to|in english|in chinese|in french|in german|in japanese|in spanish|to english|to chinese)\b",
        ],
        "summarization": [
            r"\b(summarize|summary|summarization|tldr|key points|bullet points|condense|recap|overview|digest)\b",
        ],
        "creative": [
            r"\b(write|compose|generate|create|draft|design)\b.*\b(story|poem|article|blog|essay|letter|email|message|content|copy|slogan|tagline|headline)\b",
        ],
        "math": [
            r"\b(calculate|solve|equation|math|mathematics|formula|computation|algebra|calculus|geometry|probability|statistic)\b",
        ],
        "data_science": [
            r"\b(machine learning|deep learning|neural network|training|model|dataset|feature|accuracy|precision|recall|classification|regression|clustering|nlp|computer vision)\b",
            r"\b(pandas|numpy|scikit-learn|tensorflow|pytorch|keras|xgboost|lightgbm)\b",
        ],
    }

    # Complexity indicators
    COMPLEXITY_PATTERNS = {
        "high": [
            r"\b(multi-step|complex|sophisticated|advanced|enterprise|production|scalable|distributed|microservice|orchestrat|workflow|pipeline)\b",
            r"```[\s\S]*```",  # code blocks
            r".{500,}",  # long prompts
        ],
        "low": [
            r"\b(simple|quick|easy|basic|straightforward|brief|short)\b",
        ],
    }

    def classify(self, prompt: str) -> ClassificationResult:
        """Classify a user prompt into a task type with confidence."""
        prompt_lower = prompt.lower()
        scores: dict[str, float] = {}
        all_keywords: list[str] = []

        for task_type, patterns in self.TASK_PATTERNS.items():
            score = 0.0
            matched = []
            for pattern in patterns:
                matches = re.findall(pattern, prompt_lower, re.IGNORECASE)
                if matches:
                    # More matches = higher confidence
                    score += min(len(matches), 3) * 0.33
                    matched.extend(matches[:3])
            if score > 0:
                scores[task_type] = min(score, 1.0)
                all_keywords.extend(matched)

        if not scores:
            return ClassificationResult(
                task_type="unknown",
                confidence=0.0,
                complexity=self._classify_complexity(prompt),
                estimated_tokens=self._estimate_tokens(prompt),
            )

        # Pick highest confidence type
        best_type = max(scores, key=scores.get)
        return ClassificationResult(
            task_type=best_type,
            confidence=round(min(scores[best_type], 1.0), 2),
            complexity=self._classify_complexity(prompt),
            estimated_tokens=self._estimate_tokens(prompt),
            keywords_matched=list(set(all_keywords))[:10],
        )

    def _classify_complexity(self, prompt: str) -> str:
        """Estimate task complexity: low, medium, or high."""
        prompt_lower = prompt.lower()
        prompt_len = len(prompt)

        for pattern in self.COMPLEXITY_PATTERNS["high"]:
            if re.search(pattern, prompt_lower, re.IGNORECASE | re.DOTALL):
                return "high"

        # Long prompts are at least medium
        if prompt_len > 300:
            return "medium"

        for pattern in self.COMPLEXITY_PATTERNS["low"]:
            if re.search(pattern, prompt_lower, re.IGNORECASE):
                return "low"

        return "medium"

    def _estimate_tokens(self, prompt: str) -> int:
        """Rough token count estimate (~4 chars per token for English)."""
        return max(1, len(prompt) // 4)
