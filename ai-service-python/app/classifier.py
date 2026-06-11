from app.models import (
    ClassificationRequest,
    ClassificationResponse,
    Priority,
    Sentiment,
    Urgency,
)

CLASSIFIER_VERSION = "rules-v1"


def classify_with_rules(request: ClassificationRequest) -> ClassificationResponse:
    return ClassificationResponse(
        category="general",
        urgency=Urgency.LOW,
        sentiment=Sentiment.NEUTRAL,
        priority=Priority.LOW,
        confidence=0.41,
        classifierVersion=CLASSIFIER_VERSION,
    )
