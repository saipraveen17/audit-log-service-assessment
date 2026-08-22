# Task Plan

This task plan orders the work for the tamper-evident audit log service. Status
values are `Pending`, `In progress`, `Done`, or `Blocked`.

| ID | Task | Dependencies | Status |
| --- | --- | --- | --- |
| TASK-001 | Create baseline repository files and AI usage log. | None | Done |
| TASK-002 | Document and refine normalized requirements, task plan, and Scenario C scope. | TASK-001 | Done |
| TASK-003 | Human review of requirements, Scenario C scope, and acceptance criteria. | TASK-002 | Done |
| TASK-004 | Design the complete system before implementation, including API/schema definitions, persistence model, hash-chain canonicalization, retention, redaction, export verification, Scenario C scope, security boundaries, and ADRs. | TASK-003 | Pending |
| TASK-005 | Create runnable project setup with Spring Boot, Maven Wrapper, PostgreSQL local dependency, baseline API authentication and authorization, and documented local commands. | TASK-004 | Pending |
| TASK-006 | Implement Scenario A event creation with hash-chain append, append-only persistence, unit tests, and integration tests. | TASK-005 | Pending |
| TASK-007 | Implement Scenario A combined query filters, pagination, API/schema documentation, unit tests, and integration tests. | TASK-006 | Pending |
| TASK-008 | Implement `GET /audit/verify` full-chain verification, first inconsistency reporting, direct database tamper detection test, and supporting documentation. | TASK-006 | Pending |
| TASK-009 | Implement Scenario B retention with archival or soft deletion, including configuration, tests, and documentation. | TASK-008 | Pending |
| TASK-010 | Implement Scenario B sensitive payload redaction without breaking verification, including tests and documentation. | TASK-009 | Pending |
| TASK-011 | Implement Scenario B self-contained verifiable export for one `actorId` or one `resourceId`, including independent verification tests and documentation. | TASK-010 | Pending |
| TASK-012 | Implement Scenario C mandatory scope or documented partial implementation with justification, including compliance-review report behavior, tests or validation, and documentation. | TASK-011 | Pending |
| TASK-013 | Perform security review and hardening across validation, authorization assumptions, sensitive data exposure, logging, errors, exports, redaction, and retention. | TASK-006, TASK-007, TASK-008, TASK-009, TASK-010, TASK-011, TASK-012 | Pending |
| TASK-014 | Final validation: run relevant checks and tests, inspect uncommitted diff, update AI traceability, document limitations, and prepare final summary and attestation. | TASK-013 | Pending |

## Current Task Notes

TASK-001 through TASK-003 are complete.

TASK-004 is the next active task. It must define and document the complete
system architecture before application scaffolding or implementation begins.
The task includes API and schema definitions, persistence, hashing,
verification, retention, redaction, export, Scenario C scope, authentication,
authorization, testing strategy, trade-offs, and ADRs.

## Review Gates

- Requirements review is required before system design is finalized.
- The complete system must be designed before implementation starts.
- Architecture and ADR review is required before high-impact implementation
  choices involving persistence, hashing, verification, retention, archival or
  soft deletion, redaction, export, authentication, or authorization.
- Scenario C is mandatory to address through implementation or documented
  partial implementation with justification.
- Security review is required before the service is considered complete.
- Final validation remains pending until completed by the engineer.
