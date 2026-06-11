from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _request_payload() -> dict[str, str]:
    return {
        "tenantId": "tenant-1",
        "ticketId": "ticket-1",
        "subject": "Need help",
        "customerMessage": "Please help with my account.",
    }


def test_health_returns_ok() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_classification_endpoint_returns_contract_fields() -> None:
    response = client.post("/api/v1/classifications/tickets", json=_request_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["category"] == "general"
    assert body["urgency"] == "LOW"
    assert body["sentiment"] == "NEUTRAL"
    assert body["priority"] == "LOW"
    assert 0.0 <= body["confidence"] <= 1.0
    assert body["classifierVersion"] == "rules-v1"
