# Phase 4: AI Classification Integration - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-06-08
**Phase:** 04-ai-classification-integration
**Areas discussed:** Classification Contract, Invocation Flow, Artifact Storage And Review, Local AI Behavior, Failure And Timeout Behavior

---

## Classification Contract

### Core Payload

| Option | Description | Selected |
|--------|-------------|----------|
| Operational fields only | Return `category`, `urgency`, `sentiment`, `priority`, plus confidence. | Yes |
| Fields plus explanation | Include a short human-readable explanation. | |
| Full audit envelope | Include detailed metadata in the AI response. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Operational fields only.
**Notes:** Phase 4 keeps classification output focused on operational metadata.

### Value Sets

| Option | Description | Selected |
|--------|-------------|----------|
| Small fixed enums | Fixed enums for urgency, sentiment, and priority; simple strings for category. | Yes |
| Free-form labels | Let the AI service return flexible labels. | |
| Hybrid | Mix fixed and flexible labels. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Small fixed enums for urgency, sentiment, and priority, with category strings.
**Notes:** Values must remain contract-testable.

### Confidence

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, required 0.0-1.0 | Every successful classification includes numeric confidence. | Yes |
| Yes, optional | Confidence may be omitted. | |
| No confidence in Phase 4 | Defer confidence. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Required numeric confidence.
**Notes:** Confidence is part of the testable contract.

### Rationale

| Option | Description | Selected |
|--------|-------------|----------|
| No, defer rationale | No human-readable explanation in Phase 4. | Yes |
| Yes, short rationale | Add a concise explanation. | |
| Only debug-only rationale in tests/docs | Keep rationale outside runtime contract. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Defer rationale.
**Notes:** Rationale is recorded as a deferred idea for later AI phases.

---

## Invocation Flow

### Trigger

| Option | Description | Selected |
|--------|-------------|----------|
| Manual backend endpoint only | Classification only runs when explicitly requested. | |
| Automatically on ticket creation | Ticket create triggers classification. | |
| Both manual and automatic | Create triggers classification and users can re-analyze. | Yes |
| You decide | Leave the choice to planning. | |

**User's choice:** Both automatic classification on ticket creation and manual backend re-analysis.
**Notes:** The user later reinforced that automatic classification should reduce manual operational cost.

### Automatic Failure On Create

| Option | Description | Selected |
|--------|-------------|----------|
| Ticket still creates, classification records failure | Preserve ticket creation and record the failed attempt. | Yes |
| Ticket creation fails | Reject the create request. | |
| Ticket creates with no artifact | Ignore classification failure. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Ticket creation succeeds and records a failed classification attempt.
**Notes:** Failed classification must not block intake.

### Re-Run

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, manual re-analysis allowed | Existing tickets can be re-classified. | Yes |
| Only retry failed attempts | Only failed attempts can be retried. | |
| No re-analysis in Phase 4 | Do not add re-analysis. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Manual re-analysis is allowed.
**Notes:** Re-analysis supports correcting bad AI classifications.

### Allowed States

| Option | Description | Selected |
|--------|-------------|----------|
| Any non-closed ticket | Allow classification unless the ticket is closed. | Yes |
| Only NEW/TRIAGED tickets | Restrict to early lifecycle states. | |
| Any ticket including CLOSED | Allow classification on closed tickets. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Any non-closed ticket.
**Notes:** Aligns with Phase 2 closed-ticket mutation rules.

---

## Artifact Storage And Review

### Storage

| Option | Description | Selected |
|--------|-------------|----------|
| Append-only history | Store every classification attempt. | Yes |
| Latest only | Keep only the newest classification. | |
| Latest plus compact previous status | Keep compact previous attempt information. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Append-only history.
**Notes:** Attempts should remain traceable over time.

### Visibility

| Option | Description | Selected |
|--------|-------------|----------|
| Embedded in ticket detail | Include artifacts in ticket detail responses. | Yes |
| Separate classification endpoint only | Fetch artifacts separately. | |
| Both embedded and separate endpoint | Support both access paths. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Embedded in ticket detail.
**Notes:** Operational consumers should inspect classification artifacts alongside tickets.

### Attempt Fields

| Option | Description | Selected |
|--------|-------------|----------|
| Status plus result/error | Store status, trigger, actor if manual, timestamps, success result, and failure error. | Yes |
| Only success result fields | Store only successful outputs. | |
| Full request/response snapshot | Persist detailed AI request and response bodies. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Status plus result/error.
**Notes:** The final context also includes classifier version from the Local AI Behavior decision.

### Auto-Apply Category And Priority

| Option | Description | Selected |
|--------|-------------|----------|
| No, keep as reviewable artifacts first | Do not update ticket fields automatically. | |
| Yes, overwrite category/priority automatically | Apply successful classification to ticket fields. | Yes |
| Only fill empty category/priority | Apply only when fields are missing. | |
| User-provided revision | Auto-apply while preserving append-only attempts and workflow traceability. | Yes |

**User's choice:** Successful AI predictions automatically apply ticket category and priority.
**Notes:** The user revised the earlier artifact decision to support operational cost reduction. Failed attempts do not change ticket fields. Re-analysis creates a new attempt and applies category/priority again if successful.

### Traceability

| Option | Description | Selected |
|--------|-------------|----------|
| User-provided revision | Store attempt, apply category/priority, record workflow history, and link to `classificationAttemptId`. | Yes |

**User's choice:** Link AI-applied workflow history to the classification attempt.
**Notes:** AI auto-apply must remain auditable.

### Manual Actor

| Option | Description | Selected |
|--------|-------------|----------|
| Require actorUserId | Manual re-analysis is actor-attributed. | Yes |
| Actor optional | Allow missing actor. | |
| No actor on AI classification | Do not track actor. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Require `actorUserId` for manual re-analysis.
**Notes:** Aligns with Phase 2/3 mutation attribution.

### Automatic Actor

| Option | Description | Selected |
|--------|-------------|----------|
| No human actor; use trigger metadata | Use metadata such as `AUTO_ON_CREATE`. | Yes |
| Use a reserved system actor id | Store a synthetic system actor. | |
| Use ticket creator when available | Attribute to ticket creator. | |
| You decide | Leave the choice to planning. | |

**User's choice:** No human actor for automatic classification.
**Notes:** Do not invent a fake system user.

### History Event Type

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, new AI classification event type | Distinguish AI-applied changes from human workflow edits. | Yes |
| Reuse workflow metadata changed | Use the existing event type. | |
| Use status/history note only | Avoid a new event type. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Use a new AI classification history event type.
**Notes:** Workflow history should distinguish AI-applied updates.

---

## Local AI Behavior

### Classifier Type

| Option | Description | Selected |
|--------|-------------|----------|
| Deterministic rule-based classifier | Predictable local behavior, stable tests, no provider keys. | Yes |
| Stub fixture responses only | Simplest contract proof, less realistic. | |
| Real LLM/provider adapter | More production-like, but adds secrets, network, flakiness, and cost. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Deterministic rule-based classifier.
**Notes:** Local AI behavior should be reproducible.

### Mapping Rules

| Option | Description | Selected |
|--------|-------------|----------|
| Keyword rules over subject/message | Transparent and easy to test. | Yes |
| Static default unless explicit test hints are present | Very stable but less realistic. | |
| Lightweight scoring rules across category, urgency, and sentiment | Richer but more maintenance. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Keyword rules over subject and message.
**Notes:** Planner should keep rules transparent and fixture-friendly.

### Versioning

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, include modelVersion or classifierVersion | Trace artifacts as rules evolve. | Yes |
| No, keep Phase 4 response minimal | Simpler contract. | |
| Only include it in AI service response, not stored backend artifacts | Partial traceability. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Include classifier/model version in responses and stored attempts.
**Notes:** Versioning supports later auditability.

### Confidence Behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Rule-derived confidence bands | Strong matches get higher confidence; fallback gets lower confidence. | Yes |
| Fixed confidence for all successful classifications | Simpler but less informative. | |
| Confidence only reflects category | Narrower confidence meaning. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Rule-derived confidence bands.
**Notes:** Confidence should distinguish strong rules from fallback classifications.

---

## Failure And Timeout Behavior

### Manual Failure

| Option | Description | Selected |
|--------|-------------|----------|
| Return structured failure and store failed attempt | Caller sees failure, ticket remains unchanged. | Yes |
| Return success with previous classification unchanged | Hides failed AI attempt. | |
| Retry synchronously before failing | Can slow API responses. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Return structured failure and store a failed attempt.
**Notes:** Manual failure must be visible and must not change the ticket.

### Create Response Timing

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, run classification synchronously during create | Simple Phase 4 flow with immediately visible artifacts. | Yes |
| No, classify asynchronously later | More production-like but adds job state and async complexity. | |
| Try synchronous classification, omit artifacts until detail reload | Simpler response but less immediate visibility. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Run classification synchronously during ticket creation.
**Notes:** Create response can include a successful or failed classification artifact.

### Timeout Budget

| Option | Description | Selected |
|--------|-------------|----------|
| Short local timeout, around 2 seconds | Keeps create and re-analysis responsive. | Yes |
| Moderate timeout, around 5 seconds | More tolerant but slower failures. | |
| Configurable only, no fixed default preference | No default preference. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Short local timeout around 2 seconds.
**Notes:** Timeout should be configurable but have a short local default.

### Failed Attempt History

| Option | Description | Selected |
|--------|-------------|----------|
| Store failed attempt only, no workflow history update | History records applied workflow changes; artifact records failure. | Yes |
| Add a workflow history entry for every failure | More audit visibility but may clutter history. | |
| Add workflow history only for manual failures | Partial history visibility. | |
| You decide | Leave the choice to planning. | |

**User's choice:** Failed attempts do not create workflow history entries.
**Notes:** Workflow history is reserved for successful AI-applied category/priority updates and other applied workflow changes.

---

## the agent's Discretion

- Exact naming, package layout, enum value spelling, endpoint suffixes, persistence shape, and test class organization were left to downstream research and planning.
- The planner may choose whether classification attempts are embedded in tickets or stored separately, provided ticket detail embeds artifacts and tenant isolation is proven.

## Deferred Ideas

- Human-readable AI rationale can be revisited in later evidence or draft-generation phases.
- Real LLM/provider-backed classification is deferred.
- Async retry jobs, queue inspection, and Redis-backed orchestration are deferred.
- Evidence retrieval and draft generation remain later phase scope.
