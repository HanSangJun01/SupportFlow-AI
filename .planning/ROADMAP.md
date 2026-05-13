# Roadmap: SupportFlow AI

## Overview

SupportFlow AI will be built as a backend-first, vertical MVP support operations platform. The official v1 roadmap ends at Phase 7, where the system proves the full controlled local support operations workflow through backend APIs, API documentation, automated tests, and harnesses. Post-v1 work then expands the platform with authentication, frontend UI, deeper async and reliability behavior, and broader SaaS or integration features. Each v1 phase is designed to deliver a coherent end-to-end capability while preserving tenant isolation, traceability, and verification discipline.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Backend Foundation** - Establish the Spring Boot and MongoDB base with tenant-aware ticket APIs and tests.
- [ ] **Phase 2: Tenant Workflow Core** - Harden tenant workspace operations, workflow rules, and ticket history.
- [ ] **Phase 3: Knowledge Base Core** - Add tenant-scoped FAQ and policy document management for later retrieval.
- [ ] **Phase 4: AI Classification Integration** - Introduce the separate AI service and ticket classification workflow.
- [ ] **Phase 5: Evidence Retrieval** - Add tenant-scoped RAG evidence retrieval attached to tickets.
- [ ] **Phase 6: Draft Review and Approval** - Deliver AI draft generation with human review, edit, and approval controls.
- [ ] **Phase 7: Operational Metrics** - Expose core support operations metrics and progress visibility.

## v1 Boundary

**v1 completes at the end of Phase 7.**

The official v1 workflow proven by this roadmap is:

super-admin tenant provisioning
→ tenant-isolated ticket operations
→ ticket lifecycle state transitions
→ tenant-scoped knowledge base
→ AI ticket classification
→ RAG-based FAQ or policy evidence retrieval
→ AI draft response generation based on evidence
→ human review, edit, and approval
→ basic operational metrics

## Phase Details

### Phase 1: Backend Foundation
**Goal**: As a system-level admin and support operator, I want to create tenant workspaces and manage tenant-scoped customer inquiry tickets through documented backend APIs, so that SupportFlow AI has a reliable multi-tenant backend foundation before AI, frontend, and authentication features are added.
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: [TEN-01, TICK-01, TICK-02, TICK-03, TICK-05, FLOW-01, FLOW-02, ISO-01, ISO-02, ISO-04, QUAL-01, QUAL-02, QUAL-03, QUAL-06]
**Success Criteria** (what must be TRUE):
  1. Super admin can create a tenant workspace and retrieve it through documented backend APIs.
  2. Support agent can create, list, and view tenant-scoped tickets through REST endpoints.
  3. Invalid ticket lifecycle transitions are rejected by backend rules.
  4. Automated tests prove tenant-scoped queries and cross-tenant denial for implemented foundation APIs.
**Plans**: 4 plans
**Planning Status**: In progress (02-01 completed 2026-05-13)

Plans:
**Wave 1**
- [x] 01-01: Set up Spring Boot project structure, configuration, and MongoDB connectivity

**Wave 2** *(blocked on 01-01 completion)*
- [x] 01-02: Implement tenant workspace basics and core ticket APIs

**Wave 3** *(blocked on 01-02 completion)*
- [x] 01-03: Implement ticket lifecycle transition rules and tenant-aware persistence boundaries

**Wave 4** *(blocked on 01-03 completion)*
- [x] 01-04: Add REST API documentation and foundation unit/integration tests

Cross-cutting constraints:
- Tenant identity for tenant-scoped APIs uses URL paths such as `/api/v1/tenants/{tenantId}/tickets`.
- Ticket data access must remain tenant-aware by default; cross-tenant read/write denial must be proven through HTTP integration tests.
- Phase 1 remains backend-only: no frontend, authentication, AI service integration, Redis runtime dependency, tenant update workflow, or full ticket history.

### Phase 2: Tenant Workflow Core
**Goal**: Expand the operational core with tenant metadata, operational user/role metadata, ticket ownership fields, history tracking, and prioritization-ready workflow data.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: [TEN-02, TEN-03, TEN-04, TICK-04, FLOW-03, FLOW-04, FLOW-05, ISO-03]
**Success Criteria** (what must be TRUE):
  1. Super admin can view and update tenant workspace metadata safely.
  2. Tickets record actor-attributed lifecycle history and workflow-related metadata.
  3. Support operations can track ownership and prioritization-supporting fields without introducing full SLA automation.
  4. Tenant isolation remains enforced across the expanded workflow model.
**Plans**: 3 plans

Plans:
**Wave 1**
- [x] 02-01: Implement tenant administration read/update workflows and tenant-scoped operational metadata

**Wave 2** *(blocked on Wave 1 completion)*
- [ ] 02-02: Add ticket ownership, prioritization fields, and immutable workflow history

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 02-03: Expand integration coverage for workflow and tenant isolation rules

### Phase 3: Knowledge Base Core
**Goal**: Deliver tenant-scoped knowledge document registration, update, retrieval, and storage models to support later AI retrieval workflows.
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: [KNOW-01, KNOW-02, KNOW-03, KNOW-04]
**Success Criteria** (what must be TRUE):
  1. Tenant admins can register and update FAQ or policy documents for their own tenant workspace.
  2. Knowledge documents remain isolated by tenant in storage and queries.
  3. Knowledge records expose metadata sufficient for later retrieval and evidence linking.
**Plans**: 2 plans

Plans:
- [ ] 03-01: Implement tenant-scoped knowledge document APIs and persistence model
- [ ] 03-02: Add document metadata validation and tenant-isolation tests for knowledge workflows

### Phase 4: AI Classification Integration
**Goal**: Introduce the separate FastAPI AI service and connect it to ticket analysis so agents can inspect ticket classification artifacts.
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: [AI-01, AI-02, AI-03, AI-04, QUAL-04]
**Success Criteria** (what must be TRUE):
  1. The backend can submit a tenant-scoped ticket for AI analysis through a defined inter-service contract.
  2. The AI service returns category, urgency, sentiment, and priority predictions in a testable format.
  3. Classification results are stored as reviewable ticket artifacts and visible to operational consumers.
**Plans**: 3 plans

Plans:
- [ ] 04-01: Scaffold FastAPI AI service with contract-tested analysis endpoint
- [ ] 04-02: Integrate Spring backend with AI classification workflow and artifact storage
- [ ] 04-03: Add automated tests for classification contracts and backend integration

### Phase 5: Evidence Retrieval
**Goal**: Add tenant-scoped evidence retrieval so tickets can surface relevant FAQ or policy context before drafting.
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: [RAG-01, RAG-02, RAG-03, RAG-04]
**Success Criteria** (what must be TRUE):
  1. The system can retrieve tenant-scoped evidence relevant to a ticket.
  2. Retrieved evidence includes traceable source references tied to tenant knowledge documents.
  3. Agents can inspect retrieved evidence alongside ticket and AI analysis context.
**Plans**: 3 plans

Plans:
- [ ] 05-01: Implement retrieval-ready knowledge access patterns and AI retrieval contracts
- [ ] 05-02: Attach evidence bundles to tickets with source traceability
- [ ] 05-03: Add tests proving retrieval correctness and tenant-safe evidence isolation

### Phase 6: Draft Review and Approval
**Goal**: Deliver evidence-backed AI draft generation plus human review, edit, approval, and audit history.
**Mode:** mvp
**Depends on**: Phase 5
**Requirements**: [DRAFT-01, DRAFT-02, DRAFT-03, DRAFT-04, DRAFT-05, DRAFT-06]
**Success Criteria** (what must be TRUE):
  1. The AI service can generate draft responses based on ticket content and retrieved evidence.
  2. Drafts are stored as non-final artifacts with revision history.
  3. Human agents can review, edit, and approve drafts, and unapproved AI output cannot become final.
  4. Approval history is fully traceable by actor and timestamp.
**Plans**: 3 plans

Plans:
- [ ] 06-01: Implement draft generation and artifact persistence model
- [ ] 06-02: Implement review/edit/approval workflow and finalization rules
- [ ] 06-03: Add verification for draft history, approval gates, and human-in-the-loop safety

### Phase 7: Operational Metrics
**Goal**: Expose basic operational visibility over tenant-scoped ticket status, priority, backlog, and response progress.
**Mode:** mvp
**Depends on**: Phase 6
**Requirements**: [MET-01, MET-02, MET-03, MET-04, QUAL-05]
**Success Criteria** (what must be TRUE):
  1. Tenant-scoped metrics endpoints return ticket counts by status and priority distribution.
  2. Backlog and response-progress-related metrics are derived consistently from workflow data.
  3. Verification harnesses confirm implemented metric behavior against seeded workflow scenarios.
**Plans**: 2 plans

Plans:
- [ ] 07-01: Implement tenant-scoped operational metric queries and API endpoints
- [ ] 07-02: Add metric verification scenarios and workflow-to-metric consistency tests

## Post-v1 / v2+

### Post-v1: Authentication and Basic Roles
**Goal**: Add secure authentication and practical role enforcement across the proven support operations workflow.
**Depends on**: Phase 7
**Related Requirements**: [AUTH-01, AUTH-02, AUTH-03]
**Notes**: Not part of v1 completion criteria. In v1, users and roles remain basic tenant-scoped operational metadata or seed data.

### Post-v1: Frontend Operations UI
**Goal**: Build a practical UI for tenant administration, inbox operations, AI review, knowledge management, and metrics on top of stable backend APIs.
**Depends on**: Phase 7 (and typically after Authentication and Basic Roles)
**Related Requirements**: [UI-01, UI-02, UI-03, UI-04, UI-05, UI-06]
**Notes**: v1 is verified through REST APIs, API documentation, tests, and harnesses rather than through a frontend.

### Post-v1: Reliability and Performance
**Goal**: Introduce Redis-backed async processing only where needed and prove retries, timeouts, job inspection, caching, load behavior, and failure recovery.
**Depends on**: Phase 7
**Related Requirements**: [ASYNC-01, ASYNC-02, ASYNC-03]
**Notes**: Redis is not a foundation dependency. It enters only when implemented workflows actually require async orchestration, caching, or job-state inspection.

### Later Product Expansion
**Goal**: Extend the core platform into broader channel support, SaaS workflows, and AI-operational maturity once the v1 and post-v1 core are proven.
**Depends on**: Post-v1 platform maturity
**Related Requirements**: [CHAN-01, CHAN-02, CHAN-03, INTG-01, MLOPS-01, TEN-SELF-01, BILL-01]
**Notes**: Includes inbound email, public web forms, chat or widget channels, external CRM or helpdesk integrations, tenant self-registration, billing or subscriptions, and a fuller MLOps pipeline.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Backend Foundation | 4/4 | Complete | 2026-05-11 |
| 2. Tenant Workflow Core | 1/3 | In Progress | - |
| 3. Knowledge Base Core | 0/2 | Not started | - |
| 4. AI Classification Integration | 0/3 | Not started | - |
| 5. Evidence Retrieval | 0/3 | Not started | - |
| 6. Draft Review and Approval | 0/3 | Not started | - |
| 7. Operational Metrics | 0/2 | Not started | - |
| v1 complete after Phase 7 | - | Planned | - |
