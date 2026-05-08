# Phase 1: Backend Foundation - Research

**Researched:** 2026-05-08
**Status:** Ready for planning
**Confidence:** HIGH

## Executive Summary

Phase 1 should establish a conservative Spring Boot backend foundation rather than chase the newest major line. As of this planning run, Spring Boot documentation lists 4.0.6 and 3.5.14 as stable lines. The project research already selected the Spring Boot 3.5 line to reduce ecosystem migration risk while still staying current. That remains the right Phase 1 choice because the phase is about tenant isolation, lifecycle correctness, integration tests, and reproducible local execution.

The execution plan should use Maven, Java 21, Spring Web MVC, Spring Data MongoDB, Spring Validation, Actuator, Springdoc OpenAPI, JUnit Jupiter, and Testcontainers. MongoDB should be the only required Phase 1 datastore. Redis can remain a future service in the repository but must not be required by backend startup or tests during Phase 1.

## Planning-Relevant Findings

### Stack Baseline

- Use Spring Boot `3.5.14` for the backend scaffold. Spring Boot docs list `3.5.14` as a current stable line, while `4.0.6` is also stable. Phase 1 should stay on Boot 3.5 to preserve broad ecosystem compatibility.
- Use Java 21 for the Maven project. Java 17 would work, but Java 21 is a practical current LTS target and fits Spring Boot 3.5.
- Use `spring-boot-starter-web`, `spring-boot-starter-data-mongodb`, `spring-boot-starter-validation`, and `spring-boot-starter-actuator`.
- Use `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17`; springdoc's current documentation identifies v2.8.17.
- Use Spring Boot managed JUnit Jupiter where possible and add Testcontainers MongoDB for HTTP-level integration tests.
- Testcontainers' MongoDB module provides `MongoDBContainer`, and the JUnit 5 integration uses `@Testcontainers` and `@Container`.

### Architecture for Phase 1

- Package by domain: `com.supportflow.tenant`, `com.supportflow.ticket`, and `com.supportflow.common`.
- Keep tenant ID explicit in path-based APIs, per D-01: `/api/v1/tenants/{tenantId}/tickets`.
- Use service methods that always accept `tenantId` for ticket operations. Avoid repository calls that can fetch tenant-scoped resources by ID alone.
- Use Mongo document fields:
  - Tenant: `id`, `name`, `slug`, `description`, `status`, `createdAt`, `updatedAt`.
  - Ticket: `id`, `tenantId`, `subject`, `customerName`, `customerEmail`, `customerMessage`, `status`, nullable `category`, nullable `priority`, nullable `assigneeId`, `createdAt`, `updatedAt`.
- Keep Phase 1 history minimal. Full workflow history is Phase 2, but status transition validation is Phase 1.

### API Surface

Phase 1 should expose exactly these functional APIs:

- `POST /api/v1/tenants`
- `GET /api/v1/tenants`
- `GET /api/v1/tenants/{tenantId}`
- `POST /api/v1/tenants/{tenantId}/tickets`
- `GET /api/v1/tenants/{tenantId}/tickets`
- `GET /api/v1/tenants/{tenantId}/tickets/{ticketId}`
- `PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/status`

Ticket list filters must include `status`, `priority`, `assigneeId`, `createdFrom`, and `createdTo` to satisfy TICK-02 and D-12.

### Lifecycle Rules

Use enum values from D-05: `NEW`, `TRIAGED`, `IN_PROGRESS`, `ANSWERED`, `CLOSED`.

Allowed transitions:

- `NEW -> TRIAGED`
- `TRIAGED -> IN_PROGRESS`
- `IN_PROGRESS -> ANSWERED`
- `ANSWERED -> CLOSED`
- `ANSWERED -> IN_PROGRESS`

Rejected transitions include:

- Any transition from `CLOSED`
- `NEW -> ANSWERED`
- `NEW -> CLOSED`
- `TRIAGED -> ANSWERED`
- `IN_PROGRESS -> CLOSED`

### Validation Architecture

Phase 1 validation should sample every critical behavior through automated commands:

- Build and fast unit test command: `cd backend-spring && ./mvnw test`
- Integration test command: `cd backend-spring && ./mvnw verify`
- API docs check: start app through Maven or test profile and assert `/v3/api-docs` contains tenant and ticket paths.
- Tenant isolation proof must be HTTP-level integration tests using Spring Boot plus MongoDB Testcontainer. Tests must create at least two tenants and verify that a ticket created under tenant A cannot be read or mutated through tenant B's path.

### Security and Threat Notes

Primary Phase 1 trust boundaries:

- Public HTTP request to Spring controllers.
- Tenant ID path parameter into service and repository access.
- MongoDB persistence boundary.
- API docs endpoint exposing route metadata.

Mitigations should focus on validation, tenant-scoped repository methods, and negative HTTP tests. Authentication is explicitly out of scope, so security claims must not imply role enforcement beyond controlled local API access.

## Source Notes

- Spring Boot docs list stable lines including `4.0.6` and `3.5.14`; use `3.5.14` for this phase because the project already chose the Boot 3.5 line for MVP stability.
- Spring Boot Testcontainers documentation describes using Testcontainers for integration tests against real backend services such as MongoDB.
- Testcontainers MongoDB documentation provides `MongoDBContainer`, and the JUnit 5 documentation describes `@Testcontainers` and `@Container`.
- springdoc documentation identifies `springdoc-openapi v2.8.17`.

## Out of Scope for This Phase

- Authentication and role enforcement.
- Tenant metadata update workflows.
- Full ticket workflow history.
- AI service integration.
- Redis-backed async processing.
- Frontend UI.

## RESEARCH COMPLETE

