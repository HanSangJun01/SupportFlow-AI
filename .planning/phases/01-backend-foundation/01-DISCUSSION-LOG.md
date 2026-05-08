# Phase 1: Backend Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-05-08
**Phase:** 1-Backend Foundation
**Areas discussed:** Tenant identity in APIs, tenant workspace APIs, ticket lifecycle rules, ticket foundation fields, API documentation, integration testing

---

## Tenant Identity in APIs

| Option | Description | Selected |
|--------|-------------|----------|
| URL path only | Use `/api/v1/tenants/{tenantId}/tickets`. Clear, easy to test, and explicit for tenant isolation. | yes |
| Header only | Use `X-Tenant-Id` on tenant-scoped routes. Closer to future auth middleware, but easier to miss in manual API use. | |
| Both path and header | Require both and reject mismatches. Strongest isolation signal, but more ceremony for Phase 1. | |

**User's choice:** URL path only.
**Notes:** Tenant-scoped Phase 1 APIs should use URL paths before authentication exists.

---

## Tenant Workspace Creation

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal | `name` only, backend generates the tenant ID and defaults status to `ACTIVE`. | |
| Operational basics | `name`, `slug`, and optional `description`; backend generates ID and defaults status to `ACTIVE`. | yes |
| Controlled admin metadata | `name`, `slug`, `status`, and `adminContactEmail`. | |

**User's choice:** Operational basics.
**Notes:** Phase 1 should capture enough tenant identity for stable API verification without pulling richer administration into the foundation phase.

---

## Tenant API Surface

| Option | Description | Selected |
|--------|-------------|----------|
| Create and detail only | `POST /api/v1/tenants`, `GET /api/v1/tenants/{tenantId}`. | |
| Create, list, and detail | Also include `GET /api/v1/tenants` for super-admin verification. | yes |
| Create, list, detail, and update | Include update now, even though roadmap puts richer tenant metadata in Phase 2. | |

**User's choice:** Create, list, and detail.
**Notes:** Tenant update stays out of Phase 1.

---

## Ticket Lifecycle Rules

| Option | Description | Selected |
|--------|-------------|----------|
| Linear only | `NEW -> TRIAGED -> IN_PROGRESS -> ANSWERED -> CLOSED`; no backwards moves. | |
| Practical support loop | Linear forward, plus `ANSWERED -> IN_PROGRESS` when a reply needs rework. | yes |
| Flexible early MVP | Allow any forward movement and limited reopen from `CLOSED -> IN_PROGRESS`. | |

**User's choice:** Practical support loop.
**Notes:** No closed-ticket reopen behavior in Phase 1.

---

## Ticket Creation Fields

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal inquiry | `subject`, `customerMessage`. | |
| Basic customer context | `subject`, `customerName`, `customerEmail`, `customerMessage`. | yes |
| Operational intake | Basic customer context plus optional `category` and `priority`. | |

**User's choice:** Basic customer context.
**Notes:** Category and priority should not be required at creation in Phase 1.

---

## Ticket Detail Shape

| Option | Description | Selected |
|--------|-------------|----------|
| Core only | `id`, `tenantId`, `subject`, customer fields, `status`, `createdAt`, `updatedAt`. | |
| Core plus simple priority/category placeholders | Include nullable `category`, `priority`, `assigneeId`. | yes |
| Full operational detail | Include category, urgency, sentiment, priority, assignee, SLA-risk fields, and history. | |

**User's choice:** Core plus simple priority/category placeholders.
**Notes:** Full operational enrichment and history stay in Phase 2.

---

## Ticket Listing Filters

| Option | Description | Selected |
|--------|-------------|----------|
| Status only | Enough to prove lifecycle and tenant-scoped queries. | |
| Status, priority, assignee, created date | Matches `TICK-02` directly. | yes |
| Status and created date only | Simpler but leaves priority/assignee filtering to Phase 2. | |

**User's choice:** Status, priority, assignee, created date.
**Notes:** Phase 1 should fully cover `TICK-02`.

---

## API Documentation Style

| Option | Description | Selected |
|--------|-------------|----------|
| Springdoc OpenAPI | Generate `/v3/api-docs` and Swagger UI from controller annotations/config. | yes |
| Spring REST Docs | Generate docs from tests; more verification-driven but more setup. | |
| Lightweight Markdown plus controller tests | Faster, less formal API contract. | |

**User's choice:** Springdoc OpenAPI.
**Notes:** API documentation should be generated from the Spring backend.

---

## Integration Testing Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Repository/service integration only | MongoDB persistence and tenant-scoped queries. | |
| Full API integration | HTTP endpoint tests against Spring Boot + MongoDB Testcontainer. | yes |
| Both targeted service tests and full API integration tests | Broader proof, more setup. | |

**User's choice:** Full API integration.
**Notes:** Unit tests can still cover pure lifecycle rules, but isolation proof should run through HTTP endpoints.

---

## the agent's Discretion

- Exact Java package names, DTO names, MongoDB collection names, validation annotation details, and test class organization.
- Whether API tests live entirely under `backend-spring/src/test` or whether a lightweight harness is useful in addition.

## Deferred Ideas

- Tenant update workflows.
- Full ticket history.
- Ticket urgency, sentiment, SLA-risk fields, and richer prioritization behavior.
- Authentication, frontend UI, AI workflows, Redis async behavior, metrics, and SaaS expansion.
