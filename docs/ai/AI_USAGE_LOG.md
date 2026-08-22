# AI Usage Log

This log records material AI-assisted repository work. Human review fields are
left as pending until explicitly completed by the engineer.

## 2026-08-22 - TASK-001 - Repository baseline files

- **AI tool used:** Codex
- **Prompt intent:** Create common baseline repository files for the audit log
  service assessment.
- **Important constraints supplied to the AI:** Follow `AGENTS.md`; do not
  stage, commit, or push changes; do not add confidential assessment material or
  secrets; use only current prompt context when requirements and architecture
  documents are missing; keep changes limited to `.gitignore`, `README.md`, and
  `docs/ai/AI_USAGE_LOG.md`.
- **AI proposed, generated, reviewed, or changed:** Created a conservative
  Java/Spring/Maven-oriented `.gitignore`, a high-level README that documents
  repository governance and approved technical direction without inventing
  service behavior, and the initial AI usage log.
- **Files created or modified:** `.gitignore`, `README.md`,
  `docs/ai/AI_USAGE_LOG.md`
- **Commands and tests executed:** `rg --files`, `git status --short`,
  `git status --short --ignored`, `sed -n '1,260p' AGENTS.md`,
  `find . -maxdepth 3`, `mkdir -p docs/ai`, `git diff --check`,
  `sed -n` file review commands
- **Test or validation results observed:** No application tests existed yet;
  repository context was inspected and baseline files were created.
- **Risks, assumptions, or limitations identified:** The initial task was later
  recorded as `TASK-001`; `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`,
  `docs/adr/`, and `docs/TASK_PLAN.md` were not present yet; README
  intentionally avoided claiming implemented functionality.
- **Accepted:**
  - The baseline `.gitignore`, initial README, and reusable AI usage-log
    structure.
- **Modified:**
  - No material modifications were required after review.
- **Rejected:**
  - None.
- **Rationale:**
  - The baseline established repository hygiene and AI traceability before
    requirement analysis began, while avoiding premature implementation claims
    or confidential source material.
- **Final validation:**
  - Reviewed all files created during TASK-001.
  - Confirmed IntelliJ files, Maven build output, logs, local environment
    files, secrets, keys, certificates, and PDF files are ignored.
  - Confirmed no application code, confidential assignment document, real
    customer data, or secret was added.

## 2026-08-22 - TASK-002 - Scenario requirements and task planning

- **AI tool used:** Codex
- **Prompt intent:** Organize and refine requirements and planning for all three
  tamper-evident audit log scenarios before application implementation.
- **Important constraints supplied to the AI:** Follow `AGENTS.md`;
  documentation only; do not create Java code, Maven files, application
  packages, database configuration, Docker files, or architecture decisions; do
  not stage, commit, or push changes; keep human-review fields pending; identify
  Scenario B and C impacts on Scenario A without choosing a technical solution;
  after final human review corrections, mark TASK-002 and TASK-003 as done.
- **AI proposed, generated, reviewed, or changed:** Created and refined
  normalized requirements with confirmed event fields, query filters,
  `GET /audit/verify`, retention handling, verifiable export bundle behavior,
  Scenario C mandatory or partial implementation expectations, cross-cutting
  requirements, ambiguities, assumptions, acceptance criteria, and
  cross-scenario design impacts. Simplified the task plan into meaningful
  ordered tasks that design the complete system before implementation and pair
  tests with each feature. Refined Scenario C around an authorized internal
  compliance reviewer report that minimizes sensitive payload data and connects
  results to the tamper-evident audit history. Applied final human-review
  refinements for preceding hash wording, timestamp ambiguity, redaction
  verification expectations, Scenario C access-attempt scope, baseline API
  authentication and authorization planning, and TASK-002/TASK-003 completion
  status.
- **Files created or modified:** `docs/REQUIREMENTS.md`,
  `docs/TASK_PLAN.md`, `docs/SCENARIO_C.md`,
  `docs/ai/AI_USAGE_LOG.md`
- **Commands and tests executed:** `sed -n '1,260p' AGENTS.md`, `rg --files`,
  `git status --short --ignored`, `sed -n` review commands for
  `docs/REQUIREMENTS.md`, `docs/TASK_PLAN.md`, `docs/SCENARIO_C.md`, and
  `docs/ai/AI_USAGE_LOG.md`, `find . -maxdepth 3`, `git diff --check`
- **Test or validation results observed:** No application code was changed, so
  application tests were not applicable for this documentation-only task.
  `git diff --check` passed.
- **Risks, assumptions, or limitations identified:** Pagination model,
  canonical hash inputs, timestamp ownership, authorization model, redaction
  proof details, export bundle format, retention archival versus soft deletion
  behavior, and exact Scenario C scope remain pending implementation design.
  TASK-002 documentation intentionally does not choose architecture or technical
  mechanisms.
- **Accepted:**
  - The separation of confirmed requirements, genuine ambiguities, working
    assumptions, acceptance criteria, and cross-scenario design impacts.
  - The inclusion of all three scenarios during initial requirement analysis.
  - The clarified Scenario C scope based on an authorized internal compliance
    reviewer.
  - The use of task IDs, dependencies, statuses, and review gates.
  - The requirement that architecture be documented before implementation
    begins.
  - The inclusion of unit tests, integration tests, security review, final
    validation, engineering summary, and attestation in the execution plan.

- **Modified:**
  - Moved the mandatory event fields, query filters, full-chain behavior, and
    `GET /audit/verify` endpoint from open questions into confirmed
    requirements.
  - Clarified that each record stores the immediately preceding record hash or
    a defined genesis value.
  - Kept timestamp ownership open for the architecture phase instead of
    assuming that the caller supplies it.
  - Corrected retention so the design may use archival or soft deletion.
  - Strengthened redaction acceptance criteria so legitimate redaction must
    leave full-chain verification intact.
  - Strengthened export requirements so the result is self-contained and
    independently verifiable.
  - Changed Scenario C from optional clarification to mandatory implementation
    or a justified partial implementation.
  - Refined Scenario C so the compliance report proves the integrity of stored
    audit events without claiming that every upstream access was necessarily
    captured.
  - Reduced the original highly sequential task plan to a smaller set of
    meaningful implementation and validation tasks.
  - Added baseline authentication and authorization to the project setup plan.
  - Kept technical choices such as hashing format, pagination model, redaction
    mechanism, authentication method, and export proof model for the
    architecture phase.

- **Rejected:**
  - Rejected treating explicitly supplied event fields and query filters as
    ambiguities.
  - Rejected treating full-chain verification scope as an open question.
  - Rejected treating Scenario C implementation as optional.
  - Rejected the original thirty-task plan because it created excessive
    process overhead for the assessment timeline.
  - Rejected selecting specific hashing, retention, redaction, export, and
    authentication mechanisms during requirement analysis.
  - Rejected placing technical limitations inside the attestation.

- **Rationale:**
  - The final documents preserve the assignment's explicit requirements while
    keeping genuine technical choices open for architecture review. This makes
    the repository history show a clear progression from requirement
    understanding and ambiguity management to human approval, architecture,
    implementation, and validation. The task plan was also reduced so the
    required functionality and testing remain achievable within the available
    time.

- **Final validation:**
  - Reviewed the complete TASK-002 documentation and its revisions.
  - Cross-checked the normalized requirements against Scenarios A, B, and C.
  - Confirmed that explicit requirements are no longer presented as open
    questions.
  - Confirmed that Scenario C includes a working requirement, assumptions,
    implementation expectations, and explicit scope exclusions.
  - Confirmed that all three scenarios have acceptance criteria and planned
    implementation or validation tasks.
  - Confirmed that no Java code, Maven configuration, database configuration,
    Docker files, or architecture decisions were introduced during TASK-002.
  - Confirmed that TASK-002 and the human-review gate TASK-003 are marked
    `Done`.

## 2026-08-22 - TASK-004 - Architecture documentation

- **AI tools used:** Codex for repository-aware documentation changes; ChatGPT
  for design review, alternatives, trade-off analysis, and prompt preparation.
- **Prompt intent:** Convert the approved requirements into an implementation-ready
  architecture before creating application code or configuration.
- **Important constraints supplied to the AI:** Documentation only; keep one
  Spring Boot service and a standard layered MVC structure; use PostgreSQL and
  Hibernate without Flyway; use a server-assigned UTC timestamp, service-assigned
  BIGINT chain ID, cursor pagination, a global SHA-256 chain, archive-marker
  retention, cryptographic redaction, signed export, HTTP Basic roles, and a
  bounded Scenario C report; do not stage, commit, or push.
- **AI proposed, generated, reviewed, or changed:** Created the architecture,
  API contract, data model, testing strategy, and three ADRs. Iterative review
  clarified manual ID allocation under the chain-state lock, snapshot
  verification, logical archival, creation-time sensitive-field encryption,
  idempotent key-removal redaction, export proof contents, role authorization,
  Scenario C limits, and production alternatives.
- **Files created or modified:** `docs/ARCHITECTURE.md`, `docs/API.md`,
  `docs/DATA_MODEL.md`, `docs/TESTING_STRATEGY.md`,
  `docs/adr/ADR-001-hash-chain-and-concurrency.md`,
  `docs/adr/ADR-002-retention-and-redaction.md`,
  `docs/adr/ADR-003-export-security-and-compliance.md`, `docs/TASK_PLAN.md`,
  and `docs/ai/AI_USAGE_LOG.md`.
- **Commands and tests executed:** Repository and document inspection commands,
  `git status --short`, consistency searches, and `git diff --check`.
- **Test or validation results observed:** No application tests were applicable
  because TASK-004 changed documentation only. The final documentation passed
  `git diff --check`.
- **Risks, assumptions, or limitations identified:** A global row lock serializes
  appends; Hibernate schema generation is prototype-only; redaction depends on
  creation-time classification and secure key lifecycle; full proof headers can
  make exports large; HTTP Basic requires TLS; and the Scenario C report proves
  integrity of stored events rather than completeness of upstream capture.
- **Accepted:**
  - One layered Spring Boot service under `com.assessment.auditlog`.
  - Server-assigned UTC ingestion timestamps.
  - One service-assigned BIGINT ID for identity, ordering, verification, and
    cursor pagination.
  - PostgreSQL JSONB for structured payloads.
  - A global SHA-256 chain with content, previous, and record hashes.
  - Transactional chain-state locking and snapshot verification.
  - Archive-marker retention without mutating audit-event rows.
  - AES-GCM field encryption with key-removal redaction.
  - Ed25519-signed self-contained exports.
  - Stateless HTTP Basic role authorization for the prototype.
  - The bounded internal Scenario C compliance-report API.
  - Unit tests and critical PostgreSQL integration tests.
- **Modified:**
  - Replaced separate UUID and sequence identifiers with one service-assigned
    BIGINT ID.
  - Clarified that the ID is allocated under the chain-state lock rather than
    by a database sequence.
  - Selected cursor pagination using `afterId` and `limit`.
  - Selected one server-assigned timestamp and documented its ingestion-time
    semantics.
  - Added snapshot behavior so concurrent appends do not cause false verification
    failures.
  - Reduced retention to one concrete archive-marker design.
  - Made redaction observable through a logical payload before redaction and a
    redacted marker after key removal.
  - Defined exact export proof contents and offline verification steps.
  - Added administrator access to approved operational and compliance routes.
- **Rejected:**
  - Client-controlled timestamps for the prototype.
  - Page/size or offset pagination for the append-only log.
  - Database-generated chain positions.
  - Updating, moving, or deleting original audit-event rows for retention.
  - Response-only masking as the redaction mechanism.
  - Unsigned or checksum-only export bundles.
  - Holding the append lock during the complete verification scan.
  - Adding a path-version prefix that would replace the explicitly required
    `/audit/verify` route.
- **Rationale:** The selected design prioritizes correctness, implementation
  speed, understandable failure behavior, and live explainability. High-scale
  and stronger-security alternatives are documented without adding infrastructure
  that is unnecessary for the prototype.
- **Final validation:**
  - Reviewed all TASK-004 architecture, API, data-model, testing, and ADR files.
  - Confirmed consistency of ID allocation, timestamps, hash inputs,
    transaction boundaries, verification snapshots, retention, redaction,
    export proof, authorization, and Scenario C scope.
  - Confirmed no Java code, Maven configuration, application configuration,
    Docker files, or database schema files were added during TASK-004.
  - Confirmed only the new architecture artifacts, task status, and AI log are
    part of this task's change set.