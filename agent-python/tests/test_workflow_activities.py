"""Test workflow activity business logic (pure functions)."""

import pytest

from app.core.workflow.activities import (
    RAGActivityInput,
    ReportActivityInput,
    ToolActivityInput,
    _do_analysis,
    _do_approval_request,
    _do_data_collection,
    _do_llm_call,
    _do_notification,
    _do_rag_retrieval,
    _do_report_generation,
    _do_tool_execution,
)
from app.core.workflow.activities import AgentActivityInput


class TestRAGRetrieval:

    def test_rag_retrieval_basic(self):
        result = _do_rag_retrieval(RAGActivityInput(tenant_id="t1", query="test query"))
        assert "documents" in result
        assert result["query"] == "test query"
        assert len(result["documents"]) <= 5

    def test_rag_retrieval_with_kb_ids(self):
        result = _do_rag_retrieval(RAGActivityInput(
            tenant_id="t1", query="compliance", knowledge_base_ids=["kb-1", "kb-2"], top_k=3
        ))
        assert len(result["documents"]) <= 3
        assert result["total_hits"] == 3

    def test_rag_retrieval_scores_descending(self):
        result = _do_rag_retrieval(RAGActivityInput(tenant_id="t1", query="test", top_k=5))
        scores = [d["score"] for d in result["documents"]]
        assert scores == sorted(scores, reverse=True)


class TestLLMCall:

    def test_llm_call_basic(self):
        result = _do_llm_call(AgentActivityInput(tenant_id="t1", session_id="s1", user_message="hello"))
        assert result["session_id"] == "s1"
        assert "response" in result
        assert "tokens" in result
        assert "cost" in result

    def test_llm_call_with_model(self):
        result = _do_llm_call(AgentActivityInput(
            tenant_id="t1", session_id="s1", user_message="hi", model="claude-3"
        ))
        assert result["model"] == "claude-3"


class TestToolExecution:

    def test_tool_execution_basic(self):
        result = _do_tool_execution(ToolActivityInput(
            tenant_id="t1", tool_name="search_tool", parameters={"q": "test"}
        ))
        assert result["tool_name"] == "search_tool"
        assert result["result"]["status"] == "success"

    def test_tool_execution_with_params(self):
        result = _do_tool_execution(ToolActivityInput(
            tenant_id="t1", tool_name="calculator", parameters={"expr": "2+2"}
        ))
        assert result["tool_name"] == "calculator"


class TestDataCollection:

    def test_data_collection_basic(self):
        result = _do_data_collection(AgentActivityInput(
            tenant_id="t1", session_id="s1", user_message="collect data"
        ))
        assert result["status"] == "completed"
        assert "collected_data" in result

    def test_data_collection_multiple_sources(self):
        result = _do_data_collection(AgentActivityInput(
            tenant_id="t1", session_id="s1", user_message="multi-source"
        ))
        assert len(result["collected_data"]) >= 2
        sources = [d["source"] for d in result["collected_data"]]
        assert "knowledge_base" in sources
        assert "tool_search" in sources


class TestAnalysis:

    def test_analysis_basic(self):
        result = _do_analysis(AgentActivityInput(tenant_id="t1", session_id="s1", user_message="analyze trends"))
        assert result["status"] == "completed"
        assert "analysis" in result
        assert "key_insights" in result["analysis"]
        assert len(result["analysis"]["key_insights"]) >= 1

    def test_analysis_has_confidence(self):
        result = _do_analysis(AgentActivityInput(tenant_id="t1", session_id="s1", user_message="test"))
        assert 0 <= result["analysis"]["confidence"] <= 1


class TestReportGeneration:

    def test_report_generation_basic(self):
        result = _do_report_generation(ReportActivityInput(
            tenant_id="t1", title="Test Report", sections=[{"content": "section 1"}]
        ))
        assert result["status"] == "completed"
        assert "Test Report" in result["content"]

    def test_report_generation_format(self):
        result = _do_report_generation(ReportActivityInput(
            tenant_id="t1", title="PDF Report", format="pdf"
        ))
        assert result["format"] == "pdf"


class TestApprovalRequest:

    def test_approval_request(self):
        result = _do_approval_request({"approval_id": "approval-1"})
        assert result["status"] == "approved"
        assert result["approval_id"] == "approval-1"


class TestNotification:

    def test_notification_slack(self):
        result = _do_notification({"channel": "slack", "recipient": "user-1", "message": "hello"})
        assert result["status"] == "sent"
        assert result["channel"] == "slack"

    def test_notification_email(self):
        result = _do_notification({"channel": "email", "recipient": "a@b.com", "message": "test"})
        assert result["status"] == "sent"
        assert result["channel"] == "email"
