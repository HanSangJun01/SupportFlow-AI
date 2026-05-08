# Walking Skeleton — SupportFlow AI

**Phase:** 1
**Generated:** 2026-05-08

## Capability Proven End-to-End

A local operator can start the Spring backend with MongoDB, create a tenant, create a tenant-scoped ticket, list/view that ticket through tenant path APIs, and reject an invalid ticket lifecycle transition.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Backend framework | Spring Boot 3.5.14, Java 21, Maven | Current Boot 3.5 stable line preserves ecosystem compatibility while supporting modern Spring testing and MongoDB integration. |
| Data layer | MongoDB through Spring Data MongoDB | Ticket, tenant, and future support artifacts are naturally tenant-scoped document records. |
| API style | REST under `/api/v1` with tenant IDs in URL paths | Locked by D-01 and directly testable before authentication exists. |
| Auth | None in Phase 1 | Authentication and richer role enforcement are explicitly deferred beyond the backend foundation. |
| API documentation | Springdoc OpenAPI and Swagger UI | Locked by D-13 and useful for later frontend consumption. |
| Deployment target | Local Docker Compose + Maven run command | v1 is local-first and reproducible; production deployment is out of scope. |
| Directory layout | Domain packages under `backend-spring/src/main/java/com/supportflow/{tenant,ticket,common}` | Keeps tenant and ticket boundaries explicit from the first implementation phase. |

## Stack Touched in Phase 1

- [ ] Project scaffold: Spring Boot Maven project, wrapper, build, tests.
- [ ] Routing: real REST routes for tenants and tenant-scoped tickets.
- [ ] Database: real MongoDB write and read for tenant and ticket documents.
- [ ] Interaction: Swagger UI and curl-able HTTP endpoints exercise the backend; product frontend remains deferred.
- [ ] Deployment/local run: Docker Compose plus documented backend run command exercises the local stack.

## Out of Scope (Deferred to Later Slices)

- Frontend operations UI.
- Authentication and role enforcement.
- Tenant update workflows.
- Full ticket history and actor attribution.
- AI service integration.
- Redis-backed async processing.
- Knowledge base, draft approval, metrics, billing, external channels, and SaaS onboarding.

## Subsequent Slice Plan

- Phase 2: Tenant workflow core with tenant metadata, operational users/roles, ownership, priority fields, and workflow history.
- Phase 3: Tenant-scoped knowledge document management.
- Phase 4: Separate AI classification service and backend integration.
- Phase 5: Tenant-scoped evidence retrieval.
- Phase 6: AI draft review and approval workflow.
- Phase 7: Operational metrics.

