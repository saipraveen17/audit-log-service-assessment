# Architecture

## Purpose

This document records the approved TASK-004 design for the tamper-evident audit
log service. It resolves the implementation choices left open during the
requirements phase and provides the baseline for application scaffolding and
feature work.

## System Shape

The prototype is one Java 21 Spring Boot service using a standard layered MVC
structure under `com.assessment.auditlog`:

- `controller` — HTTP endpoints and request/response mapping
- `service` — application workflows, hashing, verification, retention,
  redaction, export, and compliance reporting
- `repository` — Spring Data JPA persistence access
- `entity` — JPA entities
- `dto` — request and response models
- `security` — HTTP Basic authentication and endpoint authorization
- `config` — environment-driven settings and application wiring
- `exception` — consistent Spring `ProblemDetail` errors

This structure is intentionally simple for a small service and a time-boxed
assessment. A larger system could move to feature-oriented packages or separate
modules when team ownership and scale justify that complexity.

## API Boundaries

The prototype uses the assessment-facing routes directly:

- `POST /audit/events`
- `GET /audit/events`
- `GET /audit/verify`
- `POST /audit/retention/run`
- `POST /audit/events/{id}/redactions`
- `GET /audit/exports`
- `GET /audit/compliance/client-account-access`

No `/api` or `/v1` prefix is added because the assessment explicitly names
`GET /audit/verify`. API version `1.0.0` can be recorded in OpenAPI metadata.
Path or media-type versioning is a production evolution if incompatible versions
are introduced later.

## Persistence

The service uses PostgreSQL, Spring Data JPA, and Hibernate schema generation.
Flyway and Liquibase are intentionally not used in this prototype. Local
execution uses Hibernate schema update behavior; integration tests use a clean
PostgreSQL schema.

The audit-event payload is a structured JSON object stored as PostgreSQL JSONB.
`actorId` and `resourceId` are caller-provided opaque strings because upstream
systems may use usernames, numeric identifiers, UUIDs, or domain identifiers.

## Timestamp

The create API does not accept a timestamp. The service assigns one UTC
`timestamp` after request validation and before hashing. It represents audit
service ingestion time, not the exact database commit time.

- Java type: `Instant`
- API format: ISO-8601 UTC, for example `2026-08-22T10:15:30.123Z`
- Precision: milliseconds
- `from`: inclusive
- `to`: exclusive

This prevents client timestamp manipulation and provides a single clock for time
range queries. The accepted limitation is that a delayed request is recorded
later than the original business event. A stronger distributed design would
store both producer occurrence time and server recording time.

## Record Identity And Ordering

Each audit event uses one service-assigned `BIGINT id`. It is the:

- event identifier
- global chain position
- verification reference
- cursor for pagination

The service does not use `@GeneratedValue` or a database sequence for audit
event IDs. While holding the global chain-state row lock, it assigns:

```text
id = chainState.lastId + 1
```

This avoids rollback gaps and ordering ambiguity between database-generated IDs
and the linear hash chain.

## Hash Chain

Each audit event stores:

- `contentHash`
- `previousHash`
- `recordHash`
- `hashVersion`

`contentHash` is SHA-256 over a deterministic representation of:

```text
eventType
actorId
resourceType
resourceId
committed payload
timestamp
```

`previousHash` is the preceding event's `recordHash`. The first record uses:

```text
SHA-256("AUDIT_LOG_GENESIS_V1")
```

`recordHash` is SHA-256 over a deterministic representation of:

```text
hashVersion
id
contentHash
previousHash
```

The committed payload is the stored representation after any sensitive values
have been encrypted. Hashing never uses a decrypted response view.

Canonicalization uses UTF-8, fixed top-level field ordering, recursively sorted
JSON object keys, preserved array order, stable scalar formatting, no
insignificant whitespace, and UTC millisecond timestamps. `hashVersion` allows
the format to evolve later.

## Concurrent Appends

A singleton `audit_chain_state` row stores:

```text
name = GLOBAL
lastId = 0
lastRecordHash = genesis hash
```

An idempotent startup initializer creates that row when it does not exist.

The append workflow is transactional:

1. Validate the request.
2. Assign the server UTC timestamp.
3. Encrypt declared sensitive fields.
4. Build the committed payload and calculate `contentHash`.
5. Lock the `GLOBAL` chain-state row with a pessimistic write lock.
6. Assign `id = lastId + 1`.
7. Set `previousHash = lastRecordHash`.
8. Calculate `recordHash`.
9. Persist the event.
10. Update `lastId` and `lastRecordHash`.
11. Commit and release the lock.

Only concurrent appends serialize on this row; ordinary queries, cursor
pagination, compliance reporting, and export reads do not take the append lock.
The trade-off is limited global write throughput. Higher-scale alternatives are
partitioned chains, a dedicated append sequencer, optimistic head updates with
retry, or Merkle/checkpoint structures.

## Query And Pagination

`GET /audit/events` supports any combination of:

- `actorId`
- `resourceType`
- `resourceId`
- `eventType`
- `from`
- `to`

Cursor pagination uses:

```text
afterId
limit
ORDER BY id ASC
```

The first request omits `afterId`. The default limit is `50` and the maximum is
`200`. Cursor pagination is stable for an append-only dataset and avoids large
SQL offsets. The trade-off is that clients cannot jump directly to an arbitrary
page or obtain a cheap total-page count.

## Chain Verification

`GET /audit/verify` verifies a stable snapshot:

1. Capture `snapshotLastId` and `snapshotLastRecordHash` from chain state.
2. Read events with `id <= snapshotLastId` in ascending ID order.
3. Check ID continuity.
4. Recalculate each `contentHash`.
5. Check each `previousHash` against the prior calculated `recordHash`.
6. Recalculate each `recordHash`.
7. Compare the final calculated hash with the captured snapshot hash.
8. Stop at the first inconsistency.

The full scan does not hold the append lock. Events appended after the snapshot
are outside that verification run and do not cause a false head mismatch.

An empty chain is intact only when `snapshotLastId = 0` and the snapshot head is
the genesis hash.

Supported violation types include:

- `ID_GAP`
- `CONTENT_HASH_MISMATCH`
- `PREVIOUS_HASH_MISMATCH`
- `RECORD_HASH_MISMATCH`
- `CHAIN_HEAD_MISMATCH`
- `UNSUPPORTED_HASH_VERSION`

Verification responses identify the first inconsistent ID and violation type,
without exposing payload values.

## Retention

Retention uses a global `audit.retention-days` setting and a separate archive
marker table. An event is eligible when:

```text
event.timestamp < current UTC time - retentionDays
```

Retention inserts at most one marker per event, making repeated runs idempotent.
Original audit-event rows remain unchanged.

- Normal event queries exclude archived events.
- Verification includes archived events.
- Exports include archived matching events because the requirement asks for all
  matching records.
- Compliance reports include archived historical events.

This is logical archival and does not reduce the primary table size. A
production system could move old rows to cold or WORM storage while retaining
verifiable checkpoints.

## Structured Redaction

Create requests may supply optional JSON Pointer paths in `sensitivePaths`.
Before hashing, each selected field is protected with AES-256-GCM using a random
per-field data key. The committed payload replaces plaintext with an encrypted
envelope containing the key identifier, algorithm, IV, and ciphertext.

The field key is wrapped using an environment-provided master key and stored in
a separate key table. Redaction nulls or deletes the wrapped field-key material
while retaining non-secret audit metadata such as path, reason, time, and
administrator identity. The committed encrypted payload is never changed.

Response behavior:

- Before redaction, an authorized event reader receives the decrypted logical
  value.
- After redaction, the same path is returned as `{"redacted": true}`.
- Verification and export always use the unchanged committed encrypted payload.

Redaction is idempotent for already-redacted paths. A path that was not declared
sensitive at creation cannot be cryptographically redacted later without
changing committed content and therefore returns a safe conflict response.

Limitations include creation-time classification, backup lifecycle for old key
material, and application-managed key wrapping. Production hardening would use
KMS/HSM-backed keys, rotation, stronger separation of duties, and backup-aware
cryptographic erasure.

## Verifiable Export

`GET /audit/exports` requires exactly one selector: `actorId` or `resourceId`.
The service captures the current chain head and creates an Ed25519-signed JSON
bundle containing:

- manifest and selector
- selected full records using committed payloads
- chain proof headers from genesis through the captured head
- captured `lastId` and `lastRecordHash`
- bundle digest
- Ed25519 signature and signing-key identifier

An offline verifier:

1. Trusts a public verification key distributed outside the bundle.
2. Verifies the Ed25519 signature over the canonical bundle digest.
3. Recalculates content and record hashes for selected full records.
4. Confirms selected records match their proof headers.
5. Walks proof headers from genesis and checks ID continuity and hash linkage.
6. Confirms the final proof hash equals the captured chain head.

The proof is intentionally simple but can be large because it contains headers
for the complete chain snapshot. Merkle inclusion proofs or signed periodic
checkpoints are stronger scaling options.

## Authentication And Authorization

The prototype uses stateless Spring Security HTTP Basic. Credentials are loaded
from environment variables and must not be committed or logged. CSRF is disabled
because the API is stateless and does not use browser sessions. TLS is required
for production use.

| Endpoint | Allowed roles |
| --- | --- |
| `POST /audit/events` | `AUDIT_WRITER`, `AUDIT_ADMIN` |
| `GET /audit/events` | `AUDIT_READER`, `AUDIT_ADMIN` |
| `GET /audit/verify` | `AUDIT_VERIFIER`, `AUDIT_ADMIN` |
| `POST /audit/retention/run` | `AUDIT_ADMIN` |
| `POST /audit/events/{id}/redactions` | `AUDIT_ADMIN` |
| `GET /audit/exports` | `COMPLIANCE_REVIEWER`, `AUDIT_ADMIN` |
| `GET /audit/compliance/client-account-access` | `COMPLIANCE_REVIEWER`, `AUDIT_ADMIN` |

These are logical names. Spring Security represents them internally with the
standard `ROLE_` prefix.

A production service should use enterprise OAuth2/OIDC, short-lived tokens,
managed secrets, TLS, credential rotation, and centralized audit policy.

## Scenario C Scope

Scenario C is implemented as a time-bounded internal compliance report over
events where:

```text
resourceType = CLIENT_ACCOUNT
```

Required filters are `from` and `to`; optional filters are `actorId`,
`resourceId`, `eventType`, `afterId`, and `limit`. `eventType` represents the
access action, including denied or failed access when the source system records
such events.

The response includes identifiers, timestamp, archive state, hash references,
and the captured chain head, but excludes the raw payload. It includes human and
service actor IDs and archived history.

The report proves integrity of client-account access events stored by this
service. It does not prove that every upstream application submitted every
required event. Regulator login, UI, scheduled delivery, jurisdiction-specific
formats, automated submission, and auditing the report request itself are out of
scope.

## Error Handling And Logging

- Validate requests and query parameters with Jakarta Validation and explicit
  JSON checks.
- Use Spring `ProblemDetail` for consistent errors.
- A broken chain returns HTTP `200` with `intact=false`; it is a verification
  result, not a malformed request.
- Do not log raw payloads, credentials, field keys, master keys, signing keys, or
  sensitive values.
- Do not enable SQL parameter-value logging.

## Documentation Map

- `docs/API.md` — endpoint contracts
- `docs/DATA_MODEL.md` — entities, relationships, indexes, and hash inputs
- `docs/TESTING_STRATEGY.md` — unit, integration, security, and manual validation
- `docs/adr/ADR-001-hash-chain-and-concurrency.md`
- `docs/adr/ADR-002-retention-and-redaction.md`
- `docs/adr/ADR-003-export-security-and-compliance.md`