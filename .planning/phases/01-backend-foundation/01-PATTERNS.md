# Phase 1: Backend Foundation - Pattern Map

**Generated:** 2026-05-08
**Status:** No existing application code patterns

## Summary

The repository has planning artifacts and placeholder service directories, but no existing Spring Boot application code. Phase 1 establishes the initial backend implementation patterns. Executors should follow the contracts in `01-CONTEXT.md`, `01-RESEARCH.md`, and the plan files rather than trying to infer style from nonexistent code.

## Planned File Roles

| File Pattern | Role | Closest Existing Analog | Notes |
|--------------|------|-------------------------|-------|
| `backend-spring/pom.xml` | Java build and dependency management | None | Establishes Spring Boot, MongoDB, validation, springdoc, and Testcontainers baseline. |
| `backend-spring/src/main/java/com/supportflow/SupportFlowApplication.java` | Spring Boot entrypoint | None | First application entrypoint in repo. |
| `backend-spring/src/main/java/com/supportflow/common/*` | Shared API error and OpenAPI config | None | Keep generic HTTP concerns out of domain packages. |
| `backend-spring/src/main/java/com/supportflow/tenant/*` | Tenant workspace domain/API | None | Tenant create/list/detail only; tenant update remains Phase 2. |
| `backend-spring/src/main/java/com/supportflow/ticket/*` | Tenant-scoped ticket domain/API | None | Every service/repository operation touching tickets must include tenant ID. |
| `backend-spring/src/test/java/com/supportflow/**/*Test.java` | Unit and HTTP integration coverage | None | Use unit tests for lifecycle policy and Spring/Testcontainers integration tests for API and isolation. |
| `docs/sdd/phase-01-backend-foundation-api.md` | API contract documentation | `docs/sdd/` placeholder directory | This phase starts the SDD documentation pattern. |

## Implementation Pattern Decisions

- Package by domain, not by technical layer: `tenant`, `ticket`, and `common`.
- Controllers should be thin: validate request DTOs, pass path tenant IDs into services, and return response DTOs.
- Services enforce existence checks, lifecycle rules, and tenant-aware persistence boundaries.
- Repositories expose tenant-aware methods such as `findByTenantIdAndId`; avoid tenant-scoped reads through `findById`.
- Tests must prove behavior through HTTP wherever tenant isolation is at stake.

## Non-Patterns to Avoid

- Do not add authentication or role checks in Phase 1.
- Do not add tenant update APIs.
- Do not add full ticket history or actor attribution.
- Do not make Redis required by backend startup or tests.
- Do not use request headers for tenant identity; D-01 requires URL path tenant IDs.

