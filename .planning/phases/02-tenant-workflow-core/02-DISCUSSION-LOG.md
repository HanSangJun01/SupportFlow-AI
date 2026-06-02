# Phase 2: Tenant Workflow Core - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-05-13
**Phase:** 2-Tenant Workflow Core
**Areas discussed:** Tenant Metadata Updates, Operational Users And Roles, Ticket Ownership And Prioritization, Workflow History Shape

---

## Tenant Metadata Updates

| Question | Options Presented | User's Choice |
|----------|-------------------|---------------|
| Which tenant fields should Phase 2 allow a super admin to update? | Name/description/status; all fields including slug; metadata only | Name, description, and status. Slug remains stable. |
| If Phase 2 allows updating tenant status, what should the status model mean? | Active/inactive only; active/suspended/archived; display-only status | Active/inactive only. |
| When tenant metadata changes, what history should Phase 2 keep? | No separate tenant history; basic tenant audit fields; full tenant change history | No separate tenant history; update `updatedAt` only. |
| When a tenant is `INACTIVE`, which operations should still work? | Super admin tenant read/update only; read-only tenant-scoped operations; block everything except reactivation | Tenant-scoped reads still work; tenant-scoped mutations are blocked. |

**Notes:** Inactive tenants remain inspectable, including ticket list/detail, but cannot accept new operational changes until reactivated.

---

## Operational Users And Roles

| Question | Options Presented | User's Choice |
|----------|-------------------|---------------|
| Since authentication is post-v1, what should a user mean in Phase 2? | Tenant-local operational profile; lightweight future-auth placeholder; seed-only actor IDs | Tenant-local operational profile. |
| Which roles should Phase 2 support as operational metadata? | Tenant admin and support agent; admin/agent/viewer; freeform role labels | Tenant admin and support agent. |
| What statuses should operational users have? | Active/inactive only; active/inactive/invited; no user status | Active/inactive only. |
| When assigning a ticket or recording an actor, how strict should Phase 2 be? | Validate actor and assignee exist in same tenant; validate assignee only; store supplied IDs without validation | Validate actor and assignee exist in the same tenant. |

**Notes:** Operational users are not authentication accounts. Inactive users remain visible for history but cannot be newly assigned.

---

## Ticket Ownership And Prioritization

| Question | Options Presented | User's Choice |
|----------|-------------------|---------------|
| Which ticket operational fields should Phase 2 allow support operations to update directly? | Assignee/priority/category; assignee/priority/category/urgency; assignee plus all prioritization fields | Assignee, priority, and category. |
| How should these operational fields be updated? | One workflow metadata patch endpoint; separate endpoints per field; extend status endpoint/request | One workflow metadata patch endpoint. |
| When a ticket is assigned, what should be allowed? | Active support agents in same tenant only; active tenant admins or support agents; unassigned and any active tenant user | Assign only active support agents in the same tenant. |
| Should Phase 2 allow ownership/priority/category changes after a ticket is `CLOSED`? | Block operational edits; allow metadata corrections; allow only category changes | Block operational edits on closed tickets. |

**Notes:** Lifecycle status updates and workflow metadata updates should stay separate API operations.

---

## Workflow History Shape

| Question | Options Presented | User's Choice |
|----------|-------------------|---------------|
| Which ticket events must be recorded in immutable history for Phase 2? | Status changes and workflow metadata changes; status changes only; all ticket mutations including creation | Status changes and workflow metadata changes. |
| What should each history entry include? | Event type, actor, timestamp, changed fields, old/new values; event type/actor/timestamp/summary; event type/actor/timestamp only | Event type, actor, timestamp, changed fields, and old/new values. |
| How should actor identity be supplied on workflow operations before authentication exists? | Explicit `actorUserId` in request body; `X-Actor-User-Id` header; optional actor with system fallback | Explicit `actorUserId` in request body. |
| Where should ticket history live and how should APIs expose it? | Embedded in ticket detail response; separate history endpoint only; embedded plus separate endpoint | Embedded in ticket detail response. |

**Notes:** Actor IDs must validate against tenant-local operational users in the same tenant.

---

## the agent's Discretion

- Exact DTO names, path suffixes, Java type names, MongoDB embedded history representation, and test organization are left to downstream research and planning.

## Deferred Ideas

- Tenant slug update workflows.
- Full tenant metadata change history.
- Authentication accounts and real RBAC enforcement.
- Direct user-editable urgency, sentiment, and SLA-risk fields.
- SLA policy automation, escalation, notifications, and scheduling.
- Separate ticket history endpoint unless planning finds it necessary.
