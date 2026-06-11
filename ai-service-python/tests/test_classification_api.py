from fastapi.testclient import TestClient

from app.main import app
from app.settings import SERVICE_NAME, SERVICE_VERSION, TICKET_CLASSIFICATION_PATH

client = TestClient(app)

PROVIDER_ENV_KEYS = (
    "OPENAI_API_KEY",
    "ANTHROPIC_API_KEY",
    "PHOENIX_API_KEY",
    "PHOENIX_COLLECTOR_ENDPOINT",
)


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
    response = client.post(TICKET_CLASSIFICATION_PATH, json=_request_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["category"] == "account"
    assert body["urgency"] == "NORMAL"
    assert body["sentiment"] == "NEUTRAL"
    assert body["priority"] == "MEDIUM"
    assert 0.0 <= body["confidence"] <= 1.0
    assert body["classifierVersion"] == "rules-v1"
    assert "rationale" not in body


def test_classification_rejects_extra_request_fields() -> None:
    payload = _request_payload() | {"rationale": "please explain"}

    response = client.post(TICKET_CLASSIFICATION_PATH, json=payload)

    assert response.status_code == 422


def test_classification_rejects_blank_content() -> None:
    payload = _request_payload() | {"subject": "   "}

    response = client.post(TICKET_CLASSIFICATION_PATH, json=payload)

    assert response.status_code == 422


def test_classification_rejects_blank_message() -> None:
    payload = _request_payload() | {"customerMessage": "   "}

    response = client.post(TICKET_CLASSIFICATION_PATH, json=payload)

    assert response.status_code == 422


def test_classification_rejects_oversized_subject() -> None:
    payload = _request_payload() | {"subject": "x" * 201}

    response = client.post(TICKET_CLASSIFICATION_PATH, json=payload)

    assert response.status_code == 422


def test_classification_rejects_oversized_content() -> None:
    payload = _request_payload() | {"customerMessage": "x" * 5001}

    response = client.post(TICKET_CLASSIFICATION_PATH, json=payload)

    assert response.status_code == 422


def test_openapi_exposes_stable_classification_contract() -> None:
    response = client.get("/openapi.json")

    assert response.status_code == 200
    schema = response.json()
    assert schema["info"]["title"] == SERVICE_NAME
    assert schema["info"]["version"] == SERVICE_VERSION
    assert TICKET_CLASSIFICATION_PATH in schema["paths"]

    operation = schema["paths"][TICKET_CLASSIFICATION_PATH]["post"]
    assert operation["responses"]["200"]["content"]["application/json"]["schema"] == {
        "$ref": "#/components/schemas/ClassificationResponse"
    }

    schemas = schema["components"]["schemas"]
    assert "ClassificationRequest" in schemas
    assert "ClassificationResponse" in schemas
    assert "Urgency" in schemas
    assert "Sentiment" in schemas
    assert "Priority" in schemas
    confidence = schemas["ClassificationResponse"]["properties"]["confidence"]
    assert confidence["minimum"] == 0.0
    assert confidence["maximum"] == 1.0


def test_provider_environment_variables_are_not_required(monkeypatch) -> None:
    for key in PROVIDER_ENV_KEYS:
        monkeypatch.delenv(key, raising=False)

    response = client.post(TICKET_CLASSIFICATION_PATH, json=_request_payload())

    assert response.status_code == 200
