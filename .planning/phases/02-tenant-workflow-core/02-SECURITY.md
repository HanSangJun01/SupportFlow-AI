---
phase: 02
slug: tenant-workflow-core
status: verified
threats_open: 0
asvs_level: 1
created: 2026-06-02
verified: 2026-06-02
---

# Phase 02 - Security

Per-phase security contract for Phase 02: Tenant Workflow Core.

This report verifies the threat models authored in `02-01-PLAN.md`, `02-02-PLAN.md`, and `02-03-PLAN.md` against the completed implementation, tests, and Phase 2 API contract.

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Tenant route boundary | Tenant-scoped REST paths carry `tenantId` and route all tenant-owned resources through service-layer tenant checks. | Tenant ids, ticket ids, operational user ids, ticket workflow metadata |
| Operational user metadata boundary | Operational users are tenant-local workflow metadata only, not authentication identities. | Display name, email, role, status, actor/assignee ids |
| Ticket workflow mutation boundary | Ticket create, status, and workflow mutation paths enforce active-tenant and tenant-local user validation before persistence. | Ticket status, assignee id, priority, category, history entries |
| Ticket history boundary | Ticket responses expose embedded history for traceability without adding auth claims or session-derived actor identity. | Event type, actor user id, timestamp, field-level old/new values |
| Documentation boundary | Phase 2 API docs describe backend contract fields and exclusions only. | Public API field names, route behavior, validation behavior |

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-02-01-01 | Tampering | Tenant metadata update API | mitigate | `UpdateTenantRequest` and `UpdateTenantCommand` include only `name`, `description`, and `status`; tenant slug is not accepted on update. Evidence: `TenantController.UpdateTenantRequest`, `TenantService.UpdateTenantCommand`. | closed |
| T-02-01-02 | Information Disclosure | Operational user tenant boundary | mitigate | Route-backed user detail and validation use `findByTenantIdAndId(tenantId, userId)`, returning 404 for cross-tenant ids. Evidence: `OperationalUserRepository.findByTenantIdAndId`, `OperationalUserService.findTenantUser`. | closed |
| T-02-01-03 | Elevation of Privilege | Operational role metadata | mitigate | Roles remain workflow metadata only. No passwords, credentials, sessions, Spring Security, auth claims, or RBAC enforcement were added; the Phase 2 contract documents this exclusion. | closed |
| T-02-01-04 | Tampering | Inactive tenant mutation policy | mitigate | `TenantService.requireActiveTenant` rejects inactive tenants, and ticket create/status/workflow mutation paths call it before writes. Evidence: `TenantService.requireActiveTenant`, `TicketService.createTicket`, `TicketService.updateStatus`, `TicketService.updateWorkflowMetadata`. | closed |
| T-02-02-01 | Spoofing | `actorUserId` request body | mitigate | Status and workflow mutations validate `actorUserId` as an active same-tenant operational user before mutating state or appending history. Evidence: `TicketService.updateStatus`, `TicketService.updateWorkflowMetadata`, `OperationalUserService.validateActiveActor`. | closed |
| T-02-02-02 | Tampering | Ticket workflow metadata update | mitigate | Workflow updates are limited to `assigneeId`, `priority`, and `category`; closed tickets and inactive tenants reject workflow edits. Evidence: `TicketService.UpdateWorkflowMetadataCommand`, `TicketController.UpdateTicketWorkflowRequest`, `TicketService.updateWorkflowMetadata`. | closed |
| T-02-02-03 | Information Disclosure | Cross-tenant actor and assignee ids | mitigate | Actor and assignee validation uses tenant-scoped operational user lookup; Mongo-backed HTTP tests assert cross-tenant actor/assignee attempts fail without mutation. Evidence: `OperationalUserService.findTenantUser`, `TenantWorkflowMongoIntegrationTest`. | closed |
| T-02-02-04 | Repudiation | Ticket status and metadata changes | mitigate | Status and workflow changes append history entries with event type, actor id, timestamp, field name, old value, and new value. Evidence: `TicketHistoryEntry`, `TicketFieldChange`, `TicketService.updateStatus`, `TicketService.updateWorkflowMetadata`. | closed |
| T-02-03-01 | Information Disclosure | Cross-tenant workflow integration | mitigate | Mongo-backed HTTP tests prove cross-tenant ticket/user/actor/assignee attempts return denial statuses without leaking or mutating another tenant's data. Evidence: `TenantWorkflowMongoIntegrationTest.crossTenantActorFailsStatusAndWorkflowUpdatesWithoutMutation`, `invalidAssigneeRoleStatusOrTenantFailsWorkflowUpdateWithoutMutation`, `ticketCreationValidatesTenantLocalSupportAgentAssignee`. | closed |
| T-02-03-02 | Repudiation | Workflow verification evidence | mitigate | HTTP tests verify workflow history response fields, and the Phase 2 SDD documents history event shape and supported event types. Evidence: `TenantWorkflowMongoIntegrationTest.sameTenantSupportAgentAssignmentAppendsWorkflowHistory`, `docs/sdd/phase-02-tenant-workflow-core-api.md`. | closed |
| T-02-03-03 | Tampering | Inactive tenant mutation behavior | mitigate | Mongo-backed HTTP tests prove inactive tenants allow ticket reads but reject ticket create, status update, and workflow metadata update. Evidence: `TenantWorkflowMongoIntegrationTest.inactiveTenantAllowsReadsButRejectsTicketWorkflowMutations`. | closed |
| T-02-03-04 | Information Disclosure | API documentation | mitigate | Phase 2 API docs describe contract fields and exclusions only; scan found auth/password/session/credential terms only in explicit exclusions. No MongoDB connection details or secrets are documented. | closed |

Status: all plan-time threats are closed.

## Accepted Risks Log

No accepted risks.

## Verification Evidence

| Check | Result |
|-------|--------|
| Plan threat model coverage | Three Phase 2 plan files contain formal `<threat_model>` blocks. |
| Summary threat flags | `02-03-SUMMARY.md` reports no threat flags. |
| Implementation evidence | Tenant, operational user, and ticket services contain the planned tenant isolation, actor validation, active-tenant, and history mitigations. |
| Integration evidence | Mongo-backed HTTP tests cover inactive-tenant mutation denial, cross-tenant actor denial, cross-tenant assignee denial, invalid assignee denial, and workflow history responses. |
| Contract evidence | Phase 2 SDD documents actor attribution, inactive tenant behavior, history response shape, and explicit exclusions for auth/RBAC/SLA/AI automation scope. |
| User-reported final verification | `./mvnw verify` passed with 0 failures and 0 skipped after Docker-backed integration test fixes. |

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-06-02 | 12 | 12 | 0 | Codex |

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

Approval: verified 2026-06-02
