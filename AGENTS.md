# AGENTS.md

## Repository Purpose

This repository contains my individual implementation of a confidential
software engineering assessment.

The repository history should demonstrate the complete engineering process:
requirement analysis, task decomposition, design, implementation, testing,
validation, and controlled AI-assisted development.

## Context Hierarchy

Before starting a task, use repository context in this order:

1. `AGENTS.md` defines permanent working, security, and governance rules.
2. `docs/REQUIREMENTS.md` defines the normalized system requirements.
3. `docs/ARCHITECTURE.md` and files under `docs/adr/` define approved design
   decisions.
4. `docs/TASK_PLAN.md` defines task dependencies and implementation sequence.
5. The current task prompt defines the immediate scope and acceptance criteria.

Some of these files may not exist during initial repository setup.

When required context is missing, use only the information supplied in the
current task prompt. Do not invent requirements or design decisions.

If repository documents conflict, or an important decision is unclear, stop and
mark it for human review.

## Engineer Ownership

- The engineer owns all architecture, correctness, security, maintainability,
  testing, and production-readiness decisions.
- AI output is a proposal until the engineer reviews it.
- Do not state that the engineer approved, accepted, modified, or rejected an
  output unless the engineer explicitly records that decision.
- Mark decisions as `Pending human review` where approval has not yet been
  provided.
- The engineer performs final validation and creates all Git commits.

## Git Rules

- Do not stage files.
- Do not commit or push changes.
- Do not amend commits.
- Do not create or merge pull requests.
- Do not rebase, reset, tag, cherry-pick, or rewrite Git history.
- Do not change repository visibility or repository settings.
- Read-only Git commands such as `git status`, `git diff`, and `git log` are
  allowed.
- Leave all Git history-changing actions to the engineer.

## Confidentiality and Security

- Do not add the assignment PDF, recruiter communication, screenshots, or
  copied confidential instructions to the repository.
- Do not add passwords, tokens, API keys, credentials, private keys,
  certificates, or other secrets.
- Do not use real customer, account, employee, or client information.
- Use only synthetic data in code, tests, documentation, and demonstrations.
- Do not expose secrets, sensitive values, encryption material, or complete
  payload contents in logs or error messages.
- Do not send repository content to unrelated external services.
- Do not add client or company names unless explicitly required by the
  attestation.

## AI Usage Logging

For every material task, add or update an entry in:

`docs/ai/AI_USAGE_LOG.md`

Use the task ID supplied in the prompt, such as `TASK-001`.

Each entry should record:

- Date and task ID
- AI tool used
- Short summary of the prompt intent
- Important constraints supplied to the AI
- What the AI proposed, generated, reviewed, or changed
- Files created or modified
- Commands and tests executed
- Test or validation results observed
- Risks, assumptions, or limitations identified
- Human review status

Use these human-review fields:

- **Accepted:** Pending human review
- **Modified:** Pending human review
- **Rejected:** Pending human review
- **Rationale:** Pending human review
- **Final validation:** Pending human review

Do not fill these fields on behalf of the engineer.

Additional logging rules:

- Keep the AI log concise and connected to material repository work.
- Do not create separate entries for imports, formatting, autocomplete, or
  trivial corrections.
- Do not create a separate AI entry merely for updating the AI log.
- Do not paste full conversations or confidential source material into the log.
- Keep the AI log update in the same working change set as the related task.
- If a task is only planning or review, record that no application code was
  changed.
- Record failed commands and failed tests when they materially influenced the
  work. Do not report only successful results.

## Working Process

For each material task:

1. Read `AGENTS.md` and the relevant repository documents.
2. Restate the task in a short summary.
3. Identify assumptions, affected files, risks, and acceptance criteria.
4. Provide a short implementation or documentation plan.
5. For architecture, security, persistence, cryptography, authentication, or
   other high-impact decisions, wait for human approval before implementation
   unless the prompt explicitly says the plan is already approved.
6. Keep changes limited to the active task.
7. Do not make unrelated refactoring changes.
8. Run the relevant tests and quality checks.
9. Review the complete uncommitted diff.
10. Update the AI usage log.
11. Report completed work and anything still pending human review.

If a prompt explicitly says `plan and proceed`, provide a short plan and then
continue without waiting, but remain within the approved task scope.

## Approved Technical Direction

Unless a later approved architecture decision changes it, use:

- Java 21
- Spring Boot
- Maven with Maven Wrapper
- Spring Data JPA and Hibernate
- PostgreSQL
- JUnit 5 and Mockito
- Unit tests
- Integration tests for critical application and persistence flows
- Docker Compose for local dependencies and execution

Do not introduce additional infrastructure, frameworks, databases, messaging
systems, or external services without first explaining:

1. Why they are required
2. What simpler alternative was considered
3. What complexity, maintenance, or security cost they introduce

## Testing and Validation

- Add focused tests for behaviour introduced by each task.
- Use unit tests for isolated business logic, validation, hashing, formatting,
  and failure classification.
- Use integration tests where correctness depends on Spring configuration,
  HTTP handling, persistence, transactions, database constraints, or actual
  database behaviour.
- Prioritize the critical end-to-end flows required by the assessment.
- Use PostgreSQL behaviour for database-dependent validation.
- Do not silently replace database-dependent behaviour with H2-specific
  behaviour.
- Do not weaken, remove, or bypass tests merely to make the build pass.
- Record which tests were executed and their actual results.
- Clearly document intentionally uncovered cases and time-boxed limitations.
- Do not claim end-to-end correctness using only mocked unit tests.

## General Quality Rules

- Prefer simple, explicit, maintainable code.
- Avoid unnecessary abstractions and premature optimization.
- Validate external input.
- Use consistent API error responses.
- Avoid exposing sensitive information in logs, errors, and test output.
- Do not add dependencies without explaining why they are needed.
- Do not suppress warnings or exceptions without explaining the reason.
- Do not create placeholder implementations and describe them as complete.
- Do not claim a task is complete unless its relevant validation succeeds.
- Preserve existing working behaviour while making scoped changes.
- Keep documentation aligned with the actual implementation.

## Task Completion Report

At the end of each material task, report:

- Files created or modified
- Important decisions made during the task
- Commands and tests executed
- Actual test results
- Risks, assumptions, or limitations
- Items pending human review
- Suggested commit message

Do not commit or push the changes.