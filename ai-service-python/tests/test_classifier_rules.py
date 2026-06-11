from app.classifier import classify_with_rules
from app.models import ClassificationRequest


def test_classifier_is_deterministic_for_same_request() -> None:
    request = ClassificationRequest(
        tenantId="tenant-1",
        ticketId="ticket-1",
        subject="Need help",
        customerMessage="Please help with my account.",
    )

    assert classify_with_rules(request) == classify_with_rules(request)
