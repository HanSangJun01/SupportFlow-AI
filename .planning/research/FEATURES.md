# Feature Research

**Domain:** Multi-tenant customer support operations platform with AI-assisted handling
**Researched:** 2026-05-07
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Ticket lifecycle and statuses | Support teams expect every inquiry to move through explicit operational states. | MEDIUM | Statuses must be constrained, auditable, and reportable. |
| Unified inbox / queue view | Agents expect to work from a single queue filtered by status, assignee, risk, and priority. | MEDIUM | Backend should model queue filters even before a polished UI exists. |
| Assignment, ownership, and prioritization | Teams need to know who owns a ticket and which tickets are urgent. | MEDIUM | Priority often drives SLA logic and dashboarding. |
| SLA-aware workflow fields | Modern support tools expose response and resolution risk, not just raw status. | MEDIUM | Start with SLA metadata and risk indicators even if automation matures later. |
| Knowledge-backed handling | Agents expect linked FAQ / policy context near the ticket. | MEDIUM | Evidence visibility matters as much as retrieval quality. |
| Audit history | Support operations require a full event trail for accountability and review. | MEDIUM | Include status changes, draft history, approvals, and actor attribution. |
| Operational metrics | Admins expect backlog, status counts, and throughput visibility. | LOW | Start with basic metrics before advanced analytics. |
| Tenant isolation | B2B support platforms must isolate data by workspace or account. | HIGH | This is a trust and correctness requirement, not an enhancement. |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Human-in-the-loop AI drafting with evidence | Demonstrates AI usefulness without sacrificing operational control. | HIGH | The evidence trail must be inspectable before approval. |
| Explicit AI classification + priority recommendation | Makes triage measurable and testable instead of hidden in a black-box chatbot. | HIGH | Store model outputs separately from human overrides. |
| Spec-first, verification-heavy development discipline | Strong portfolio signal; shows engineering process, not just features. | MEDIUM | Should surface in repo structure, tests, and harnesses. |
| Separate operational backend and AI service | Shows clear architectural boundaries and future scalability. | MEDIUM | Also improves failure isolation and clearer testing responsibilities. |
| Failure and performance harnesses | Distinguishes the project from typical CRUD demos. | HIGH | Add after core workflows are stable. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Fully autonomous AI replies | Sounds impressive and reduces manual effort on paper | Unacceptable risk for customer-facing correctness and policy adherence in an MVP | Require human review and approval for every AI-generated response |
| Omnichannel intake from day one | Feels more “real product” | Adds integration noise before ticket domain rules are stable | Start with manual ticket creation and add channels later |
| Deep BI / advanced analytics early | Dashboards look portfolio-friendly | Often delays core operational correctness and clutters the data model | Ship basic operational metrics first |
| Fine-grained permissions early | Seems enterprise-ready | Usually creates role-matrix complexity before workflow is proven | Start with simple roles and expand after core flows stabilize |
| Real-time everything | Feels modern | Adds async and consistency complexity without validating the base loop first | Build deterministic CRUD + async job boundaries first |

## Feature Dependencies

```text
Tenant provisioning
    └──requires──> tenant-isolated data model
                           └──requires──> tenant-aware queries and tests

Ticket workflow
    └──requires──> ticket domain model
                           └──requires──> state transition rules

AI classification
    └──requires──> ticket detail retrieval
    └──requires──> async orchestration boundary

RAG evidence retrieval
    └──requires──> knowledge base ingestion and indexing

AI draft response generation
    └──requires──> classification + evidence retrieval
    └──requires──> human review workflow

Operational dashboards
    └──requires──> durable workflow events and queryable metrics
```

### Dependency Notes

- **Tenant provisioning requires tenant-isolated data modeling:** a tenant can only exist safely if resource ownership is designed into collections and queries from the start.
- **Ticket workflow requires explicit state transition rules:** without valid transition logic, reporting and agent actions become unreliable.
- **AI draft generation requires evidence retrieval and review workflow:** draft generation without grounded context or approval control undermines trust.
- **Dashboards require durable workflow events:** metrics should come from explicit state/history data, not ad hoc controller calculations.

## MVP Definition

### Launch With (v1)

- [ ] Super-admin tenant provisioning — essential to prove controlled multi-tenant operations
- [ ] Tenant-scoped ticket creation, listing, detail, and processing — essential operational backbone
- [ ] Enforced ticket state transitions — essential for correctness and traceability
- [ ] Basic role-aware workspace operations — needed for admin vs support responsibilities
- [ ] Knowledge document management for FAQ / policy content — required for retrieval-backed drafting
- [ ] AI ticket classification — core AI assistance capability
- [ ] RAG evidence retrieval — grounds AI output in tenant knowledge
- [ ] AI draft response generation with visible evidence — core support-assist loop
- [ ] Human review, edit, and approval — mandatory safety boundary
- [ ] Basic operational metrics — required to demonstrate operational visibility
- [ ] Automated tests and harnesses for core rules and isolation — essential portfolio proof

### Add After Validation (v1.x)

- [ ] Authentication and stronger RBAC — add once the tenant and ticket core is stable
- [ ] Frontend inbox and admin UI — add once backend contracts settle
- [ ] Redis-backed async processing improvements — deepen when AI workflows need durable background jobs
- [ ] Monitoring dashboard and performance harness expansion — add as the system becomes more integrated

### Future Consideration (v2+)

- [ ] Email / web form / chat intake channels — defer until the core ticket domain is proven
- [ ] External CRM / helpdesk integrations — defer until internal workflows are stable
- [ ] Advanced analytics and BI reporting — defer until operational event models mature
- [ ] Experiment tracking / prompt versioning / broader MLOps — defer until AI features are in routine use
- [ ] Tenant self-registration and billing — defer until there is a clear SaaS product direction

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Tenant provisioning and isolation | HIGH | HIGH | P1 |
| Ticket CRUD + state machine | HIGH | MEDIUM | P1 |
| Knowledge base management | HIGH | MEDIUM | P1 |
| AI classification | HIGH | HIGH | P1 |
| Evidence retrieval | HIGH | HIGH | P1 |
| Draft generation + approval | HIGH | HIGH | P1 |
| Basic operational metrics | HIGH | LOW | P1 |
| Authentication / RBAC hardening | HIGH | MEDIUM | P2 |
| Frontend UI | MEDIUM | HIGH | P2 |
| Advanced analytics | MEDIUM | HIGH | P3 |
| Omnichannel intake | MEDIUM | HIGH | P3 |
| Full MLOps | MEDIUM | HIGH | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Competitor A | Competitor B | Our Approach |
|---------|--------------|--------------|--------------|
| Ticket statuses and lifecycle | Zendesk has explicit lifecycle categories and closure rules | Intercom manages conversation/ticket workflows through inbox actions and routing | Keep a simpler but explicit state machine that is easy to verify and report on |
| Assignment and queue handling | Intercom emphasizes team inboxes, views, and teammate assignment | Zendesk centers views and business rules around ticket fields and SLAs | Model inbox filters, assignment, and SLA metadata in the backend before UI polish |
| Knowledge-backed assistance | Zendesk and Intercom both surface knowledge near ticket handling | Both support source-backed assistance from help content and history | Make evidence inspection first-class, not hidden behind the generated draft |
| AI drafting support | Competitors offer AI suggestions, macros, and reply assistance | Competitors increasingly blend retrieval and drafting in the workspace | Preserve human approval as a hard boundary and record draft/approval history clearly |

## Sources

- Zendesk ticket lifecycle docs — ticket statuses and lifecycle expectations
- Zendesk SLA policy docs — how priority and SLA rules influence ticket handling
- Intercom inbox and assignment docs — inbox, queue, and ownership expectations
- Intercom Copilot / AI inbox docs — knowledge-backed agent assistance patterns
- Zendesk AI suggestion / auto assist docs — reply suggestion and evidence-assist patterns

---
*Feature research for: multi-tenant support operations AI platform*
*Researched: 2026-05-07*
