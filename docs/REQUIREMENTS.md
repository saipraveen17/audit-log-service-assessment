# Requirements

## Scope

This document normalizes the currently supplied requirements for a
tamper-evident audit log service across Scenarios A, B, and C. It is not an
architecture decision record and does not choose implementation mechanisms.

## Confirmed Requirements

### Scenario A: Core Tamper-Evident Audit Log

- The service must support append-only audit event creation.
- Created audit events must not be mutable through the application API.
- Each audit event must include `eventType`, `actorId`, `resourceType`,
  `resourceId`, `payload`, and `timestamp`.
- Each audit event must store a hash for that event.
- Each audit event must store the immediately preceding record hash, or a
  defined genesis value for the first event, so that records form a verifiable
  chain.
- The service must support querying audit events with `actorId`,
  `resourceType`, `resourceId`, `eventType`, `from`, and `to` filters in any
  combination.
- Query results must support pagination.
- The service must expose `GET /audit/verify`.
- `GET /audit/verify` must verify the full audit-event hash chain.
- Full-chain verification must report the first inconsistency found.
- Full-chain verification must detect tampering after a record is changed
  directly in the database.

### Scenario B: Retention, Redaction, And Export

- The service must support configurable retention.
- Retention handling must allow archival or soft deletion.
- The service must support redaction of sensitive payload fields.
- Redaction must not break verification of audit history.
- The service must provide a verifiable export for one `actorId` or one
  `resourceId`.
- The export must contain all records for the selected `actorId` or
  `resourceId`.
- The export must be a self-contained bundle that can be independently
  verified.

### Scenario C: Regulator Access Audit

- Scenario C must be addressed through an implementation or a documented
  partial implementation with a clear scope boundary.
- Scenario C must clarify how authorized reviewers can audit access to client
  account data.
- Scenario C must connect regulator-facing or compliance-review evidence to the
  tamper-evident audit history.

### Cross-Cutting Assessment Requirements

- The repository must include a runnable setup for local development and
  validation.
- The API and data schema must be documented.
- Unit tests and integration tests must cover required behavior.
- Architecture decisions and design rationale must be documented before
  implementation decisions are locked in.
- Material AI-assisted work must be traceable in `docs/ai/AI_USAGE_LOG.md`.
- The final submission must include a final summary and attestation.

## Ambiguities And Open Questions

### Scenario A

- What sort order is required for paginated query results?
- What pagination model is expected: page/size, cursor, or another approach?
- What is the required response shape for create, query, and verification
  operations?
- Is `timestamp` caller-supplied, server-assigned, or should the service store
  both event time and recorded time?
- Should duplicate event submissions be rejected, accepted, or made idempotent?
- What is the expected behavior when appending an event while verification is
  running?
- What hash inputs are required, and how should canonicalization be specified?
- What exact inconsistency details may be returned without exposing sensitive
  payload data?
- What are the expected authorization requirements, if any, for append, query,
  export, verification, and compliance-review operations?

### Scenario B

- What retention periods are required, and are they global or tenant/resource
  specific?
- When should retention use archival versus soft deletion?
- Must archived or soft-deleted records remain queryable through the same API?
- Which payload fields are considered sensitive and redaction-eligible?
- Is redaction irreversible, reversible by privileged users, or represented as
  a later correction event?
- Should redaction preserve field names, value presence, value hashes, or only
  chain validity?
- What export bundle format is required?
- What proof material must be included so an export is independently
  verifiable?
- Should exports include archived, soft-deleted, and redacted records?

### Scenario C

- Which human and service access events count as access to client account data?
- Who can act as an authorized internal compliance reviewer?
- What filters beyond time range are required for the compliance report?
- What evidence must the reviewer be able to inspect?
- What payload fields must be hidden, redacted, summarized, or excluded from the
  report?
- What authentication and authorization model is expected for compliance
  reviewers?
- What audit trail is required for compliance-review activity itself?

## Assumptions Needing Review

- Audit event payloads may contain sensitive data and must be treated as
  confidential in logs, errors, tests, and documentation.
- Hash-chain verification must be deterministic across service restarts and
  database reads.
- Direct database modification includes changing at least one persisted field
  that participates in verification.
- Scenario B redaction and retention may require Scenario A to avoid hashing
  only the raw mutable payload value.
- Scenario B export verification may require Scenario A to define stable event
  ordering, canonical event representation, and chain metadata clearly.
- Scenario C may require Scenario A events to capture enough actor, resource,
  action, timestamp, and access-context metadata to support later compliance
  reporting.
- Authentication and authorization behavior is not yet defined and must not be
  invented during Scenario A implementation.
- Human-readable documentation can use synthetic examples, but no confidential
  assessment text, real customer data, secrets, or recruiter communication may
  be stored in the repository.

## Scenario B And C Design Impact On Scenario A

- Scenario B redaction can affect which event fields are included in the event
  hash and whether redacted values need independent proof of prior existence.
- Scenario B retention can affect whether chain verification must work across
  active, archived, and soft-deleted records.
- Scenario B verifiable export can affect event ordering, chain boundary
  metadata, export manifests, and proof material needed by external consumers.
- Scenario C compliance reporting can affect event metadata captured in
  Scenario A, especially actor identity, resource identity, account-data access
  action, request context, and timestamp precision.
- Scenario C privacy constraints can affect query, export, and report response
  fields, even for records that are valid in the chain.
- These impacts require design review before implementation; this document does
  not choose the technical solution.

## Acceptance Criteria

### Scenario A

- A caller can append audit events through the application API with the
  required business fields, and the service records `timestamp` according to
  the documented timestamp-ownership decision.
- Existing audit events cannot be updated or deleted through the application
  API.
- A caller can query events using `actorId`, `resourceType`, `resourceId`,
  `eventType`, `from`, and `to` filters in any combination.
- Query responses are paginated and have deterministic ordering.
- Each persisted event includes its own hash and previous-record hash metadata.
- `GET /audit/verify` verifies the full chain and succeeds for an untampered
  chain.
- `GET /audit/verify` reports the first inconsistent record when a verification
  failure is found.
- Direct database modification of a verification-participating record is
  detected by full-chain verification.
- Tests cover append-only behavior, hash-chain append, combined filtering,
  pagination, successful verification, and direct-database tamper detection.

### Scenario B

- Retention behavior is configurable.
- Records eligible for retention handling are archived or soft-deleted
  according to the approved design.
- Sensitive payload fields can be redacted according to approved rules.
- Full-chain verification remains intact after legitimate redaction.
- A caller can create a self-contained verifiable export containing all records
  for one `actorId` or one `resourceId`.
- The export can be independently verified from the bundle contents.
- Tests cover retention, archival or soft deletion, redaction without broken
  verification, and export verification.

### Scenario C

- Scenario C is addressed through implementation or documented partial
  implementation with a clear scope boundary.
- An authorized internal compliance reviewer can retrieve a time-bounded,
  filterable report of human and service access to client account data.
- The report minimizes sensitive payload data.
- The report connects results to the tamper-evident audit history.
- Tests or documented validation cover the implemented Scenario C scope.
- Any Scenario C limitations are included in the final engineering summary.
