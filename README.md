# SupportFlow AI

멀티테넌트 고객문의 운영 AI Agent 플랫폼입니다.

## 목표

고객문의 접수, 분류, 우선순위화, FAQ/정책 문서 기반 RAG 검색, 답변 초안 생성, 상담사 검토, 운영 대시보드까지 연결되는 AI 운영 플랫폼을 구현합니다.

## 기술 스택

- Backend: Spring Boot
- AI Service: Python FastAPI
- Database: MongoDB
- Cache: Redis
- Infra: Docker Compose
- Test: JUnit5, Testcontainers, Pytest, Playwright

## 개발 방식

- GSD 기반 목표 정의
- SDD 기반 기능 명세
- Codex 기반 AI-assisted 개발
- Harness 기반 자동 검증

## Backend Foundation Local Development

Start MongoDB:

```bash
docker compose up -d mongodb
```

Run the Spring backend:

```bash
cd backend-spring && ./mvnw spring-boot:run
```

Run tests:

```bash
cd backend-spring && ./mvnw test
cd backend-spring && ./mvnw verify
```

API documentation:

- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui.html
- Phase 1 backend contract: `docs/sdd/phase-01-backend-foundation-api.md`
- Phase 2 tenant workflow contract: `docs/sdd/phase-02-tenant-workflow-core-api.md`
- Phase 3 knowledge base contract: `docs/sdd/phase-03-knowledge-base-core-api.md`
- Phase 4 AI classification contract: `docs/sdd/phase-04-ai-classification-integration-api.md`

Phase 2 verification:

```bash
cd backend-spring && ./mvnw verify
```

## Phase 4 AI Classification Local Development

Run the FastAPI AI service tests:

```bash
cd ai-service-python
. .venv/bin/activate 2>/dev/null || python3 -m venv .venv && . .venv/bin/activate
python -m pip install "fastapi[standard]==0.136.3" "pydantic==2.13.4" "pytest==9.0.3" "httpx>=0.27,<1"
python -m pytest
```

Run the full local stack with Docker Compose:

```bash
docker compose up --build
```

The compose stack includes `mongodb`, `backend`, `ai-service`, and `redis`. Classification uses the FastAPI service through:

```bash
SUPPORTFLOW_AI_CLASSIFICATION_BASE_URL=http://ai-service:8000
```

Targeted Phase 4 verification:

```bash
cd backend-spring && ./mvnw test -Dtest=TicketClassificationMongoIntegrationTest
cd backend-spring && ./mvnw test -Dtest=OpenApiDocumentationTest,FoundationVerificationTest
```

Full Phase 4 verification:

```bash
cd ai-service-python && python -m pytest
cd ../backend-spring && ./mvnw verify
```

Phase 4 uses deterministic local keyword classification only: no provider-backed LLM, no prompts, no RAG, no embeddings, no vector search, no draft generation, no async jobs, no frontend UI, no authentication, and no full RBAC.
