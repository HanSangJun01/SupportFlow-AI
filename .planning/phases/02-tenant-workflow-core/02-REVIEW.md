---
phase: 02-tenant-workflow-core
reviewed: 2026-05-13T13:27:30Z
depth: standard
files_reviewed: 28
files_reviewed_list:
  - README.md
  - backend-spring/src/main/java/com/supportflow/tenant/TenantController.java
  - backend-spring/src/main/java/com/supportflow/tenant/TenantService.java
  - backend-spring/src/main/java/com/supportflow/tenant/TenantStatus.java
  - backend-spring/src/main/java/com/supportflow/ticket/Ticket.java
  - backend-spring/src/main/java/com/supportflow/ticket/TicketController.java
  - backend-spring/src/main/java/com/supportflow/ticket/TicketFieldChange.java
  - backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEntry.java
  - backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEventType.java
  - backend-spring/src/main/java/com/supportflow/ticket/TicketService.java
  - backend-spring/src/main/java/com/supportflow/user/OperationalUser.java
  - backend-spring/src/main/java/com/supportflow/user/OperationalUserController.java
  - backend-spring/src/main/java/com/supportflow/user/OperationalUserRepository.java
  - backend-spring/src/main/java/com/supportflow/user/OperationalUserRole.java
  - backend-spring/src/main/java/com/supportflow/user/OperationalUserService.java
  - backend-spring/src/main/java/com/supportflow/user/OperationalUserStatus.java
  - backend-spring/src/test/java/com/supportflow/FoundationVerificationTest.java
  - backend-spring/src/test/java/com/supportflow/SupportFlowApplicationTests.java
  - backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java
  - backend-spring/src/test/java/com/supportflow/tenant/TenantApiIntegrationTest.java
  - backend-spring/src/test/java/com/supportflow/tenant/TenantServiceTest.java
  - backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationIntegrationTest.java
  - backend-spring/src/test/java/com/supportflow/ticket/TenantWorkflowMongoIntegrationTest.java
  - backend-spring/src/test/java/com/supportflow/ticket/TicketApiIntegrationTest.java
  - backend-spring/src/test/java/com/supportflow/ticket/TicketServiceTest.java
  - backend-spring/src/test/java/com/supportflow/user/OperationalUserApiIntegrationTest.java
  - backend-spring/src/test/java/com/supportflow/user/OperationalUserServiceTest.java
  - docs/sdd/phase-02-tenant-workflow-core-api.md
findings:
  critical: 1
  warning: 1
  info: 0
  total: 2
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-05-13T13:27:30Z
**Depth:** standard
**Files Reviewed:** 28
**Status:** issues_found

## Summary

Reviewed the Phase 2 tenant workflow API implementation, tests, README, and SDD. The main correctness risk is that initial ticket creation bypasses the same tenant-local support-agent validation enforced by the workflow update endpoint, allowing invalid or cross-tenant assignee metadata to be stored. Tenant metadata updates also allow blank replacement names, which weakens a field that is required at creation.

## Critical Issues

### CR-01: Ticket creation bypasses tenant-local assignee validation

**Severity:** BLOCKER
**File:** `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java:41`
**Issue:** `createTicket` copies `command.assigneeId()` directly onto the ticket without validating that the id belongs to an active same-tenant `SUPPORT_AGENT`. The workflow update path performs this check before assignment, but the create path can persist a missing, inactive, tenant-admin, or cross-tenant assignee. That breaks tenant isolation and assignment integrity before the first workflow metadata update.
**Fix:**
```java
public Ticket createTicket(String tenantId, CreateTicketCommand command) {
    tenantService.requireActiveTenant(tenantId);
    if (command.assigneeId() != null) {
        operationalUserService.validateActiveSupportAgent(tenantId, command.assigneeId());
    }

    Instant now = Instant.now();
    Ticket ticket = new Ticket();
    ticket.setTenantId(tenantId);
    ticket.setSubject(command.subject());
    ticket.setCustomerName(command.customerName());
    ticket.setCustomerEmail(command.customerEmail());
    ticket.setCustomerMessage(command.customerMessage());
    ticket.setCategory(command.category());
    ticket.setPriority(command.priority());
    ticket.setAssigneeId(command.assigneeId());
    ticket.setStatus(TicketStatus.NEW);
    ticket.setCreatedAt(now);
    ticket.setUpdatedAt(now);
    return ticketRepository.save(ticket);
}
```
Add service and integration coverage for valid same-tenant support-agent assignment at creation and rejection for cross-tenant, inactive, and non-support-agent assignees.

## Warnings

### WR-01: Tenant updates can persist blank names

**Severity:** WARNING
**File:** `backend-spring/src/main/java/com/supportflow/tenant/TenantService.java:45`
**Issue:** Tenant creation requires a non-blank `name`, but `updateTenant` accepts any non-null name and persists it. A request such as `{"name": "   "}` replaces the tenant display name with whitespace, leaving an invalid tenant state through the update path.
**Fix:**
```java
public Tenant updateTenant(String tenantId, UpdateTenantCommand command) {
    Tenant tenant = getTenant(tenantId);
    if (command.name() != null) {
        if (command.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant name must not be blank");
        }
        tenant.setName(command.name());
    }
    if (command.description() != null) {
        tenant.setDescription(command.description());
    }
    if (command.status() != null) {
        tenant.setStatus(command.status());
    }
    tenant.setUpdatedAt(Instant.now());
    return tenantRepository.save(tenant);
}
```
Alternatively, add Bean Validation to `UpdateTenantRequest.name` and annotate the controller request with `@Valid`, then keep an equivalent service-level guard for non-controller callers.

---

_Reviewed: 2026-05-13T13:27:30Z_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
