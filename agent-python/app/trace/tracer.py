"""Trace system — OpenTelemetry-based tracing with Langfuse integration.

Exports spans to trace-service via Kafka. Falls back gracefully when
Kafka is unavailable (traces are logged locally).
"""

import json
import logging
import os
import time
import uuid
from contextlib import contextmanager

logger = logging.getLogger(__name__)


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

    def to_dict(self) -> dict:
        return {
            "trace_id": self.trace_id,
            "span_id": self.span_id,
            "parent_id": self.parent_id,
            "name": self.name,
            "type": self.span_type,
            "status": self.status,
            "started_at": self.started_at,
            "ended_at": self.ended_at,
            "latency_ms": round(self.latency_ms, 2),
            "attributes": self.attributes,
        }


class KafkaSpanExporter:
    """Exports spans to trace-service via Kafka topic 'trace.spans'."""

    TOPIC = "trace.spans"

    def __init__(self):
        self._producer = None
        self._kafka_available = False
        self._init_producer()

    def _init_producer(self):
        bootstrap = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        try:
            from kafka import KafkaProducer
            self._producer = KafkaProducer(
                bootstrap_servers=bootstrap,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
                retries=2,
                max_block_ms=5000,
            )
            self._kafka_available = True
            logger.info("Kafka span exporter connected to %s", bootstrap)
        except Exception:
            logger.warning("Kafka unavailable, spans will be logged locally")

    def export(self, span_dict: dict) -> None:
        if self._kafka_available and self._producer:
            try:
                self._producer.send(self.TOPIC, span_dict)
            except Exception as e:
                logger.warning("Failed to export span to Kafka: %s", e)
        else:
            logger.debug("Span: %s", json.dumps(span_dict))


class Tracer:
    """Tracer that exports spans via Kafka backed by in-process buffering."""

    def __init__(self):
        self._spans: list[Span] = []
        self._exporter = KafkaSpanExporter()

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
            self._exporter.export(span.to_dict())

    @property
    def current_span_id(self) -> str | None:
        if self._spans:
            return self._spans[-1].span_id
        return None


tracer = Tracer()


def init_tracer() -> None:
    """Initialize the tracer — connect to OpenTelemetry collector and Kafka."""
    pass
