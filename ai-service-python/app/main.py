from fastapi import FastAPI

from app.classifier import classify_with_rules
from app.models import ClassificationRequest, ClassificationResponse, HealthResponse
from app.settings import SERVICE_NAME, SERVICE_VERSION

app = FastAPI(title=SERVICE_NAME, version=SERVICE_VERSION)


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post(
    "/api/v1/classifications/tickets",
    response_model=ClassificationResponse,
)
async def classify_ticket(request: ClassificationRequest) -> ClassificationResponse:
    return classify_with_rules(request)
