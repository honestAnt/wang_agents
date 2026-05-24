"""Test trace context and span lifecycle."""

from app.trace.tracer import TraceContext, Span


class TestTraceContext:

    def test_generate_returns_uuid(self):
        trace_id = TraceContext.generate()
        assert len(trace_id) == 36
        assert "-" in trace_id

    def test_current_trace_id_consistent(self):
        # Clear for test isolation
        TraceContext._trace_id = None
        t1 = TraceContext.current_trace_id()
        t2 = TraceContext.current_trace_id()
        assert t1 == t2


class TestSpan:

    def test_creates_with_defaults(self):
        span = Span(name="test", span_type="llm")
        assert span.name == "test"
        assert span.span_type == "llm"
        assert span.status == "ok"
        assert span.started_at > 0

    def test_end_sets_latency(self):
        span = Span(name="test", span_type="tool")
        span.end()
        assert span.ended_at is not None
        assert span.latency_ms >= 0
        assert span.status == "ok"

    def test_end_with_error(self):
        span = Span(name="test", span_type="rag")
        span.end(error="Connection timeout")
        assert span.status == "error"
        assert span.ended_at is not None

    def test_set_attribute(self):
        span = Span(name="test", span_type="llm")
        span.set_attribute("model", "gpt-4.1")
        assert span.attributes["model"] == "gpt-4.1"
