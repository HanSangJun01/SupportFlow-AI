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
