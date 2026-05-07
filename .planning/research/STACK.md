# Stack Research

**Domain:** Multi-tenant customer support operations platform with human-in-the-loop AI workflows
**Researched:** 2026-05-07
**Confidence:** HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Spring Boot | 3.5.13 | Main backend API | Stable current Spring line with broad ecosystem compatibility and lower migration risk than adopting Boot 4 for an MVP foundation. |
| Spring Security | 6.5.10 | Authentication and authorization | Aligns with the Spring 6 / Boot 3 generation and is the standard path for API security, method security, and future RBAC. |
| MongoDB Community Server | 8.2.7 | Primary operational datastore | Fits tenant-scoped ticketing, knowledge docs, drafts, logs, and workflow records with flexible document models and indexing. |
| Redis Open Source | 8.2.x | Caching and async job state | Good fit for transient AI job coordination, rate limiting, queue-like status tracking, and short-lived workflow state. |
| FastAPI | 0.135.3 | Separate AI service API | Strong OpenAPI support, Python typing, and fast implementation speed for classification, retrieval, and draft generation endpoints. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring Data MongoDB | Managed by Spring Boot 3.5.13 | Mongo persistence and repository support | Use for tenant-scoped aggregates, indexed queries, and lifecycle hooks. |
| Springdoc OpenAPI | 2.x line compatible with Boot 3.5 | API documentation | Use from Phase 1 to expose backend contracts and support verification. |
| JUnit Jupiter | 5.13.1 | Java unit and slice testing | Use for domain, service, and controller tests in the Spring backend. |
| Mockito | 5.23.0 | Java mocking | Use for isolated backend unit tests where collaborators should not hit infrastructure. |
| Testcontainers for Java | 2.0.4 | Integration tests with real services | Use for MongoDB and Redis integration tests to prove tenant isolation and persistence behavior. |
| Pytest | 9.0.3 | Python AI service testing | Use for FastAPI endpoint tests, retrieval logic, prompt-flow tests, and contract tests. |
| Playwright | 1.59 | End-to-end UI testing | Add once the frontend exists; use for end-to-end workflow verification across backend and UI. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Docker Compose v2 / Compose Specification | Local orchestration | Use named services for `backend`, `ai-service`, `mongodb`, `redis`, and later `frontend` / harness services. |
| Maven or Gradle | Java build | Maven is fine for explicit enterprise-style reproducibility; Gradle is fine if the team prefers convention-heavy builds. |
| Uvicorn | FastAPI runtime | Good default local ASGI server for the AI service. |
| GitHub PR workflow | Traceable delivery | Aligns with the project's requirement for atomic commits and reviewed feature branches. |

## Installation

```bash
# Java backend
# Use Spring Boot 3.5.13 with Spring Data MongoDB, Validation, Actuator, and OpenAPI support

# Python AI service
pip install "fastapi[standard]==0.135.3" pytest==9.0.3

# Browser E2E (later phase)
npm install -D @playwright/test@1.59
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Spring Boot 3.5.13 | Spring Boot 4.0.5 | Use Boot 4 if you explicitly want the newest servlet stack and are ready to absorb newer ecosystem edges earlier. |
| MongoDB 8.2.x | PostgreSQL | Use PostgreSQL if relational joins, stricter schema evolution, or complex transactional reporting become dominant. |
| Redis 8.2.x | RabbitMQ / Kafka | Use a dedicated broker only when async workflows outgrow simple job-state and short-latency orchestration needs. |
| FastAPI 0.135.3 | Flask / Django / BentoML | Use Flask only for a thinner service; use BentoML when model serving concerns outweigh application orchestration concerns. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Shared tenant-agnostic repositories with optional tenant filters | This is how cross-tenant leaks happen in practice. | Enforce tenant-aware repository and service boundaries by default. |
| Boot 4 for the first foundation phase by default | Technically viable, but unnecessary migration surface for a verification-first MVP. | Start on Boot 3.5.13 and upgrade later deliberately. |
| Embedding AI logic directly in the Spring service | Blurs operational and AI boundaries, complicates deployment and verification. | Keep a separate FastAPI AI service. |
| Treating Redis as the system of record | Cache/job state stores are the wrong place for durable support workflow history. | Keep MongoDB as the source of truth. |

## Stack Patterns by Variant

**If the MVP remains local-first and backend-heavy:**
- Use Spring Boot 3.5.13 + MongoDB + Redis + FastAPI.
- Because it minimizes moving parts while preserving clear service boundaries.

**If the AI workflow becomes compute-heavy later:**
- Keep the same operational backend, but scale the FastAPI service independently.
- Because classification/retrieval/drafting load will not grow like the CRUD API load.

**If reporting needs expand materially:**
- Add dedicated analytics projections or warehouse exports later.
- Because MongoDB operational collections should not become a BI substitute.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Spring Boot 3.5.13 | Spring Security 6.5.10 | Conservative, stable pairing for the Boot 3 line. |
| Spring Boot 3.5.13 | Java 17+ | Chosen to keep broad compatibility without forcing a bleeding-edge Java move. |
| FastAPI 0.135.3 | Python 3.10+ | Verified on PyPI metadata. |
| Testcontainers 2.0.4 | JUnit 5.13.1 | Standard Java integration-testing pairing. |
| Playwright 1.59 | Node 20+ preferred | Suitable when frontend and end-to-end testing arrive. |

## Sources

- https://docs.spring.io/spring-boot/index.html — verified current stable Spring Boot lines
- https://docs.spring.io/spring-boot/3.5/how-to/deployment/installing.html — verified Spring Boot 3.5.13 docs exist and are current
- https://docs.spring.io/spring-security/reference/index.html — verified current stable Spring Security lines
- https://www.mongodb.com/try/download/community — verified current MongoDB Community Server release line
- https://redis.io/docs/latest/operate/oss_and_stack/install/version-mgmt/ — verified supported Redis Open Source lines
- https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/release-notes/redisce/redisos-8.2-release-notes/ — verified Redis 8.2 GA and patch line
- https://pypi.org/project/fastapi/ — verified current FastAPI release and Python support
- https://docs.pytest.org/en/latest/ and https://pypi.org/project/pytest/ — verified pytest docs and current release
- https://java.testcontainers.org/ and https://github.com/testcontainers/testcontainers-java/releases — verified Testcontainers for Java usage and current release
- https://docs.junit.org/5.13.1/user-guide/index.html — verified JUnit Jupiter version
- https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html — verified Mockito current version
- https://playwright.dev/docs/release-notes — verified current Playwright release line
- https://docs.docker.com/reference/compose-file/ — verified Compose Specification guidance

---
*Stack research for: multi-tenant support operations AI platform*
*Researched: 2026-05-07*
