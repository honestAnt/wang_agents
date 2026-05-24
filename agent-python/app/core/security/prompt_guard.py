"""Prompt injection detection — regex + pattern-based guard.

Detects common injection patterns:
- "Ignore previous instructions" / "system prompt override"
- "You are now DAN" / jailbreak attempts
- Prompt leak attempts ("show me your instructions")
- SQL injection in prompts
- Code injection (eval, exec, os.system)
"""

import re
from dataclasses import dataclass, field
from enum import Enum


class Severity(Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


@dataclass
class DetectionResult:
    flagged: bool
    patterns_matched: list[str] = field(default_factory=list)
    severity: Severity = Severity.LOW
    sanitized: str = ""
    risk_score: float = 0.0  # 0.0–1.0


class PromptGuard:
    """Detects prompt injection attempts in user input."""

    # ── Injection Patterns ───────────────────────────────────

    JAILBREAK_PATTERNS = [
        r"ignore (all |your )?(previous|prior|above|original|system) (instructions?|prompts?|rules?)",
        r"(forget|disregard|override) (all |your )?(previous|instructions?|prompts?)",
        r"you are now (dan|jailbroken|unleashed|unrestricted|free)",
        r"(pretend|imagine|act as if) you (are|were) (a |an )?(evil|malicious|unethical|unfiltered|without restrictions)",
        r"(developer mode|god mode|admin mode|root mode)",
        r"(bypass|circumvent|disable) (your )?(safety|content filter|restrictions|guidelines|rules)",
        r"respond (as|like) (a |an )?(unfiltered|uncensored|unrestricted) ",
    ]

    PROMPT_LEAK_PATTERNS = [
        r"(show|reveal|tell me|print|output|display|what is) .{0,5}your (system |base |initial )?(prompt|instructions?|rules?)",
        r"what (are|were) you (programmed|instructed|told) to ",
        r"repeat (the |your )?(system |base )?(prompt|instructions?)",
        r"(send|give) me (a copy of )?your (system |base )?(prompt|instructions?)",
    ]

    CODE_INJECTION_PATTERNS = [
        r"\b(eval|exec|execfile|compile|__import__|importlib)\s*\(",
        r"\b(os\.system|subprocess\.(call|Popen|run)|commands\.getoutput)\b",
        r"\b(__builtins__|__globals__|__locals__|__dict__|__class__|__bases__|__subclasses__)\b",
        r"\b(rm\s+-rf|del\s+/[FS]|format\s+[CFD]:)\b",
    ]

    SQL_INJECTION_PATTERNS = [
        r"(\bSELECT\b.*\bFROM\b|\bINSERT\b.*\bINTO\b|\bUPDATE\b.*\bSET\b|\bDELETE\b.*\bFROM\b|\bDROP\b.*\bTABLE\b)",
        r"(';\s*(DROP|ALTER|CREATE|TRUNCATE)\b)",
        r"(\bUNION\b.*\bSELECT\b)",
        r"(\bOR\b\s+['\"]?\d['\"]?\s*=\s*['\"]?\d['\"]?)",
    ]

    def detect(self, prompt: str, agent_config: dict | None = None) -> DetectionResult:
        """Detect prompt injection attempts. Returns DetectionResult."""
        prompt_lower = prompt.lower()
        matched: list[str] = []
        risk = 0.0

        # Check jailbreak
        for pattern in self.JAILBREAK_PATTERNS:
            if re.search(pattern, prompt_lower, re.IGNORECASE):
                matched.append("jailbreak")
                risk += 0.35
                break

        # Check prompt leak
        for pattern in self.PROMPT_LEAK_PATTERNS:
            if re.search(pattern, prompt_lower, re.IGNORECASE):
                matched.append("prompt_leak")
                risk += 0.3
                break

        # Check code injection
        for pattern in self.CODE_INJECTION_PATTERNS:
            if re.search(pattern, prompt_lower, re.IGNORECASE):
                matched.append("code_injection")
                risk += 0.25
                break

        # Check SQL injection
        for pattern in self.SQL_INJECTION_PATTERNS:
            if re.search(pattern, prompt, re.IGNORECASE):
                matched.append("sql_injection")
                risk += 0.2
                break

        risk = min(risk, 1.0)

        if risk >= 0.7:
            severity = Severity.CRITICAL
        elif risk >= 0.5:
            severity = Severity.HIGH
        elif risk >= 0.3:
            severity = Severity.MEDIUM
        else:
            severity = Severity.LOW

        return DetectionResult(
            flagged=risk > 0,
            patterns_matched=matched,
            severity=severity,
            sanitized=prompt if risk < 0.5 else "[POTENTIALLY MALICIOUS PROMPT DETECTED]",
            risk_score=round(risk, 2),
        )


class DataMasker:
    """Detects and masks PII/sensitive data in text."""

    # Order matters: more specific patterns first to avoid partial matches
    PATTERNS = [
        ("id_card_cn", r"\b\d{17}[\dXx]\b"),
        ("credit_card", r"\b\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}\b"),
        ("email", r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b"),
        ("bank_card", r"\b\d{16,19}\b"),
        ("phone_cn", r"1[3-9]\d{9}"),
        ("phone_us", r"\b\d{3}[-.]?\d{3}[-.]?\d{4}\b"),
        ("ip_address", r"\b(?:\d{1,3}\.){3}\d{1,3}\b"),
    ]

    def mask(self, text: str) -> dict:
        """Mask all PII patterns. Returns masked text and found items."""
        found: list[str] = []
        masked = text

        for pii_type, pattern in self.PATTERNS:
            matches = re.findall(pattern, masked)
            if matches:
                found.append(pii_type)
                for match in matches[:10]:
                    masked = masked.replace(str(match), f"[MASKED_{pii_type.upper()}]")

        return {"masked_text": masked, "pii_types_found": list(set(found))}

    def mask_prompt(self, prompt: str) -> str:
        """Mask a user prompt before sending to LLM."""
        result = self.mask(prompt)
        return result["masked_text"]
