import re

from app.models import (
    ClassificationRequest,
    ClassificationResponse,
    Priority,
    Sentiment,
    Urgency,
)

CLASSIFIER_VERSION = "rules-v1"

CATEGORY_KEYWORDS: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("billing", ("bill", "billing", "payment", "invoice", "refund", "charge", "card")),
    ("technical", ("bug", "error", "broken", "crash", "cannot load", "outage", "down")),
    ("account", ("login", "password", "account", "locked", "sign in", "access")),
    ("cancellation", ("cancel", "cancellation", "terminate", "churn")),
)

CRITICAL_KEYWORDS = ("outage", "down", "security", "breach")
HIGH_KEYWORDS = ("urgent", "blocked", "cannot", "failed payment", "cancel")
NEGATIVE_KEYWORDS = ("angry", "frustrated", "upset", "terrible", "unacceptable")
POSITIVE_KEYWORDS = ("thanks", "thank you", "great", "appreciate")


def classify_with_rules(request: ClassificationRequest) -> ClassificationResponse:
    text = _normalize(f"{request.subject} {request.customerMessage}")
    category, category_match_count = _classify_category(text)
    urgency, priority = _classify_priority(text, category_match_count)
    sentiment = _classify_sentiment(text)
    confidence = _confidence_for(text, category_match_count, urgency, sentiment)

    return ClassificationResponse(
        category=category,
        urgency=urgency,
        sentiment=sentiment,
        priority=priority,
        confidence=confidence,
        classifierVersion=CLASSIFIER_VERSION,
    )


def _normalize(value: str) -> str:
    return " ".join(value.lower().split())


def _classify_category(text: str) -> tuple[str, int]:
    best_category = "general"
    best_match_count = 0

    for category, keywords in CATEGORY_KEYWORDS:
        match_count = sum(1 for keyword in keywords if _keyword_matches(text, keyword))
        if match_count > best_match_count:
            best_category = category
            best_match_count = match_count

    return best_category, best_match_count


def _classify_priority(
    text: str,
    category_match_count: int,
) -> tuple[Urgency, Priority]:
    if _contains_any(text, CRITICAL_KEYWORDS):
        return Urgency.CRITICAL, Priority.URGENT

    if _contains_any(text, HIGH_KEYWORDS):
        return Urgency.HIGH, Priority.HIGH

    if category_match_count > 0:
        return Urgency.NORMAL, Priority.MEDIUM

    return Urgency.LOW, Priority.LOW


def _classify_sentiment(text: str) -> Sentiment:
    if _contains_any(text, NEGATIVE_KEYWORDS):
        return Sentiment.NEGATIVE

    if _contains_any(text, POSITIVE_KEYWORDS):
        return Sentiment.POSITIVE

    return Sentiment.NEUTRAL


def _confidence_for(
    text: str,
    category_match_count: int,
    urgency: Urgency,
    sentiment: Sentiment,
) -> float:
    has_escalation = urgency in {Urgency.HIGH, Urgency.CRITICAL}

    if urgency == Urgency.CRITICAL or (
        category_match_count >= 2
        and has_escalation
        and sentiment == Sentiment.NEGATIVE
    ):
        return 0.92

    if category_match_count > 0 and has_escalation:
        return 0.84

    if category_match_count > 0:
        return 0.74

    if has_escalation or sentiment != Sentiment.NEUTRAL:
        return 0.58

    return 0.41


def _contains_any(text: str, keywords: tuple[str, ...]) -> bool:
    return any(_keyword_matches(text, keyword) for keyword in keywords)


def _keyword_matches(text: str, keyword: str) -> bool:
    return re.search(rf"\b{re.escape(keyword)}\b", text) is not None
