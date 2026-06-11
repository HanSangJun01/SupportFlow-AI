import pytest

from app.classifier import classify_with_rules
from app.models import ClassificationRequest, Priority, Sentiment, Urgency


def _request(subject: str, message: str) -> ClassificationRequest:
    return ClassificationRequest(
        tenantId="tenant-1",
        ticketId="ticket-1",
        subject=subject,
        customerMessage=message,
    )


@pytest.mark.parametrize(
    (
        "subject",
        "message",
        "category",
        "urgency",
        "sentiment",
        "priority",
        "confidence",
    ),
    [
        (
            "Invoice charge is wrong",
            "Please refund the duplicate payment on my card.",
            "billing",
            Urgency.NORMAL,
            Sentiment.NEUTRAL,
            Priority.MEDIUM,
            0.74,
        ),
        (
            "Production outage",
            "The app is down and this is unacceptable.",
            "technical",
            Urgency.CRITICAL,
            Sentiment.NEGATIVE,
            Priority.URGENT,
            0.92,
        ),
        (
            "Locked out of account",
            "I cannot login or reset my password.",
            "account",
            Urgency.HIGH,
            Sentiment.NEUTRAL,
            Priority.HIGH,
            0.84,
        ),
        (
            "Cancel subscription",
            "We are ready to terminate this account.",
            "cancellation",
            Urgency.HIGH,
            Sentiment.NEUTRAL,
            Priority.HIGH,
            0.84,
        ),
        (
            "Thanks for the billing help",
            "Great support, I appreciate the invoice explanation.",
            "billing",
            Urgency.NORMAL,
            Sentiment.POSITIVE,
            Priority.MEDIUM,
            0.74,
        ),
        (
            "Question about account access",
            "Can you explain how shared access works?",
            "account",
            Urgency.NORMAL,
            Sentiment.NEUTRAL,
            Priority.MEDIUM,
            0.74,
        ),
        (
            "General question",
            "What are your support hours?",
            "general",
            Urgency.LOW,
            Sentiment.NEUTRAL,
            Priority.LOW,
            0.41,
        ),
        (
            "Urgent help needed",
            "I am blocked and frustrated.",
            "general",
            Urgency.HIGH,
            Sentiment.NEGATIVE,
            Priority.HIGH,
            0.58,
        ),
    ],
)
def test_classifier_maps_reference_examples(
    subject: str,
    message: str,
    category: str,
    urgency: Urgency,
    sentiment: Sentiment,
    priority: Priority,
    confidence: float,
) -> None:
    response = classify_with_rules(_request(subject, message))

    assert response.category == category
    assert response.urgency == urgency
    assert response.sentiment == sentiment
    assert response.priority == priority
    assert response.confidence == confidence
    assert response.classifierVersion == "rules-v1"


def test_classifier_is_deterministic_for_same_request() -> None:
    request = _request(
        subject="Payment bug",
        message="The invoice payment page has an error.",
    )

    assert classify_with_rules(request).model_dump() == classify_with_rules(request).model_dump()
