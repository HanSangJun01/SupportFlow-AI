# SupportFlow AI

## What This Is

SupportFlow AI is a multi-tenant customer inquiry operations platform built to turn customer support from a manual, experience-dependent workflow into a structured, traceable, data-driven system. It covers the lifecycle of support work across tenant-scoped ticket intake, ticket management, AI classification, evidence retrieval, AI-assisted draft response generation, human review and approval, history tracking, and operational analytics. The platform is intended both as a usable support operations backend and as a portfolio-grade demonstration of multi-tenant architecture, AI-service integration, verification discipline, and reproducible local deployment.

## Core Value

Prove a portfolio-grade support operations platform that demonstrates reliable multi-tenant architecture, strong testing discipline, and human-in-the-loop AI integration without compromising correctness, traceability, or tenant isolation.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Super admins can provision and manage tenant workspaces in a controlled system.
- [ ] Tenant-scoped users can create, list, view, and process customer inquiry tickets inside isolated workspaces.
- [ ] Ticket lifecycle rules are enforced through explicit state transitions such as `NEW`, `TRIAGED`, `IN_PROGRESS`, `ANSWERED`, and `CLOSED`.
- [ ] The system maintains strict tenant isolation across tickets, knowledge documents, response drafts, logs, and operational metrics.
- [ ] AI services can classify tickets, retrieve relevant FAQ or policy evidence, and generate draft responses based on that evidence.
- [ ] Human support agents must review, edit, and approve AI-generated drafts before any response becomes final.
- [ ] The platform exposes basic operational metrics such as ticket count by status, priority distribution, backlog count, and response progress.
- [ ] The full core workflow is verified through automated tests, harnesses, and reproducible local execution.

### Out of Scope

- Tenant self-registration and public signup flows — v1 focuses on controlled super-admin tenant provisioning.
- Inbound email integration — manual ticket intake comes first while the core workflow is stabilized.
- Public web form intake — deferred until the backend workflow and tenant boundaries are proven.
- Live chat or chatbot widget integration — not needed to validate the support operations backbone.
- External CRM or helpdesk integrations — excluded to keep v1 focused on the internal core flow.
- Real customer email sending — response generation and approval matter first; outbound delivery can follow later.
- Billing, subscription, or plan management — v1 is not a complete SaaS commercialization layer.
- Production Kubernetes deployment — local reproducibility matters more than production orchestration in v1.
- Production-grade cloud infrastructure — intentionally deferred beyond the MVP proof point.
- Multi-region deployment — the MVP assumes a single-region operating environment.
- Full MLOps pipeline — tracking and deployment sophistication can follow after core AI workflow validation.
- Model fine-tuning — unnecessary before the base workflow and prompt-driven orchestration are proven.
- Advanced analytics or BI-level reporting — v1 needs operational visibility, not deep reporting breadth.
- Multilingual product support as a formal v1 requirement — not needed for initial workflow validation.
- Fine-grained permission models beyond basic roles — initial access control should remain simple and testable.
- Enterprise-grade compliance features — deferred until after the core platform is proven.
- Mobile app support — outside the scope of the local-first MVP.
- Highly polished UI or UX design — backend correctness and workflow clarity come first.

## Context

SupportFlow AI is intentionally backend-first in its early phases. Phase 1 should establish a stable operational foundation with project structure, Spring Boot backend setup, MongoDB connectivity, tenant management, ticket creation and retrieval APIs, ticket state transition rules, tenant isolation enforcement, and unit/integration tests. Frontend UI should begin only after the backend domain model, tenant boundaries, ticket APIs, and harnesses are stable.

The architecture should separate the operational backend from the AI service. The main backend API is planned in Spring Boot, with Spring Security added in later phases for authentication and authorization. MongoDB stores tenants, users, tickets, knowledge documents, response drafts, logs, and operational data. Redis supports caching and asynchronous job state. The AI service is planned as a separate Python FastAPI service responsible for classification, RAG retrieval, and draft response generation. Local development should be reproducible through Docker Compose in a monorepo.

The initial system should support multiple customer companies in the same platform while strictly isolating each tenant's data. Each company must have separate workspaces, users, roles, tickets, knowledge assets, draft responses, logs, approval history, and metrics. Every tenant-scoped resource should carry a `tenantId`, and every tenant-scoped query should enforce `tenantId` constraints. Automated verification must prove that one tenant cannot access another tenant's resources.

The development process is specification-first and verification-driven. Major features should be defined through SDD-style specifications before implementation, including API contracts, data models, state transitions, validation rules, tenant isolation rules, failure cases, and expected test scenarios. Codex may assist with implementation, but generated code must be verified through tests and harnesses before merge. GitHub workflow expectations include feature branches, atomic commits, and pull requests that clearly show the progression from specification to implementation to verification.

The intended v1 proof point is a controlled local environment where a super admin can create a tenant, support agents can operate isolated tickets through a clear lifecycle, AI services can classify and draft with evidence, humans can review and approve drafts, and the system can expose basic operational visibility.

## Constraints

- **Architecture**: Spring Boot backend and Python FastAPI AI service must remain separate services — preserves clear operational and AI boundaries.
- **Development Model**: Local-first development in a monorepo — the MVP should be easy to run, inspect, and verify on a single machine.
- **Environment**: Docker Compose must provide a reproducible local environment — setup consistency is part of the portfolio goal.
- **Dependency Strategy**: Avoid paid third-party SaaS dependencies where possible — keeps the MVP accessible and reproducible.
- **API Style**: REST-first APIs — favors explicit contracts and testability.
- **Intake Strategy**: Customer inquiry intake is manual in early phases — protects focus on the core workflow.
- **Deployment Assumption**: Single-region environment — avoids premature distributed-systems complexity.
- **Phase 1 Scope**: Phase 1 stays backend-only with tests and API documentation — no UI work until the foundation is stable.
- **UI Sequencing**: Frontend follows backend stabilization — prevents visual work from masking weak domain or isolation design.
- **Tenant Isolation**: Isolation must be enforced from the beginning — this is non-negotiable for correctness.
- **Data Modeling**: Every tenant-scoped resource must include `tenantId` — ensures consistent isolation enforcement across the system.
- **Query Enforcement**: Every tenant-scoped query must include `tenantId` constraints — avoids accidental cross-tenant access.
- **Data Safety**: One tenant must never access another tenant's tickets, knowledge documents, drafts, logs, or metrics — core multi-tenant correctness rule.
- **AI Safety**: AI-generated responses must never become final without human review and approval — preserves human-in-the-loop reliability.
- **Verification Standard**: Generated code must not be accepted blindly — tests and harnesses must verify behavior before merge.
- **Git Workflow**: GitHub feature branches, atomic commits, and pull requests are required — supports traceability from specification to verified implementation.
- **Prioritization**: Correctness, traceability, tenant isolation, and testability take priority over feature completeness — defines how tradeoffs should be resolved.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Phase 1 is backend-only | The backend domain model, APIs, tenant isolation, and tests need to stabilize before UI work begins. | — Pending |
| Manual ticket intake comes first | Early scope should prove core ticket operations before external channels are added. | — Pending |
| Super admins provision tenants in the MVP | Controlled tenant creation reduces scope and keeps early multi-tenant behavior auditable. | — Pending |
| Tenant isolation is designed in from the beginning | Retrofitting isolation later would be risky and undermine the platform's core claim. | — Pending |
| The backend and AI service remain separate services | Clear boundaries improve architecture quality, operability, and future scaling flexibility. | — Pending |
| AI output always requires human approval | The product is a support operations system, not an autonomous response engine. | — Pending |
| The project is specification-first and verification-driven | Portfolio value depends on defensible architecture, test evidence, and traceable delivery. | — Pending |
| v1 proves the end-to-end workflow, not full SaaS completeness | Focus is needed to validate the core operational and AI-assisted support loop. | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `$gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `$gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-07 after initialization*
