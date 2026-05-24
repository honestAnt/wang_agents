"""Test workflow API endpoints (integration tests with FastAPI TestClient)."""

import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    """Create test client — import here to avoid temporalio import at test collection if not needed."""
    from app.main import app
    return TestClient(app)


class TestHealthEndpoint:

    def test_health_check(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"


class TestWorkflowAPIStructure:

    def test_workflow_routes_registered(self, client):
        """Verify the workflow routes are registered (will 422 for missing body, not 404)."""
        response = client.post("/api/workflows", json={})
        # 422 = validation error (body accepted but invalid), 404 = route not found
        assert response.status_code != 404

    def test_list_workflows_requires_no_body(self, client):
        """GET /api/workflows should accept (though it needs Temporal, may error differently)."""
        response = client.get("/api/workflows")
        # May fail at Temporal connection, but route should exist
        assert response.status_code != 404

    def test_get_workflow_route_exists(self, client):
        response = client.get("/api/workflows/nonexistent")
        assert response.status_code != 404

    def test_cancel_workflow_route_exists(self, client):
        response = client.delete("/api/workflows/nonexistent")
        assert response.status_code != 404

    def test_retry_workflow_route_exists(self, client):
        response = client.post("/api/workflows/nonexistent/retry")
        assert response.status_code != 404

    def test_signal_workflow_route_exists(self, client):
        response = client.post("/api/workflows/nonexistent/signal?signal_name=approve")
        assert response.status_code != 404
