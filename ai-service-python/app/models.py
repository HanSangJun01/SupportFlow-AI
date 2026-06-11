from enum import StrEnum

from pydantic import BaseModel, ConfigDict, Field


class Urgency(StrEnum):
    LOW = "LOW"
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class Sentiment(StrEnum):
    NEGATIVE = "NEGATIVE"
    NEUTRAL = "NEUTRAL"
    POSITIVE = "POSITIVE"


class Priority(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    URGENT = "URGENT"


class ClassificationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    tenantId: str = Field(min_length=1)
    ticketId: str = Field(min_length=1)
    subject: str = Field(min_length=1, max_length=200)
    customerMessage: str = Field(min_length=1, max_length=5000)


class ClassificationResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    category: str = Field(min_length=1, max_length=80)
    urgency: Urgency
    sentiment: Sentiment
    priority: Priority
    confidence: float = Field(ge=0.0, le=1.0)
    classifierVersion: str = Field(min_length=1)


class HealthResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status: str
