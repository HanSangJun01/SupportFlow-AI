# Phase 01 Backend Foundation API

This document captures the Phase 1 backend API contract for tenant workspaces and tenant-scoped tickets.

## Tenant Endpoints

### POST /api/v1/tenants

Creates a tenant workspace.

Required request fields:

- `name`
- `slug`

Optional request fields:

- `description`

Response:

- HTTP 201
- Server-generated `id`
- `status` defaults to `ACTIVE`
- `createdAt` and `updatedAt` timestamps

### GET /api/v1/tenants

Lists tenant workspaces.

Response:

- HTTP 200
- Array of tenant workspace summaries

### GET /api/v1/tenants/{tenantId}

Returns a tenant workspace by ID.

Response:

- HTTP 200 when the tenant exists
- HTTP 404 when the tenant does not exist

## Ticket Endpoints

All ticket endpoints are tenant-scoped through the URL path. Phase 1 does not use request headers for tenant identity.

### POST /api/v1/tenants/{tenantId}/tickets

Creates a ticket inside a tenant workspace.

Required request fields:

- `subject`
- `customerName`
- `customerEmail`
- `customerMessage`

Optional request fields:

- `category`
- `priority`
- `assigneeId`

Response:

- HTTP 201
- Server-generated `id`
- Persisted `tenantId`
- `status` defaults to `NEW`

### GET /api/v1/tenants/{tenantId}/tickets

Lists tickets for one tenant workspace.

Supported filters:

- `status`
- `priority`
- `assigneeId`
- `createdFrom`
- `createdTo`

Every list query is constrained by `tenantId`.

### GET /api/v1/tenants/{tenantId}/tickets/{ticketId}

Returns a ticket only when both `tenantId` and `ticketId` match.

Response:

- HTTP 200 when the ticket exists under the requested tenant
- HTTP 404 when the ticket does not exist or belongs to another tenant

### PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/status

Updates a tenant-scoped ticket status.

Request body:

```json
{ "status": "TRIAGED" }
```

Allowed transitions:

- `NEW -> TRIAGED`
- `TRIAGED -> IN_PROGRESS`
- `IN_PROGRESS -> ANSWERED`
- `ANSWERED -> CLOSED`
- `ANSWERED -> IN_PROGRESS`

Rejected transitions include:

- Any transition from `CLOSED`
- `NEW -> ANSWERED`
- `NEW -> CLOSED`
- `TRIAGED -> ANSWERED`
- `IN_PROGRESS -> CLOSED`

Response:

- HTTP 200 when the transition is allowed
- HTTP 400 when the transition is invalid
- HTTP 404 when the ticket does not exist under the requested tenant

## Deferred Phase 2 Items

- Tenant metadata update workflows
- Operational user and role metadata
- Ticket ownership update workflows
- Actor-attributed ticket history
- Category, urgency, sentiment, and SLA-risk enrichment
- Authentication and role enforcement
