"""Trace system — OpenTelemetry-based tracing with Jaeger OTLP export.

Exports spans to Jaeger via OTLP gRPC (http://localhost:4317) and to
trace-service via Kafka. Falls back gracefully when external services
are unavailable (traces are logged locally).
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


class JaegerSpanExporter:
    """Exports spans to Jaeger via OTLP gRPC.

    Configured via environment variables:
      JAEGER_ENDPOINT     — OTLP gRPC endpoint (default: http://localhost:4317)
      JAEGER_SERVICE_NAME — service name in Jaeger UI (default: agent-python-runtime)
      JAEGER_INSECURE     — use plaintext connection (default: true)
    """

    def __init__(self):
        self._provider = None
        self._tracer = None
        self._jaeger_available = False
        self._endpoint = os.getenv("JAEGER_ENDPOINT", os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"))
        self._service_name = os.getenv("JAEGER_SERVICE_NAME", "agent-python-runtime")
        self._insecure = os.getenv("JAEGER_INSECURE", "true").lower() in ("true", "1", "yes")
        self._init_otlp()

    def _init_otlp(self):
        try:
            from opentelemetry import trace as otel_trace
            from opentelemetry.sdk.trace import TracerProvider
            from opentelemetry.sdk.trace.export import BatchSpanProcessor
            from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
            from opentelemetry.sdk.resources import Resource, SERVICE_NAME

            resource = Resource.create({SERVICE_NAME: self._service_name})
            exporter = OTLPSpanExporter(endpoint=self._endpoint, insecure=self._insecure)
            provider = TracerProvider(resource=resource)
            provider.add_span_processor(BatchSpanProcessor(exporter))

            self._provider = provider
            self._tracer = provider.get_tracer(self._service_name)
            self._jaeger_available = True
            logger.info("Jaeger OTLP exporter connected to %s (service=%s, insecure=%s)",
                        self._endpoint, self._service_name, self._insecure)
        except ImportError:
            logger.warning(
                "opentelemetry-exporter-otlp not installed. "
                "Install: pip install opentelemetry-exporter-otlp"
            )
        except Exception as e:
            logger.warning("Jaeger OTLP unavailable at %s: %s", self._endpoint, e)

    def export(self, span_dict: dict) -> None:
        if not self._jaeger_available or not self._tracer:
            return
        try:
            from opentelemetry import trace as otel_trace
            from opentelemetry.trace import Status, StatusCode

            with self._tracer.start_as_current_span(
                span_dict["name"],
                kind=otel_trace.SpanKind.INTERNAL,
            ) as otel_span:
                otel_span.set_attribute("span.type", span_dict.get("type", "agent"))
                otel_span.set_attribute("latency_ms", span_dict.get("latency_ms", 0))
                for key, value in span_dict.get("attributes", {}).items():
                    otel_span.set_attribute(key, value)
                status = span_dict.get("status", "ok")
                if status == "error":
                    otel_span.set_status(Status(StatusCode.ERROR))
        except Exception as e:
            logger.debug("Failed to export span to Jaeger: %s", e)


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
    """Tracer that exports spans to Jaeger + Kafka, with local log fallback."""

    def __init__(self):
        self._spans: list[Span] = []
        self._jaeger = JaegerSpanExporter()
        self._kafka = KafkaSpanExporter()

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
            span_data = span.to_dict()
            self._jaeger.export(span_data)
            self._kafka.export(span_data)

    @property
    def current_span_id(self) -> str | None:
        if self._spans:
            return self._spans[-1].span_id
        return None


tracer = Tracer()


def init_tracer() -> None:
    """Initialize the tracer — connect to Jaeger OTLP collector and Kafka."""
    pass
