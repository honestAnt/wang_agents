"""Basic smoke test for the agent runtime."""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"


def test_openapi_schema():
    """Verify OpenAPI schema is generated."""
    schema = app.openapi()
    assert "paths" in schema
    assert "/api/v1/chat" in schema["paths"]
