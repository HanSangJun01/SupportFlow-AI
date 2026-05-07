# Pitfalls Research

**Domain:** Multi-tenant customer support operations platform with AI-assisted workflows
**Researched:** 2026-05-07
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: Cross-Tenant Data Leakage

**What goes wrong:**
One tenant can read, infer, or mutate another tenant's tickets, documents, drafts, logs, or metrics.

**Why it happens:**
Teams treat tenant filtering as a controller concern instead of a domain invariant, or forget it in secondary queries and dashboards.

**How to avoid:**
Require `tenantId` on every tenant-scoped record, build tenant-aware repository/service contracts, and add automated negative tests for cross-tenant access.

**Warning signs:**
Shared repository methods without tenant parameters, dashboards aggregating all data by default, or tests that only verify happy-path same-tenant access.

**Phase to address:**
Phase 1 foundation and every later phase touching data access.

---

### Pitfall 2: AI Drafts Without Evidence or Approval Control

**What goes wrong:**
Agents receive ungrounded suggestions or AI-generated responses can become customer-facing without sufficient review.

**Why it happens:**
Teams optimize for visible AI output before they model evidence provenance, approval states, and human override paths.

**How to avoid:**
Persist evidence bundles, store model outputs separately from human-edited drafts, and enforce an explicit approval step before finalization.

**Warning signs:**
No source list attached to drafts, no distinction between model text and approved text, or no audit event for approval.

**Phase to address:**
AI service integration, RAG, and review/approval phases.

---

### Pitfall 3: Statuses That Exist But Are Not Enforced

**What goes wrong:**
Tickets have named statuses, but invalid transitions, hidden side effects, and inconsistent history make them operationally meaningless.

**Why it happens:**
CRUD endpoints are built first and state rules are retrofitted later.

**How to avoid:**
Model transition rules explicitly, reject invalid transitions, and record status changes as auditable events.

**Warning signs:**
Endpoints that update status with free-form strings, metrics that do not align with history, or manual patch scripts to fix broken state.

**Phase to address:**
Phase 1 foundation and ticket workflow phases.

---

### Pitfall 4: Analytics Built on Ad Hoc Queries

**What goes wrong:**
Backlog, progress, and priority metrics become slow, inconsistent, or impossible to trust.

**Why it happens:**
Operational events and read models are not designed early; dashboards are treated as a cosmetic feature.

**How to avoid:**
Store workflow history cleanly, define core metrics upfront, and add aggregation-friendly indexes or snapshot strategies.

**Warning signs:**
Controllers composing metrics directly from raw collection scans, disagreement between ticket lists and metric cards, or no definition of metric semantics.

**Phase to address:**
Ticket workflow and operations dashboard phases.

---

### Pitfall 5: Async AI Orchestration Without Failure Semantics

**What goes wrong:**
Classification or draft jobs hang, duplicate, or silently fail, leaving tickets in ambiguous states.

**Why it happens:**
Redis or background processing is added as an implementation detail without explicit job states, retries, timeouts, or idempotency rules.

**How to avoid:**
Define job states, correlation IDs, retry policy, timeout policy, and user-visible failure handling before deep async expansion.

**Warning signs:**
No durable job status, no retry audit trail, or tickets referencing “AI pending” forever.

**Phase to address:**
Redis-based async processing and AI workflow hardening phases.

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hard-coding statuses in controllers | Faster first endpoint | Status drift and invalid transitions | Never once workflow rules matter |
| Storing only the latest draft | Simpler schema | No audit trail or model-vs-human comparison | Never for human-reviewed AI workflows |
| Global admin shortcuts that bypass tenant checks | Faster internal tooling | Highest-risk isolation hole | Never |
| Dashboard metrics from live full scans | Quick demo | Slow, inconsistent operational reporting | Only for a temporary prototype before metrics phase |
| Embedding AI prompts in controller code | Fast first draft | Unversioned, untestable AI behavior | Never beyond a disposable spike |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Spring ↔ FastAPI | Passing loosely defined JSON blobs | Use versioned DTOs / schemas with explicit artifact types |
| MongoDB | Indexing status or assignee without `tenantId` | Build compound indexes starting with `tenantId` for tenant-scoped queries |
| Redis | Treating cache keys and job keys as informal | Standardize namespacing, TTL, and ownership semantics |
| RAG knowledge | Mixing tenant documents in a shared retrieval space without filtering | Apply tenant scoping at ingestion and retrieval time |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Inbox queries without compound indexes | Ticket list latency spikes | Index `tenantId + status + priority + assignee + createdAt` patterns | As ticket counts and tenants grow |
| Recomputing metrics on every request | Slow dashboards and API hotspots | Predefine aggregation queries or snapshots | Once dashboards poll frequently |
| Synchronous AI generation in ticket write path | Slow ticket updates and timeouts | Separate request acceptance from AI processing | As soon as model latency varies materially |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Trusting client-supplied tenant IDs without server-side validation | Cross-tenant access or writes | Resolve and validate tenant context server-side |
| Exposing AI evidence from the wrong tenant | Sensitive policy leakage | Scope retrieval and evidence serialization by tenant |
| Letting approval be optional for convenience | Incorrect customer-facing responses | Make approval state mandatory before finalization |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| AI suggestions without source visibility | Agents do not trust or cannot verify outputs | Show evidence alongside classification and draft suggestions |
| Too many statuses too early | Agents cannot predict workflow behavior | Start with a small explicit lifecycle and expand only if needed |
| Dashboards with vague metrics | Admins cannot act on what they see | Define clear metric semantics and tie them to workflow events |

## "Looks Done But Isn't" Checklist

- [ ] **Tenant isolation:** Often missing negative tests — verify cross-tenant reads and writes fail.
- [ ] **Ticket workflow:** Often missing invalid transition rejection — verify every forbidden status change.
- [ ] **AI drafting:** Often missing evidence provenance — verify draft responses cite retrievable tenant-scoped sources.
- [ ] **Approval flow:** Often missing immutable audit trail — verify who approved what and when.
- [ ] **Metrics:** Often missing metric definitions — verify dashboard counts match ticket history semantics.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Cross-tenant leakage | HIGH | Freeze rollout, identify affected query paths, add regression tests, audit exposed data |
| Unapproved AI response path | HIGH | Disable finalization path, reconstruct history, enforce approval gate in domain layer |
| Broken status model | MEDIUM | Introduce state machine, backfill valid history where possible, add transition tests |
| Unreliable async jobs | MEDIUM | Add job-state persistence, retry visibility, timeout handling, and operator-facing diagnostics |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Cross-tenant data leakage | Phase 1 foundation | Automated negative integration tests across tenants |
| Statuses not enforced | Phase 1 foundation | Unit and integration tests for allowed and denied transitions |
| AI drafts without evidence/approval | AI workflow phases | Contract tests for evidence bundles and approval rules |
| Weak metrics semantics | Metrics / dashboard phase | Cross-check metric outputs against seeded workflow histories |
| Async orchestration ambiguity | Redis / async phase | Harness tests for retries, stuck jobs, and failure surfacing |

## Sources

- Spring and data-store docs for system boundary and persistence failure patterns
- Zendesk support docs for lifecycle, SLA, and assistive workflow expectations
- Intercom inbox and Copilot docs for queueing, assignment, and evidence-backed AI assist patterns
- Project constraints and target workflow from PROJECT.md

---
*Pitfalls research for: multi-tenant support operations AI platform*
*Researched: 2026-05-07*
