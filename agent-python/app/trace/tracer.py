"""Trace system — OpenTelemetry-based tracing for agent execution."""

import time
import uuid
from contextlib import contextmanager


class TraceContext:
    """Thread-local trace context (simplified OpenTelemetry API)."""

    _trace_id: str | None = None
    _span_id: str | None = None

    @classmethod
    def generate(cls) -> str:
        return str(uuid.uuid4())

    @classmethod
    def current_trace_id(cls) -> str:
        if cls._trace_id is None:
            cls._trace_id = cls.generate()
        return cls._trace_id

    @classmethod
    def current_span_id(cls) -> str:
        if cls._span_id is None:
            cls._span_id = cls.generate()
        return cls._span_id


class Span:
    """A single span in the trace tree."""

    def __init__(self, name: str, span_type: str, trace_id: str | None = None, parent_id: str | None = None):
        self.name = name
        self.span_type = span_type
        self.trace_id = trace_id or TraceContext.current_trace_id()
        self.span_id = TraceContext.generate()
        self.parent_id = parent_id or TraceContext.current_span_id()
        self.started_at = time.time()
        self.ended_at: float | None = None
        self.attributes: dict[str, str] = {}
        self.status = "ok"

    def set_attribute(self, key: str, value: str) -> None:
        self.attributes[key] = value

    def end(self, error: str | None = None) -> None:
        self.ended_at = time.time()
        if error:
            self.status = "error"

    @property
    def latency_ms(self) -> float:
        if self.ended_at:
            return (self.ended_at - self.started_at) * 1000
        return 0


class Tracer:
    """Simple tracer that mirrors OpenTelemetry API.

    In production, replace with real OpenTelemetry + Langfuse integration.
    """

    def __init__(self):
        self._spans: list[Span] = []

    @contextmanager
    def start_span(self, name: str, span_type: str = "agent", trace_id: str | None = None, parent_id: str | None = None):
        span = Span(name, span_type, trace_id, parent_id)
        self._spans.append(span)
        try:
            yield span
        except Exception as e:
            span.end(str(e))
            raise
        finally:
            span.end()
            self._export(span)

    @property
    def current_span_id(self) -> str | None:
        if self._spans:
            return self._spans[-1].span_id
        return None

    def _export(self, span: Span) -> None:
        """Export span to trace-service via Kafka or HTTP.

        In production: send to Kafka topic 'trace.spans' or call trace-service API.
        """
        pass


tracer = Tracer()


def init_tracer() -> None:
    """Initialize the tracer — connect to OpenTelemetry collector."""
    pass
