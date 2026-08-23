# Final Engineering Summary

## 1. Overview

This assessment implements a tamper-evident audit log service using Java 21, Spring Boot, Spring Data JPA/Hibernate, PostgreSQL, Spring Security, Docker Compose, JUnit, and Testcontainers.

The implementation was developed incrementally from requirement analysis through architecture, implementation, testing, security review, and final manual validation.

The service covers all three assessment scenarios:

* Scenario A — append-only audit events, querying, hash-chain integrity, and verification
* Scenario B — retention, structured redaction, and verifiable export
* Scenario C — scoped client-account access compliance reporting

AI was used as an engineering accelerator throughout the process. Material AI-assisted work, refinements, tests, failures, and human-review decisions are recorded in `docs/ai/AI_USAGE_LOG.md`.

---

## 2. Implementation Approach

The service is implemented as a single Spring Boot application using a conventional layered MVC structure under:

```text
com.assessment.auditlog
```

The application separates controller, service, repository, entity, DTO, security, configuration, and exception-handling responsibilities.

A single deployable service was selected because the assessment scope does not require distributed components or multiple independently deployable services.

PostgreSQL is used for persistence, including structured JSON payloads through JSONB.

Hibernate schema generation is used for this time-boxed prototype. Versioned database migrations were considered but intentionally not introduced because the assessment focused primarily on audit integrity, redaction, validation, and engineering execution.

---

## 3. Scenario A — Core Audit Log

### Event Creation

The service exposes:

```text
POST /audit/events
```

Audit events contain:

* `eventType`
* `actorId`
* `resourceType`
* `resourceId`
* structured `payload`
* server-assigned UTC `timestamp`

The timestamp is assigned by the audit service rather than accepted from the caller. This avoids client-side timestamp manipulation and provides one consistent time source.

The trade-off is that the timestamp represents audit-service ingestion time. If an upstream request is delayed, it may differ from the original business-event occurrence time.

For a more distributed production system, both occurrence time and server-recorded time would be useful.

### Append-Only Persistence

Historical audit events are inserted through an explicit persist-only path.

The application exposes no update or delete API for audit events.

Each event receives a service-assigned increasing `BIGINT` ID that is also used for:

* chain ordering
* verification
* cursor pagination

### Hash Chain

Each event stores:

* `contentHash`
* `previousHash`
* `recordHash`
* `hashVersion`

`contentHash` is calculated from a deterministic canonical representation of:

* event type
* actor ID
* resource type
* resource ID
* committed payload
* timestamp

SHA-256 is used for hashing.

The first event references a defined genesis hash.

Each later event references the immediately preceding event's `recordHash`.

A global chain-state row stores the current chain head.

Concurrent append operations lock this row while allocating the next ID and updating the head. This serializes the small critical append section and prevents two concurrent requests from creating a fork.

This favors correctness over maximum append throughput.

For significantly higher production throughput, possible alternatives include partitioned chains, tenant-specific chains, a dedicated append coordinator, or other scalable integrity structures.

### Querying

The service exposes:

```text
GET /audit/events
```

Supported filters include:

* `actorId`
* `resourceType`
* `resourceId`
* `eventType`
* `from`
* `to`

Filters can be combined.

Cursor pagination uses:

```text
afterId
limit
```

Results are ordered by ID ascending.

Cursor pagination was selected instead of page/offset pagination because the audit log is append-only and already has a stable increasing ID. It avoids large offsets and remains stable while new records are appended.

### Chain Verification

The service exposes:

```text
GET /audit/verify
```

Verification checks:

1. ID continuity
2. supported hash version
3. recalculated content hash
4. previous-hash linkage
5. recalculated record hash
6. captured chain-head consistency

Verification runs against a PostgreSQL repeatable-read snapshot and does not hold the append lock for the entire scan.

The process stops at the first inconsistency and returns the inconsistent record ID and violation type.

A broken chain is returned as a verification result with HTTP `200`, rather than as an API failure.

---

## 4. Scenario B — Retention, Redaction, and Export

### Retention

Retention is implemented using separate archive-marker records.

The original audit event is never changed or deleted.

Records older than the configured retention window can be marked archived.

Normal audit queries exclude archived records, while:

* chain verification
* exports
* compliance reporting

continue to include the required historical records.

This avoids false chain failures caused by legitimate retention behavior.

The prototype performs logical archival only. A production system with very large history could move archived data to controlled cold or WORM storage while preserving integrity evidence.

### Sensitive-Field Redaction

Sensitive payload paths may be identified during event creation using JSON Pointer paths.

Before hashing and persistence:

1. A random AES-256 field key is generated.
2. The sensitive value is encrypted using AES-GCM.
3. The encrypted representation is stored in the committed payload.
4. The field key is protected using a separately configured master key.
5. The encrypted committed payload is included in the event hash.

Normal authorized reads return the logical decrypted value while the corresponding field key exists.

Redaction removes the decryptable key material but does not alter the committed encrypted payload.

After redaction, the API returns a redacted marker such as:

```json
{
  "redacted": true
}
```

Because the committed payload remains unchanged, hash-chain verification remains valid.

Additional fail-closed validation prevents corrupted or incomplete key metadata from exposing internal encrypted representations.

Key-management limitations are documented. A production implementation should use managed KMS/HSM infrastructure, key rotation, and backup-aware cryptographic erasure.

### Verifiable Export

The service exposes:

```text
GET /audit/exports
```

Exactly one selector is required:

* `actorId`
* `resourceId`

The exported bundle contains:

* matching committed audit records
* chain-proof metadata
* captured chain-head information
* SHA-256 bundle digest
* Ed25519 digital signature

Sensitive logical plaintext is not exported.

The export is created from a stable database snapshot and is refused if the source chain is already broken.

An independent verifier can validate the bundle using a trusted public key supplied outside the bundle.

The prototype includes full chain-proof headers from genesis through the captured head. This is simple and independently verifiable but can become large.

For large-scale systems, Merkle proofs or signed periodic checkpoints would provide more compact evidence.

---

## 5. Scenario C — Compliance Reporting

The original requirement that regulators need to audit access to client-account data was intentionally ambiguous.

The implemented scope was normalized to an internal compliance-review capability.

The service exposes:

```text
GET /audit/compliance/client-account-access
```

The report:

* requires a time range
* supports actor, resource, event-type, and cursor filters
* returns only `CLIENT_ACCOUNT` events
* includes archived historical records
* supports both human and service actor identifiers
* validates the captured audit chain before returning evidence
* excludes raw and committed payload contents

The report provides integrity references such as event and chain hashes.

The implemented scope proves that relevant events stored by this service have not been modified.

It does not prove that every upstream system submitted every required client-account access event.

Regulator identity integration, regulator-facing UI, scheduled delivery, jurisdiction-specific formats, and automated external submission were intentionally scoped out.

---

## 6. Security and Validation

The service uses stateless Spring Security HTTP Basic authentication with endpoint-level role authorization.

Configured users and roles are validated during startup.

Security controls include:

* role-based endpoint authorization
* input length and collection-size limits
* generic `ProblemDetail` API errors
* no historical audit-event update/delete API
* environment-backed secret configuration
* fail-closed cryptographic configuration
* AES-GCM encryption for sensitive fields
* Ed25519 export signing
* no sensitive payload/key material in API errors
* no committed local `.env`, keys, certificates, or credentials

HTTP Basic is appropriate for the prototype but requires TLS in production.

For a production deployment, centralized OAuth2/OIDC identity, managed secrets, TLS enforcement, and stronger monitoring would be preferred.

---

## 7. Testing and Final Validation

Testing includes both focused unit tests and PostgreSQL integration tests using Testcontainers.

Coverage includes:

* deterministic canonicalization and hashing
* append-only event creation
* concurrent chain append
* query filtering
* cursor pagination
* authorization
* full-chain verification
* direct database tampering
* retention
* sensitive-field encryption
* redaction
* invalid key-state handling
* export signing
* independent export verification
* compliance reporting
* security configuration
* input validation
* generic error handling

The complete automated test suite passed after final security hardening.

In addition to automated tests, I manually validated the running application using Postman.

Manual validation included:

* authenticated and unauthorized requests
* audit-event creation
* visible previous-hash chaining
* combined querying
* cursor pagination
* time filtering
* intact-chain verification
* sensitive-value reads before redaction
* redacted-value reads after redaction
* verification after redaction
* retention execution
* signed export generation
* client-account compliance reporting
* role authorization
* direct PostgreSQL modification of a historical event
* successful detection of the first inconsistent record
* refusal of export/compliance evidence generation after chain corruption

The manual database-tampering workflow closely matches the expected assessment validation flow.

---

## 8. Key Trade-offs and Limitations

### Global Chain Serialization

A single global chain-state lock keeps chain ordering simple and correct but serializes concurrent append operations.

Higher-scale implementations could partition chains or introduce a specialized append architecture.

### Full Verification Cost

Full-chain verification is O(n).

For larger histories, signed checkpoints, segmented verification, or Merkle-based structures could reduce verification cost.

### Logical Retention

Archive markers preserve integrity but do not reduce the physical size of the audit-event table.

Production archival could move records to controlled historical storage.

### Cryptographic Key Management

Local/environment-managed keys are sufficient for the prototype.

Production should use managed KMS/HSM infrastructure, rotation policies, restricted key access, and backup-aware destruction.

### Export Proof Size

The export contains proof headers across the captured chain, which may become large.

Merkle inclusion proofs or trusted signed checkpoints could provide more compact verification.

### Database Schema Management

Hibernate schema generation was used for this prototype.

A long-lived production application should use reviewed, versioned migrations.

### Authentication

HTTP Basic keeps the prototype self-contained but requires TLS and long-lived credentials.

Production should use short-lived tokens through centralized identity infrastructure.

### Request Size

Individual fields and collections are bounded, but a complete HTTP request-body size policy was not implemented.

A production deployment should enforce request-size limits at the gateway/server and, where appropriate, application layers.

---

## 9. AI-Assisted Engineering

AI was used for:

* requirement normalization
* task decomposition
* architecture review
* implementation
* debugging
* test generation
* security review
* documentation
* review preparation

AI-generated changes were treated as proposals rather than automatically accepted output.

Material AI usage is documented in:

```text
docs/ai/AI_USAGE_LOG.md
```

The engineer retained ownership of:

* requirements and scope
* architecture decisions
* acceptance, modification, or rejection of AI output
* code review
* validation
* security decisions
* Git commits
* production-readiness assessment
* final submission sign-off

---

## 10. Final State

All three scenarios have been implemented within the documented scope.

The automated test suite and manual end-to-end API validation completed successfully, including direct datastore tamper detection.

The repository is ready for the final commit, push, and submission to the review panel.