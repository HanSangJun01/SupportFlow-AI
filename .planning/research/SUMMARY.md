# Project Research Summary

**Project:** SupportFlow AI
**Domain:** Multi-tenant customer support operations platform with AI-assisted handling
**Researched:** 2026-05-07
**Confidence:** HIGH

## Executive Summary

SupportFlow AI fits the pattern of a modern support operations platform more than a chatbot product. The research points toward a backend-centered system with explicit ticket workflow, queue/assignment semantics, knowledge-backed handling, audit history, and operational metrics. The recommended implementation approach is to treat AI as an assistive subsystem that produces classification, evidence, and draft artifacts, while the operational backend remains the system of record and enforces approval before any response becomes final.

The strongest architecture for this project is a split-service model: Spring Boot for tenant-aware operational workflows and FastAPI for AI tasks such as classification, retrieval, and drafting. This aligns with the project's portfolio goal because it demonstrates system boundaries, testability, and a realistic human-in-the-loop control plane instead of a simplistic prompt wrapper.

The key risk is not model quality first; it is correctness around tenant isolation, workflow enforcement, and evidence-backed review. If those are designed well, the AI features can mature safely. If they are weak, the platform looks complete in demos but fails the core architectural claim.

## Key Findings

### Recommended Stack

The best-fit stack is conservative where the foundation matters and flexible where experimentation matters. Spring Boot 3.5.13 is the safer MVP baseline than Boot 4 because it keeps the ecosystem stable while still current. MongoDB remains a good operational store for tenant-scoped support objects, Redis is enough for cache and async job state, and a separate FastAPI service gives the AI layer room to evolve independently.

**Core technologies:**
- Spring Boot 3.5.13: operational backend and domain rules — stable current line with strong ecosystem support
- MongoDB 8.2.x: operational document storage — fits tickets, knowledge docs, drafts, approvals, and logs
- Redis 8.2.x: cache and async orchestration state — good fit for transient workflow coordination
- FastAPI 0.135.3: AI service API — strong typed API ergonomics for classification, retrieval, and drafting

### Expected Features

The research confirms that ticket lifecycle, inbox semantics, assignment, priority/SLA visibility, knowledge-backed handling, audit history, and metrics are all table stakes for this kind of product. The differentiator is not “AI exists,” but that AI assistance is grounded, reviewable, and embedded into the operational workflow without bypassing human control.

**Must have (table stakes):**
- Explicit ticket lifecycle and state transitions — users expect operational clarity
- Tenant-scoped ticket queues, assignment, and prioritization — core support workflow
- Knowledge-backed handling and audit history — required for trust and traceability
- Basic operational metrics — admins expect backlog and progress visibility

**Should have (competitive):**
- AI classification and priority recommendation — makes triage measurable
- Evidence-backed draft response generation — demonstrates practical AI assistance
- Human review and approval over AI drafts — strong safety and trust signal

**Defer (v2+):**
- Omnichannel intake, advanced analytics, tenant self-registration, and full MLOps — important eventually, but not necessary to prove the core loop

### Architecture Approach

The major architectural recommendation is to keep tenant-aware operational control in Spring Boot and keep model-driven behavior in a separate FastAPI service. MongoDB should remain the source of truth for workflow and history, while Redis should stay limited to cache and async job-state responsibilities. AI outputs should be modeled as artifacts attached to tickets, not as authoritative state replacements.

**Major components:**
1. Operational backend — owns tenants, tickets, workflow rules, knowledge metadata, drafts, approvals, metrics, and audit history
2. AI service — owns classification, evidence retrieval, and draft generation
3. Data and orchestration layer — MongoDB for durable records, Redis for transient async coordination

### Critical Pitfalls

1. **Cross-tenant data leakage** — avoid by enforcing tenant-aware service/repository boundaries and negative integration tests
2. **AI drafts without evidence or approval control** — avoid by persisting evidence bundles and mandatory approval state
3. **Statuses that exist but are not enforced** — avoid by modeling an explicit state machine and rejecting invalid transitions
4. **Metrics built on ad hoc queries** — avoid by defining metric semantics and workflow history early
5. **Async AI orchestration without failure semantics** — avoid by defining job states, retries, and timeout behavior before scaling async processing

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Backend Foundation and Tenant Isolation
**Rationale:** Everything depends on correct tenant-aware persistence and workflow primitives.
**Delivers:** Monorepo structure, Spring backend setup, MongoDB integration, tenant provisioning, ticket CRUD baseline, state transitions, tenant-aware tests.
**Addresses:** Tenant isolation, ticket lifecycle, audit-ready workflow base.
**Avoids:** Cross-tenant leakage and fake-status anti-patterns.

### Phase 2: Operational Workflow Hardening
**Rationale:** Queue semantics, assignment, and history must stabilize before AI features attach to them.
**Delivers:** Assignee/priority/SLA metadata, audit history, validation rules, API docs, stronger integration coverage.
**Uses:** Spring data patterns and Mongo indexing.
**Implements:** Ticket workflow and operational history components.

### Phase 3: Knowledge Base and Retrieval Readiness
**Rationale:** RAG needs a clean tenant-scoped knowledge domain before retrieval or drafting can be trusted.
**Delivers:** FAQ/policy document management, retrieval metadata model, tenant-aware knowledge APIs.

### Phase 4: AI Classification Service
**Rationale:** Classification is the safest first AI attachment point and helps establish inter-service contracts.
**Delivers:** FastAPI service scaffold, ticket classification endpoint, stored AI analysis artifacts, backend integration.

### Phase 5: Evidence Retrieval and RAG Flow
**Rationale:** Evidence must exist before drafts can be grounded and reviewed intelligently.
**Delivers:** Retrieval pipeline, evidence bundle contracts, tenant-aware retrieval tests, failure behavior.

### Phase 6: Draft Generation and Human Review
**Rationale:** This is the core human-in-the-loop value loop once evidence is trustworthy.
**Delivers:** AI draft generation, draft revision history, human edit/review/approval workflow, finalization rules.

### Phase 7: Authentication and Role Enforcement
**Rationale:** Once the core loop exists, access controls can be integrated against real workflow behavior.
**Delivers:** Spring Security auth flows, basic roles, protected tenant-aware endpoints.

### Phase 8: Frontend Operational UI
**Rationale:** UI should consume stable backend contracts, not define them prematurely.
**Delivers:** Admin workspace screen, ticket inbox, ticket detail/AI review, knowledge management surface.

### Phase 9: Operations Dashboard and Async Processing
**Rationale:** Metrics and async orchestration become more meaningful after real AI workflow artifacts exist.
**Delivers:** Dashboard endpoints/views, Redis-backed job-state handling, monitoring surfaces.

### Phase 10: Verification, Performance, and Failure Harnesses
**Rationale:** The portfolio claim is strongest when correctness and resilience are demonstrated explicitly.
**Delivers:** Load tests, failure recovery harnesses, end-to-end verification, system-quality evidence.

### Phase Ordering Rationale

- Tenant isolation and ticket workflow must come before AI, because every AI artifact is tenant-scoped and workflow-bound.
- Knowledge retrieval must come before draft generation, because evidence-backed assistance is part of the product claim.
- UI comes after backend stabilization to avoid optimizing presentation while the domain is still moving.
- Performance and failure harnesses should validate the integrated system, not just isolated CRUD endpoints.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 4:** AI classification service — model/provider contract details and evaluation strategy need deeper planning
- **Phase 5:** RAG retrieval — document chunking, indexing, and retrieval quality heuristics need focused research
- **Phase 9:** Async processing and metrics — retry semantics and aggregation strategy need deliberate design
- **Phase 10:** Performance and failure harnesses — scenario selection and measurable thresholds need explicit planning

Phases with standard patterns (skip research-phase):
- **Phase 1:** Spring Boot + MongoDB foundation — well-established and highly documented
- **Phase 2:** Ticket workflow hardening — standard domain and API discipline patterns
- **Phase 8:** Frontend operational UI consuming stable APIs — standard application-layer work once contracts exist

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Versions and ecosystem direction were verified against primary docs |
| Features | HIGH | Support-platform expectations are well established across official vendor docs |
| Architecture | HIGH | Strong alignment between user goals and known multi-service support-platform patterns |
| Pitfalls | HIGH | Risks are consistent with the project's domain and architecture choices |

**Overall confidence:** HIGH

### Gaps to Address

- AI model/provider selection specifics: decide during phase planning based on evaluation and cost needs.
- Retrieval implementation details: finalize chunking, indexing, and evidence formatting once knowledge documents are modeled.
- Auth model depth: basic roles are clear, but the exact permission boundary design should wait until core flows are implemented.

## Sources

### Primary (HIGH confidence)
- https://docs.spring.io/spring-boot/index.html — current Spring Boot stable lines
- https://docs.spring.io/spring-security/reference/index.html — current Spring Security stable lines
- https://www.mongodb.com/try/download/community — current MongoDB release line
- https://redis.io/docs/latest/operate/oss_and_stack/install/version-mgmt/ — supported Redis lines
- https://pypi.org/project/fastapi/ — current FastAPI release line
- https://docs.pytest.org/en/latest/ — pytest docs
- https://java.testcontainers.org/ — Testcontainers for Java
- https://playwright.dev/docs/release-notes — Playwright current release line

### Secondary (MEDIUM confidence)
- https://support.zendesk.com/hc/en-us/articles/8263915942938-About-the-ticket-lifecycle-and-ticket-statuses — lifecycle expectations
- https://support.zendesk.com/hc/en-us/articles/5600997516058-About-SLA-policies-and-how-they-work — SLA/priority semantics
- https://www.intercom.com/help/en/articles/6274899-get-started-with-intercom-inbox — inbox setup expectations
- https://www.intercom.com/help/en/articles/6561699-assign-conversations-to-teammates-and-teams-in-the-next-gen-inbox — assignment patterns
- https://www.intercom.com/help/en/articles/8587194-how-to-use-copilot — evidence-backed AI assistance patterns

---
*Research completed: 2026-05-07*
*Ready for roadmap: yes*
