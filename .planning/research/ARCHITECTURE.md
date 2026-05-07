# Architecture Research

**Domain:** Multi-tenant customer support operations platform with separate AI service
**Researched:** 2026-05-07
**Confidence:** HIGH

## Standard Architecture

### System Overview

```text
┌────────────────────────────────────────────────────────────────────┐
│                         Interaction Layer                         │
├────────────────────────────────────────────────────────────────────┤
│  Admin API / Future UI   Agent API / Future UI   API Docs         │
└──────────────┬──────────────────────┬──────────────────────────────┘
               │                      │
┌──────────────┴──────────────────────┴──────────────────────────────┐
│                      Operational Backend Layer                     │
├────────────────────────────────────────────────────────────────────┤
│ Tenant Mgmt │ Ticket Service │ Workflow Rules │ Knowledge Service  │
│ Draft/Approval Service │ Metrics/Reporting │ Audit/History         │
└──────────────┬──────────────────────┬──────────────────────────────┘
               │                      │
┌──────────────┴───────────────┐  ┌───┴──────────────────────────────┐
│      AI Service Layer         │  │         Async / Cache Layer      │
├───────────────────────────────┤  ├──────────────────────────────────┤
│ Classification │ Retrieval    │  │ Redis job state / cache / queue  │
│ Draft Generation │ Eval hooks │  │ orchestration primitives         │
└──────────────┬────────────────┘  └───┬──────────────────────────────┘
               │                        │
┌──────────────┴────────────────────────┴─────────────────────────────┐
│                         Data / Evidence Layer                        │
├──────────────────────────────────────────────────────────────────────┤
│ MongoDB collections: tenants, users, tickets, knowledge, drafts,    │
│ approvals, audit events, metrics snapshots, AI result records        │
└──────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Tenant management | Owns tenant/workspace lifecycle and tenant-scoped admin rules | Spring Boot module with strict tenant-aware services and validation |
| Ticket workflow | Owns ticket creation, retrieval, state transitions, assignment, and SLA metadata | Spring Boot domain/service layer backed by MongoDB |
| Knowledge service | Owns FAQ/policy document ingestion, storage, and retrieval metadata | Spring Boot operational API with AI service integration hooks |
| AI service | Owns classification, evidence retrieval, and draft response generation | FastAPI service with explicit request/response contracts |
| Draft/approval service | Owns draft revisions, human edits, approval decisions, and finalization rules | Spring Boot service with immutable history records |
| Metrics/reporting | Owns operational counters and dashboard-friendly summaries | Read-model or aggregation endpoints over workflow data |
| Audit/history | Owns append-only operational trace | Dedicated event/history collection and serializers |

## Recommended Project Structure

```text
/
├── backend-spring/              # Spring Boot operational backend
│   ├── src/main/java/.../tenant # Tenant provisioning and admin domain
│   ├── src/main/java/.../ticket # Ticket aggregates, services, API
│   ├── src/main/java/.../knowledge
│   ├── src/main/java/.../draft
│   ├── src/main/java/.../metrics
│   ├── src/main/java/.../common # tenant context, errors, config
│   └── src/test/...             # unit, slice, integration tests
├── ai-service-python/           # FastAPI AI service
│   ├── app/api/                 # HTTP endpoints
│   ├── app/services/            # classification, retrieval, drafting
│   ├── app/models/              # request/response schemas
│   ├── app/clients/             # backend or vector/LLM clients
│   └── tests/                   # pytest suites
├── frontend/                    # later-phase UI
├── harness/                     # load, failure, and verification harnesses
├── docs/                        # specs, API contracts, architecture notes
├── docker-compose.yml           # reproducible local environment
└── .planning/                   # GSD project management artifacts
```

### Structure Rationale

- **`backend-spring/`**: keep the operational API and domain rules centralized and strongly typed.
- **`ai-service-python/`**: keep model-driven behavior, retrieval logic, and prompt orchestration isolated from CRUD workflows.
- **`harness/`**: treat verification as a first-class part of the repo, not an afterthought.
- **`docs/`**: support the spec-first process with durable API and design contracts.

## Architectural Patterns

### Pattern 1: Tenant-Scoped Service Boundary

**What:** Every tenant-scoped operation requires tenant context at the service boundary, not just at the controller layer.
**When to use:** Always, for tickets, knowledge documents, drafts, logs, and metrics.
**Trade-offs:** Slightly more boilerplate, much lower risk of cross-tenant leaks.

### Pattern 2: Explicit Workflow State Machine

**What:** Ticket status transitions are modeled as allowed state changes with validation and audit logging.
**When to use:** From Phase 1 onward.
**Trade-offs:** More up-front domain modeling, but dramatically better correctness and reporting.

### Pattern 3: AI-as-Assistant, Not Source of Truth

**What:** AI outputs are advisory artifacts attached to a ticket, not authoritative final responses.
**When to use:** For classification, priority suggestions, evidence bundles, and response drafts.
**Trade-offs:** More review workflow plumbing, but safer customer-facing behavior and clearer accountability.

## Data Flow

### Request Flow

```text
Super admin creates tenant
    ↓
Tenant service → validation → MongoDB tenant record

Agent creates ticket
    ↓
Ticket API → ticket service → state initialization → MongoDB ticket
    ↓
Optional async trigger → AI service request
    ↓
Classification / retrieval / draft generation
    ↓
Results stored as AI artifacts attached to ticket
    ↓
Human review/edit/approve
    ↓
Final response + history + metrics update
```

### State Management

```text
Ticket
  ├── current status
  ├── current assignee / priority / SLA metadata
  ├── latest AI analysis
  ├── latest evidence bundle
  └── draft + approval history
```

### Key Data Flows

1. **Ticket handling flow:** tenant-scoped ticket CRUD, status transitions, assignment, and history writes.
2. **AI assistance flow:** ticket snapshot → AI service → classification/retrieval/draft artifacts → human decision.
3. **Metrics flow:** workflow events → aggregation endpoints or snapshots for backlog/status/progress dashboards.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k tickets/day | Monorepo + one Spring service + one FastAPI service + MongoDB + Redis is enough |
| 1k-20k tickets/day | Add background workers, richer Redis orchestration, query/index tuning, metrics pre-aggregation |
| 20k+ tickets/day | Split retrieval/generation workloads, introduce dedicated queues, harden observability and storage projections |

### Scaling Priorities

1. **First bottleneck:** AI request latency and retry behavior — isolate async execution and persist job/result state clearly.
2. **Second bottleneck:** MongoDB query/index quality for inbox and dashboard views — design compound indexes early around `tenantId`, status, assignee, priority, and timestamps.

## Anti-Patterns

### Anti-Pattern 1: Controller-Level Tenant Checks Only

**What people do:** Validate tenant at the request edge, then use generic repositories underneath.
**Why it's wrong:** One forgotten repository method can leak cross-tenant data.
**Do this instead:** Build tenant-scoped service/repository APIs and test failure cases directly.

### Anti-Pattern 2: Mutable “Current Draft” Without History

**What people do:** Overwrite the current AI draft in place.
**Why it's wrong:** You lose who changed what, what the model suggested, and what was finally approved.
**Do this instead:** Store immutable draft revisions and explicit approval events.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| AI model providers | Encapsulated behind FastAPI service adapters | Keep provider-specific behavior out of the Spring backend |
| Future inbound channels | Adapter layer into ticket creation workflow | Do not let channel-specific concerns rewrite core ticket rules |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Spring backend ↔ FastAPI AI service | HTTP API with explicit contracts | Keep payloads versioned and auditable |
| Spring backend ↔ Redis | Cache / job-state operations | Avoid turning Redis into the durable source of truth |
| Spring backend ↔ MongoDB | Direct persistence | All tenant-scoped collections need indexed tenant-aware access patterns |

## Sources

- Spring Boot docs — service structure, testing, configuration, and production conventions
- Spring Security docs — future auth and authorization layering
- MongoDB docs — document-model and operational datastore guidance
- Redis docs — cache and transient orchestration patterns
- Intercom and Zendesk support workspace docs — evidence that modern support tools center inbox, assignment, knowledge, and AI assist patterns

---
*Architecture research for: multi-tenant support operations AI platform*
*Researched: 2026-05-07*
