---
phase: 04-ai-classification-integration
plan: 04-01
subsystem: ai-service
tags: [fastapi, pydantic, pytest, deterministic-classifier, docker]

requires:
  - phase: 04-ai-classification-integration
    provides: Phase 4 AI-SPEC and deterministic classification contract
provides:
  - FastAPI AI service scaffold under ai-service-python
  - strict Pydantic request and response models for ticket classification
  - deterministic keyword classifier returning category, urgency, sentiment, priority, confidence, and classifierVersion
  - pytest contract, OpenAPI, runtime, and reference dataset coverage
affects: [ai-classification-integration, backend-ai-client, docker-compose, api-contracts]

tech-stack:
  added: [fastapi, pydantic, pytest, httpx]
  patterns: [stateless FastAPI service, strict Pydantic contracts, pure deterministic classifier, provider-free runtime checks]

key-files:
  created:
    - ai-service-python/pyproject.toml
    - ai-service-python/Dockerfile
    - ai-service-python/app/__init__.py
    - ai-service-python/app/main.py
    - ai-service-python/app/models.py
    - ai-service-python/app/classifier.py
    - ai-service-python/app/settings.py
    - ai-service-python/tests/test_classifier_rules.py
    - ai-service-python/tests/test_classification_api.py
    - ai-service-python/tests/test_runtime_contract.py
  modified: []

key-decisions:
  - "The AI service is a stateless FastAPI boundary with strict Pydantic request/response models."
  - "Classification is implemented as deterministic local keyword rules with classifierVersion `rules-v1`."
  - "Runtime checks reject provider-backed scope creep by keeping external AI provider strings out of app, Dockerfile, and pyproject files."

patterns-established:
  - "Use `ClassificationRequest` and `ClassificationResponse` as the contract source for both OpenAPI and tests."
  - "Use word-boundary keyword matching to avoid accidental substring overmatches."
  - "Use confidence bands to distinguish critical, escalated, direct category, weak signal, and fallback classifications."

requirements-completed: [AI-01, AI-02, QUAL-04]

duration: 24min
completed: 2026-06-11
---

# Phase 04-01: FastAPI Classification Service Summary

**Provider-free FastAPI ticket classification service with strict Pydantic contracts, deterministic keyword rules, Docker runtime, and pytest coverage**

## Performance

- **Duration:** 24 min
- **Started:** 2026-06-11T07:56:00Z
- **Completed:** 2026-06-11T08:20:36Z
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments

- Added `ai-service-python` as a callable FastAPI service with `/health` and `POST /api/v1/classifications/tickets`.
- Added strict Pydantic request/response models with fixed urgency, sentiment, and priority enums plus bounded confidence.
- Implemented deterministic keyword classification for billing, technical, account, cancellation, and fallback general tickets.
- Added pytest coverage for API validation, OpenAPI schema visibility, deterministic rules, Dockerfile runtime shape, and provider-free runtime files.

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffold FastAPI package, models, and health/classification routes** - `3e76099` (feat)
2. **Task 2: Implement deterministic keyword classifier and reference dataset tests** - `0d11bf3` (feat)
3. **Task 3: Harden FastAPI service contract, OpenAPI visibility, and Docker runtime** - `f10b5fa` (test)

**Plan metadata:** committed with this summary.

## Files Created/Modified

- `ai-service-python/pyproject.toml` - Python project metadata, pinned FastAPI/Pydantic/pytest/httpx dependencies, and pytest config.
- `ai-service-python/Dockerfile` - Python 3.12 FastAPI runtime exposing port 8000.
- `ai-service-python/app/main.py` - FastAPI app, health route, and ticket classification route.
- `ai-service-python/app/models.py` - Strict Pydantic request/response models and enums.
- `ai-service-python/app/classifier.py` - Pure deterministic classifier and `CLASSIFIER_VERSION = "rules-v1"`.
- `ai-service-python/app/settings.py` - Service name, version, and ticket classification path constants.
- `ai-service-python/tests/test_classifier_rules.py` - Reference dataset and deterministic repeated-call coverage.
- `ai-service-python/tests/test_classification_api.py` - Health, classification, validation, OpenAPI, and provider env tests.
- `ai-service-python/tests/test_runtime_contract.py` - Dockerfile and runtime provider-string checks.

## Decisions Made

- Used direct FastAPI + Pydantic service code without LangChain, LangGraph, LlamaIndex, CrewAI, provider SDKs, prompts, embeddings, or async queues.
- Kept classification stateless; the Spring backend will own persistence, attempts, auto-apply, and workflow traceability in later plans.
- Used regex word boundaries for keyword matching after tests exposed a `blocked` versus `locked` substring overmatch.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Prevented keyword substring overmatch**
- **Found during:** Task 2 (deterministic classifier rules)
- **Issue:** Simple substring matching classified `blocked` as account-related because it contains `locked`.
- **Fix:** Changed classifier keyword matching to regex word-boundary matching for both single-word and phrase keywords.
- **Files modified:** `ai-service-python/app/classifier.py`
- **Verification:** `cd ai-service-python && python -m pytest` passed with 20 tests.
- **Committed in:** `0d11bf3` (Task 2 commit)

**Total deviations:** 1 auto-fixed (blocking correctness issue)
**Impact on plan:** No scope change; the fix improves deterministic rule correctness.

## Issues Encountered

- Local shell does not expose a `python` command until the service venv is activated. Verification was run with `. .venv/bin/activate && python -m pytest`, which uses the plan's command form inside the venv.
- FastAPI's current `TestClient` emits a Starlette deprecation warning about `httpx`; tests pass and the warning is from dependencies, not project code.

## User Setup Required

None - no external service configuration required.

## Verification

- `cd ai-service-python && python -m pytest` in the activated service venv - PASS, 20 tests.
- `/health` and `/api/v1/classifications/tickets` are exposed - PASS.
- `ClassificationRequest` and `ClassificationResponse` use strict models with bounded confidence - PASS.
- `CLASSIFIER_VERSION = "rules-v1"` and deterministic keyword rules are covered by tests - PASS.
- Runtime files contain no provider-backed LLM, prompt, embedding, RAG, queue, Redis, frontend, auth, or draft behavior - PASS.

## Self-Check: PASSED

- Created summary file exists: `.planning/phases/04-ai-classification-integration/04-01-SUMMARY.md`.
- Task commits found: `3e76099`, `0d11bf3`, and `f10b5fa`.
- Key AI service files exist under `ai-service-python`.
- All task acceptance criteria and plan-level verification passed.
- `.planning/STATE.md` and `.planning/ROADMAP.md` were not staged or committed by this executor.

## Next Phase Readiness

Plan 04-02 can now add the Spring backend AI classification client, classification attempts, automatic category/priority application, manual re-analysis, and workflow history links against the stable FastAPI contract.

---
*Phase: 04-ai-classification-integration*
*Completed: 2026-06-11*
