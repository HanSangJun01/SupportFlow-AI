# Phase 02 Tenant Workflow Core API

This document captures the Phase 2 backend API contract for tenant metadata updates, tenant-local operational users, actor-attributed ticket status changes, workflow metadata updates, and embedded ticket history.

Phase 2 keeps tenant identity in URL paths and does not add authentication headers or auth claims. Operational users are workflow metadata records only.

## Tenant Endpoints

### PATCH /api/v1/tenants/{tenantId}

Updates mutable tenant workspace metadata.

Request fields:

- `name`: optional replacement display name.
- `description`: optional replacement description.
- `status`: optional `ACTIVE` or `INACTIVE`.

Response:

- HTTP 200 with the updated tenant.
- HTTP 404 when the tenant does not exist.

The tenant `slug` is immutable and is not accepted by this request.

## Operational User Endpoints

Operational users are tenant-local metadata used for assignment and history attribution. They are not login accounts.

### POST /api/v1/tenants/{tenantId}/users

Creates an operational user inside one tenant.

Request fields:

- `displayName`: required.
- `email`: required email.
- `role`: required `TENANT_ADMIN` or `SUPPORT_AGENT`.

Response:

- HTTP 201 with `id`, `tenantId`, `displayName`, `email`, `role`, `status`, `createdAt`, and `updatedAt`.
- New users default to `ACTIVE`.

### GET /api/v1/tenants/{tenantId}/users

Lists operational users for one tenant.

### GET /api/v1/tenants/{tenantId}/users/{userId}

Returns a user only when both `tenantId` and `userId` match.

### PATCH /api/v1/tenants/{tenantId}/users/{userId}/status

Updates a tenant-local operational user status.

Request fields:

- `status`: required `ACTIVE` or `INACTIVE`.

Inactive users remain readable for historical references, but cannot be used as new workflow actors or assignees.

## Ticket Workflow Endpoints

### PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/status

Updates a tenant-scoped ticket lifecycle status.

Request fields:

- `status`: required target status.
- `actorUserId`: required tenant-local operational user id for attribution.

Validation:

- The tenant must be `ACTIVE`.
- The ticket must exist under the requested tenant.
- `actorUserId` must identify an `ACTIVE` operational user in the same tenant.
- The status transition must be allowed by the ticket lifecycle policy.

Successful status updates append a `STATUS_CHANGED` history entry.

### PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/workflow

Updates ticket workflow metadata without changing lifecycle status.

Request fields:

- `actorUserId`: required tenant-local operational user id for attribution.
- `assigneeId`: optional operational user id. When supplied, it must be an `ACTIVE` same-tenant `SUPPORT_AGENT`.
- `priority`: optional priority value.
- `category`: optional category string.

Validation:

- The tenant must be `ACTIVE`.
- The ticket must exist under the requested tenant.
- Closed tickets reject workflow metadata edits.
- Cross-tenant actors and assignees are rejected without mutating the ticket.
- Inactive users and non-`SUPPORT_AGENT` assignees are rejected.

Successful metadata updates append a `WORKFLOW_METADATA_CHANGED` history entry. Supplied values that match the current ticket do not create a new history entry.

## Ticket History Response Shape

Ticket detail and list responses include embedded chronological history entries:

```json
{
  "history": [
    {
      "eventType": "WORKFLOW_METADATA_CHANGED",
      "actorUserId": "tenant-local-user-id",
      "occurredAt": "2026-05-13T00:00:00Z",
      "changes": [
        {
          "field": "assigneeId",
          "oldValue": null,
          "newValue": "support-agent-id"
        }
      ]
    }
  ]
}
```

History event types:

- `STATUS_CHANGED`
- `WORKFLOW_METADATA_CHANGED`

Each change records the changed field plus string-form old and new values.

## Inactive Tenant Behavior

Inactive tenants remain inspectable:

- Tenant list/detail/update is available.
- Ticket list/detail is available.
- Operational user list/detail/status update is available.

Inactive tenants reject tenant-scoped ticket mutations:

- Ticket create.
- Ticket status update.
- Ticket workflow metadata update.

## Phase 2 Exclusions

Phase 2 intentionally does not add authentication, login credentials, sessions, auth claims, or full RBAC enforcement.

No SLA policy, escalation, notification, scheduling, urgency automation, sentiment automation, or SLA-risk automation is added in Phase 2. Priority and category remain explicit workflow metadata fields; later AI phases can add classification and enrichment artifacts.
