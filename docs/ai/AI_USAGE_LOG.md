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

## 2026-08-22 - TASK-005 - Runnable Spring Boot project setup

- **AI tool used:** Codex
- **Prompt intent:** Create the runnable Spring Boot project setup without
  implementing audit events, hash-chain, query, verification, retention,
  redaction, export, or compliance features.
- **Important constraints supplied to the AI:** Follow `AGENTS.md`; use Java 21,
  Spring Boot, Maven Wrapper, Spring Web, Validation, Spring Data JPA, Spring
  Security, PostgreSQL, Spring Boot Test, Spring Security Test, and
  Testcontainers PostgreSQL; create the approved layered package structure under
  `com.assessment.auditlog`; use environment-backed HTTP Basic prototype users
  and roles; keep credentials out of committed source files; configure
  PostgreSQL from environment variables with safe local defaults; use
  `ddl-auto=update` locally and `create-drop` for tests; disable Open Session in
  View, CSRF, and SQL parameter logging; do not add audit entities or feature
  endpoints; do not add Flyway, Liquibase, Lombok, H2, Kafka, Redis, or other
  services; do not stage, commit, or push.
- **AI proposed, generated, reviewed, or changed:** Created the Maven project,
  self-bootstrapping Maven Wrapper, Spring Boot application entry point,
  approved package structure, environment-backed security properties and
  stateless HTTP Basic security configuration, ProblemDetail exception
  foundation, local and test application configuration, PostgreSQL Compose file,
  `.env.example`, one application context test, README setup instructions, and
  `.gitignore` additions for confidential and secret material. Marked TASK-005
  as `In progress`.
- **Files created or modified:** `pom.xml`, `mvnw`, `mvnw.cmd`,
  `.mvn/wrapper/maven-wrapper.properties`, `src/main/java/**`,
  `src/main/resources/application.properties`, `src/test/java/**`,
  `../../src/test/resources/application.properties`, `compose.yml`, `.env.example`,
  `.gitignore`, `README.md`, `docs/TASK_PLAN.md`,
  `docs/ai/AI_USAGE_LOG.md`
- **Commands and tests executed:** `sed -n` review commands for `AGENTS.md` and
  TASK-004 architecture documents; `find` repository inspection commands;
  `mvn -version` which failed because Maven is not globally installed;
  `java -version`; `mkdir -p`; `chmod +x mvnw`; `./mvnw test`;
  `./mvnw verify`; `docker compose config`; `rg -n` consistency checks;
  `git diff --check`; `git status --short --ignored`
- **Test or validation results observed:** `./mvnw test` passed with one context
  test. `./mvnw verify` passed and built the Spring Boot jar. `docker compose
  config` passed. `git diff --check` passed. Maven is not globally installed,
  so the Maven Wrapper performed dependency resolution. Test output included
  Mockito/Byte Buddy dynamic-agent warnings from the test stack, with no test
  failures.
- **Risks, assumptions, or limitations identified:** The context test excludes
  datasource and JPA auto-configuration because TASK-005 has no audit entities
  or persistence flows yet; Testcontainers PostgreSQL is present for later
  integration tests. The Maven Wrapper is self-bootstrapping in this assessment
  environment. Prototype HTTP Basic users are configured only through
  environment variables; no runnable credentials are committed. No audit
  feature behavior has been implemented.
- **Accepted:**
  - The Java 21 Spring Boot Maven scaffold and Maven Wrapper.
  - Spring Web, Validation, JPA, Security, PostgreSQL, and test dependencies.
  - Docker Compose PostgreSQL setup.
  - Environment-backed HTTP Basic users and endpoint role authorization.
  - The stateless Spring Security configuration.
  - The ProblemDetail exception-handling foundation.
  - The application context smoke test.
  - The README local startup instructions and `.env.example`.

- **Modified:**
  - Replaced YAML configuration with `application.properties` because it
    matches the engineer's normal Spring Boot workflow.
  - Removed empty `package-info.java` placeholder files because they provided
    no package documentation or annotations.
  - Kept one default runtime configuration instead of introducing local,
    development, or Docker profiles.
  - Replaced the unused named test-profile configuration with test-classpath
    `application.properties`.
  - Disabled Hibernate SQL logging explicitly.
  - Corrected README instructions so `.env` values are exported before starting
    the Spring Boot process.
  - Clarified that Maven was unavailable only inside the Codex execution
    environment, while the Maven Wrapper completed the build.

- **Rejected:**
  - Empty package placeholder files used only to force folders into Git.
  - YAML configuration for this implementation.
  - Additional named Spring profiles before a real environment-specific need
    exists.
  - Hardcoded runnable usernames or passwords in committed configuration.
  - Treating the scaffold context test as sufficient database integration
    coverage.

- **Rationale:**
  - The final scaffold uses the engineer's familiar configuration format and
    keeps runtime configuration simple. Packages will be created when real
    feature classes are introduced. Environment-backed credentials preserve
    repository safety, while PostgreSQL and Testcontainers dependencies prepare
    the service for the required persistence integration tests without
    prematurely implementing scenario behavior.

- **Final validation:**
  - Reviewed all TASK-005 source, configuration, security, Docker Compose,
    README, and build files.
  - Ran `./mvnw clean verify`; it passed.
  - Ran `docker compose config`; it passed.
  - Started PostgreSQL through Docker Compose.
  - Started the Spring Boot application and confirmed that it connected to
    PostgreSQL successfully.
  - Confirmed no audit-event feature implementation was added during TASK-005.

## 2026-08-22 - TASK-006 - Scenario A event creation and hash-chain append

- **AI tool used:** Codex
- **Prompt intent:** Implement Scenario A audit event creation with append-only
  PostgreSQL persistence and transactional hash-chain append.
- **Important constraints supplied to the AI:** Follow `AGENTS.md`; implement
  only `POST /audit/events`; do not implement querying, `/audit/verify`,
  retention, redaction, encryption, export, or compliance reporting; assign the
  timestamp server-side in UTC at millisecond precision through an injectable
  `Clock`; store payload as PostgreSQL JSONB in `committedPayload`; manually
  allocate event IDs from the locked GLOBAL chain-state row; avoid
  `@GeneratedValue`, sequences, public entity setters, and update/delete APIs;
  use deterministic JSON canonicalization and lowercase SHA-256 hashes; avoid
  caller-controlled delimiter concatenation in content hashing; reject unknown
  request fields, including caller-supplied `timestamp` and unsupported
  `sensitivePaths`; keep payload values out of logs and error messages; leave
  TASK-006 in progress; do not stage, commit, or push.
- **AI proposed, generated, reviewed, or changed:** Added audit event and chain
  state entities, request/response DTOs, controller, service, repositories,
  chain-state initialization, deterministic JSON canonicalization, UTC
  timestamp formatting, SHA-256 content and record hashing, manual ID
  allocation under a pessimistic chain-state lock, and focused unit and
  PostgreSQL integration tests for creation, linking, concurrency, validation,
  authorization, and application context startup. Refined content hashing to
  canonicalize one JSON object, removed runtime chain-state recreation from the
  append path, added defensive response payload copying, rejected unknown
  request properties, made unreadable request-body errors generic, strengthened
  persisted hash and chain-head assertions, and updated the README status.
  Marked TASK-006 as `In progress`.
- **Files created or modified:** `src/main/java/com/assessment/auditlog/config/TimeConfig.java`,
  `src/main/java/com/assessment/auditlog/controller/AuditEventController.java`,
  `src/main/java/com/assessment/auditlog/dto/AuditEventResponse.java`,
  `src/main/java/com/assessment/auditlog/dto/CreateAuditEventRequest.java`,
  `src/main/java/com/assessment/auditlog/entity/AuditChainState.java`,
  `src/main/java/com/assessment/auditlog/entity/AuditEvent.java`,
  `src/main/java/com/assessment/auditlog/exception/ApiExceptionHandler.java`,
  `src/main/java/com/assessment/auditlog/repository/AuditChainStateRepository.java`,
  `src/main/java/com/assessment/auditlog/repository/AuditEventInsertRepository.java`,
  `src/main/java/com/assessment/auditlog/repository/AuditEventRepository.java`,
  `src/main/java/com/assessment/auditlog/service/AuditChainStateInitializer.java`,
  `src/main/java/com/assessment/auditlog/service/AuditEventService.java`,
  `src/main/java/com/assessment/auditlog/service/AuditHashService.java`,
  `src/main/java/com/assessment/auditlog/service/JsonCanonicalizer.java`,
  `src/main/java/com/assessment/auditlog/service/TimeFormats.java`,
  `src/test/java/com/assessment/auditlog/AuditLogApplicationTests.java`,
  `src/test/java/com/assessment/auditlog/PostgreSqlIntegrationTestSupport.java`,
  `src/test/java/com/assessment/auditlog/controller/AuditEventControllerIntegrationTest.java`,
  `src/test/java/com/assessment/auditlog/service/AuditHashServiceTest.java`,
  `src/test/java/com/assessment/auditlog/service/JsonCanonicalizerTest.java`,
  `src/main/resources/application.properties`,
  `src/test/resources/application.properties`, `README.md`,
  `docs/TASK_PLAN.md`, `docs/ai/AI_USAGE_LOG.md`
- **Commands and tests executed:** `sed -n`, `rg --files`, and `git status`
  review commands; `./mvnw -Dtest=JsonCanonicalizerTest,AuditHashServiceTest
  test`; `./mvnw -Dtest=AuditEventControllerIntegrationTest,AuditLogApplicationTests
  test`; `./mvnw
  -Dtest=JsonCanonicalizerTest,AuditHashServiceTest,AuditEventControllerIntegrationTest,AuditLogApplicationTests
  test`; `./mvnw verify`; `git diff --check`.
- **Test or validation results observed:** Focused unit tests passed
  (4 tests). The first focused integration run was interrupted after exposing a
  test-fixture issue where Spring cached a datasource after a Testcontainers
  PostgreSQL instance stopped. A second integration run failed because the
  fixed test `Clock` was not imported into the Spring test context. After
  adding explicit test context cleanup and importing the fixed clock, the
  focused integration tests passed (5 tests). Full `./mvnw verify` passed
  (9 tests). Final focused tests after the refinement passed (11 tests). Final
  `./mvnw verify` passed (11 tests). `git diff --check` passed.
- **Risks, assumptions, or limitations identified:** Scenario A currently
  covers event creation and append-only hash-chain linking only. Query,
  full-chain verification, retention, redaction, export, and compliance
  reporting remain future tasks. The implementation serializes appends through
  one GLOBAL chain-state row as approved; this favors correctness over write
  throughput for the prototype.
- **Accepted:**
  - The authenticated `POST /audit/events` endpoint and validation flow.
  - Server-assigned UTC timestamps through an injectable `Clock`.
  - PostgreSQL JSONB persistence for structured event payloads.
  - Service-assigned event IDs allocated from the locked GLOBAL chain state.
  - Insert-only event persistence with no update or delete API.
  - SHA-256 content, previous-record, and record hash storage.
  - Transactional pessimistic locking for linear concurrent append behavior.
  - Unit tests and PostgreSQL Testcontainers integration tests.

- **Modified:**
  - Replaced newline-delimited content-hash input with one canonical JSON
    representation of all hashed event fields.
  - Rejected unknown JSON request fields so timestamp and unsupported
    sensitivePaths are not silently ignored.
  - Changed missing chain-state behavior from runtime recreation to fail-closed
    handling.
  - Returned a defensive payload copy in the API response.
  - Strengthened integration tests to recalculate persisted hashes and verify
    the final chain-state head.
  - Updated the README to reflect the implemented TASK-006 capability.

- **Rejected:**
  - Unescaped string concatenation as the canonical content-hash format because
    different field combinations could produce the same pre-hash input.
  - Silently accepting and ignoring unknown request properties.
  - Recreating missing global chain state from an append request without an
    existing row lock.
  - Adding query, verification, retention, redaction, export, or compliance
    behavior before their planned tasks.

- **Rationale:**
  - The final implementation keeps append behavior simple while ensuring that
    the hash representation has unambiguous field boundaries, concurrent writes
    form one linear chain, unsupported security-related fields fail safely, and
    historical events have no application-level mutation path.

- **Final validation:**
  - Reviewed all TASK-006 source code, tests, task-plan changes, README status,
    and AI traceability.
  - Ran the focused unit and integration tests successfully.
  - Ran `./mvnw clean verify`; all tests passed.
  - Confirmed concurrent appends produced unique contiguous IDs and valid
    previous-hash links.
  - Confirmed persisted content and record hashes can be independently
    recalculated.
  - Confirmed unauthorized and incorrectly authorized requests are rejected.
  - Confirmed unknown timestamp and sensitivePaths properties are rejected.
  - Confirmed no `.env`, `.idea`, `target`, secret, or confidential source file
    is staged.

## 2026-08-22 - TASK-007 - Scenario A audit-event query API

- **AI tool used:** Codex
- **Prompt intent:** Implement `GET /audit/events` with combined filters and
  cursor pagination for non-archived audit events.
- **Important constraints supplied to the AI:** Follow `AGENTS.md`; implement
  only the query API; support optional `actorId`, `resourceType`, `resourceId`,
  `eventType`, `from`, and `to` filters in any combination; support exclusive
  `afterId` and bounded `limit`; order by `id ASC`; fetch `limit + 1` records
  for `hasMore`; do not add page/size pagination, total counts, GET-by-ID,
  verification, retention, redaction, export, or compliance reporting; keep the
  audit-event repository write-free; create test data through the normal append
  flow; leave human-review fields pending; do not stage, commit, or push.
- **AI proposed, generated, reviewed, or changed:** Added query response DTOs,
  a read-only Criteria-based query repository, service-level query parameter
  parsing and validation, `GET /audit/events` controller support, documented
  query indexes on the audit-event entity, an adjustable PostgreSQL integration
  test clock, and integration tests covering filters, combined filters,
  inclusive/exclusive time bounds, cursor pagination, empty results, limits,
  invalid query parameters, and authorization. Updated README status and marked
  TASK-007 as `In progress`.
- **Files created or modified:** `README.md`, `docs/TASK_PLAN.md`,
  `docs/ai/AI_USAGE_LOG.md`,
  `src/main/java/com/assessment/auditlog/controller/AuditEventController.java`,
  `src/main/java/com/assessment/auditlog/dto/AuditEventQueryItemResponse.java`,
  `src/main/java/com/assessment/auditlog/dto/AuditEventQueryResponse.java`,
  `src/main/java/com/assessment/auditlog/entity/AuditEvent.java`,
  `src/main/java/com/assessment/auditlog/repository/AuditEventQueryRepository.java`,
  `src/main/java/com/assessment/auditlog/service/AuditEventQuery.java`,
  `src/main/java/com/assessment/auditlog/service/AuditEventService.java`,
  `src/main/java/com/assessment/auditlog/service/TimeFormats.java`,
  `src/test/java/com/assessment/auditlog/PostgreSqlIntegrationTestSupport.java`,
  `src/test/java/com/assessment/auditlog/controller/AuditEventQueryIntegrationTest.java`
- **Commands and tests executed:** `sed -n` review commands for `AGENTS.md`,
  architecture, API, data model, task plan, README, AI log, and current
  implementation files; `./mvnw -Dtest=JsonCanonicalizerTest,AuditHashServiceTest
  test`; `./mvnw
  -Dtest=AuditEventControllerIntegrationTest,AuditEventQueryIntegrationTest,AuditLogApplicationTests
  test`; `./mvnw verify`; `git diff --check`; `rg -n` guardrail searches.
- **Test or validation results observed:** Focused unit tests passed (5 tests).
  Focused PostgreSQL integration tests passed (15 tests). Full `./mvnw verify`
  passed (20 tests). `git diff --check` passed. Guardrail searches found no
  audit-event `@GeneratedValue`, sequence generation, update/delete endpoint
  mappings, or write-capable `save` method added to the audit-event repository.
- **Risks, assumptions, or limitations identified:** Query responses currently
  report all returned records as `archived=false` because retention and archive
  markers are intentionally deferred. Full-chain verification, retention,
  redaction, export, and compliance reporting remain future tasks.
- **Accepted:**
  - The authenticated `GET /audit/events` endpoint.
  - Combined filtering by actor, resource type, resource ID, event type, and
    time range.
  - Cursor pagination using exclusive `afterId`, bounded `limit`, and ascending
    event ID order.
  - The read-only Criteria-based query repository.
  - Fetching `limit + 1` rows instead of running a total-count query.
  - Defensive payload copying in query responses.
  - Inclusive `from` and exclusive `to` time-boundary behavior.
  - Query indexes and PostgreSQL integration coverage.
  - Reader and administrator authorization with 401 and 403 validation.

- **Modified:**
  - No material implementation changes were required after final human review.
  - The temporary `archived=false` response behavior was retained and clearly
    documented because retention markers are intentionally deferred to
    TASK-009.

- **Rejected:**
  - Page-and-size and offset pagination.
  - Total-page and total-record count queries.
  - A general write-capable repository for historical audit events.
  - A GET-by-ID endpoint outside the required query scope.
  - Implementing verification, retention, redaction, export, or compliance
    behavior during the query task.

- **Rationale:**
  - Cursor pagination uses the existing immutable, increasing event ID and
    remains stable while new events are appended. The Criteria query supports
    all required filter combinations without exposing mutation operations or
    constructing SQL from user-controlled values.

- **Final validation:**
  - Reviewed the controller, service, query DTOs, Criteria repository, entity
    indexes, test infrastructure, README, task plan, and AI usage entry.
  - Confirmed the existing audit-event repository still exposes no save,
    update, or delete operation.
  - Confirmed all query results are ordered by ID ascending.
  - Confirmed filter, time-boundary, pagination, validation, and authorization
    integration tests are present.
  - Ran the focused unit tests; 5 tests passed.
  - Ran the focused PostgreSQL integration tests; 15 tests passed.
  - Ran `./mvnw verify`; all 20 tests passed.
  - Ran `git diff --check`; it passed.
  - Confirmed `.env`, `.idea`, and `target` are not staged or tracked.

## 2026-08-22 - TASK-008 - Full-chain verification and tamper detection

- **AI tool used:** Codex
- **Prompt intent:** Implement `GET /audit/verify` full-chain verification with
  first-inconsistency reporting and direct database tamper-detection tests.
- **Important constraints supplied to the AI:** Follow `AGENTS.md`; return HTTP
  200 for intact and broken chains; use the documented verification response
  shape; run verification in one read-only PostgreSQL `REPEATABLE_READ`
  transaction; read the GLOBAL chain state without taking the append lock; stop
  at the first inconsistency; never return payload contents in verification
  responses or errors; detect ID gaps, unsupported hash versions, content hash
  mismatches, previous hash mismatches, record hash mismatches, and chain-head
  mismatches; use direct SQL tampering in tests; do not implement retention,
  redaction, export, or compliance reporting; leave human-review fields
  pending; do not stage, commit, or push.
- **AI proposed, generated, reviewed, or changed:** Added a dedicated
  verification controller, response DTO, violation enum, and verification
  service. The service captures the chain head, scans audit events in ID order,
  recalculates content and record hashes, checks continuity and previous-hash
  linkage, detects stale or inconsistent chain heads, and reports the first
  inconsistency without returning payloads. Added PostgreSQL integration tests
  for empty chain, intact chains, direct actor/payload tampering,
  previous-hash tampering, record-hash tampering, unsupported hash version,
  deleted middle and final records, corrupted chain head, event beyond the
  captured chain-state last ID, earliest-inconsistency precedence, HTTP 200
  broken-chain responses, and verifier/admin authorization. Updated README
  status and marked TASK-008 as `In progress`.
- **Files created or modified:** `README.md`, `docs/TASK_PLAN.md`,
  `docs/ai/AI_USAGE_LOG.md`,
  `src/main/java/com/assessment/auditlog/controller/AuditVerificationController.java`,
  `src/main/java/com/assessment/auditlog/dto/AuditVerificationResponse.java`,
  `src/main/java/com/assessment/auditlog/service/AuditVerificationService.java`,
  `src/main/java/com/assessment/auditlog/service/AuditVerificationViolationType.java`,
  `src/test/java/com/assessment/auditlog/controller/AuditVerificationIntegrationTest.java`
- **Commands and tests executed:** `sed -n` review commands for `AGENTS.md`,
  architecture, API, ADR-001, testing strategy, task plan, and current Scenario
  A implementation; `./mvnw -Dtest=AuditVerificationIntegrationTest test`;
  `./mvnw
  -Dtest=AuditEventControllerIntegrationTest,AuditEventQueryIntegrationTest,AuditVerificationIntegrationTest,AuditLogApplicationTests
  test`; `./mvnw verify`; `git diff --check`; `rg -n` guardrail searches.
- **Test or validation results observed:** Focused verification integration
  tests passed (13 tests). Broader focused PostgreSQL integration tests passed
  (28 tests). Full `./mvnw verify` passed (33 tests). `git diff --check`
  passed. Guardrail searches confirmed verification does not use the
  pessimistic append-lock method and does not expose payload fields in its DTO
  or controller.
- **Risks, assumptions, or limitations identified:** Verification is a full
  linear scan and is intentionally O(n) for the prototype. It detects database
  tampering against stored hashes and chain state, but without an external
  signed checkpoint a database administrator who rewrites every record and the
  chain state consistently remains outside the current trust boundary.
  Retention, redaction, export, and compliance reporting remain future tasks.
- **Accepted:**
  - Snapshot-based full-chain verification in a read-only PostgreSQL
    `REPEATABLE_READ` transaction.
  - Verification without acquiring the append lock.
  - Ordered validation of ID continuity, hash version, content hash,
    previous-hash linkage, record hash, and chain head.
  - First-inconsistency reporting and HTTP 200 responses for broken chains.
  - Direct-SQL tampering tests and verifier/admin authorization.

- **Modified:**
  - No material code changes were required after final human review.

- **Rejected:**
  - Repairing or modifying corrupted records during verification.
  - Returning payload contents in verification responses.
  - Holding the append lock during the complete scan.
  - Continuing after the first inconsistency.
  - Implementing Scenario B or C behavior in this task.

- **Rationale:**
  - The implementation validates a stable database snapshot without blocking
    concurrent appends for the duration of the scan. It reports the earliest
    detectable failure while preserving sensitive-data boundaries.

- **Final validation:**
  - Reviewed the verification controller, DTO, service, violation enum, tests,
    README, task plan, and AI usage entry.
  - Confirmed verification does not call the pessimistic append-lock method.
  - Confirmed verification responses do not expose payload fields.
  - Ran the focused verification tests; 13 tests passed.
  - Ran the broader integration set; 28 tests passed.
  - Ran `./mvnw verify`; all 33 tests passed.
  - Ran `git diff --check`; it passed.
