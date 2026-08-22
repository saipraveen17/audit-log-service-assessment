# Data Model

## Overview

The service uses PostgreSQL with Spring Data JPA and Hibernate schema
generation. Audit-event IDs are manually assigned by the service while holding
the global chain-state lock.

## `audit_event`

Append-only audit records.

| Field | Database shape | Purpose |
| --- | --- | --- |
| `id` | `BIGINT` primary key | Event identity, global chain position, verification reference, and cursor |
| `event_type` | `VARCHAR` not null | Caller-provided event type |
| `actor_id` | `VARCHAR` not null | Caller-provided actor identifier |
| `resource_type` | `VARCHAR` not null | Caller-provided resource type |
| `resource_id` | `VARCHAR` not null | Caller-provided resource identifier |
| `payload` | `JSONB` not null | Committed structured payload; sensitive values are stored as encrypted envelopes |
| `timestamp` | `TIMESTAMPTZ` not null | Server-assigned UTC ingestion time |
| `content_hash` | `CHAR(64)` not null | SHA-256 of canonical event content |
| `previous_hash` | `CHAR(64)` not null | Prior event's record hash or genesis hash |
| `record_hash` | `CHAR(64)` not null | SHA-256 binding content and chain metadata |
| `hash_version` | `INTEGER` not null | Canonicalization/hash format version |

Recommended indexes:

- primary key on `id`
- `(actor_id, id)`
- `(resource_type, resource_id, id)`
- `(event_type, id)`
- `(timestamp, id)`

The application exposes no update or delete operation for this table.

## `audit_chain_state`

Singleton row coordinating the global linear chain.

| Field | Database shape | Purpose |
| --- | --- | --- |
| `name` | `VARCHAR` primary key | Constant value `GLOBAL` |
| `last_id` | `BIGINT` not null | Last appended event ID |
| `last_record_hash` | `CHAR(64)` not null | Record hash for `last_id`, or genesis hash before the first append |

Initial state:

```text
name = GLOBAL
last_id = 0
last_record_hash = SHA-256("AUDIT_LOG_GENESIS_V1")
```

The append transaction loads this row with a pessimistic write lock. Audit event
IDs must not use `@GeneratedValue` or a database sequence.

## Committed Sensitive-Field Envelope

A sensitive value is replaced before hashing with an envelope such as:

```json
{
  "_encrypted": {
    "keyId": "d05b3c9f-45cc-4b0b-a734-fac3f7c24239",
    "algorithm": "AES-256-GCM",
    "iv": "base64-iv",
    "ciphertext": "base64-ciphertext"
  }
}
```

The AES-GCM authentication tag is included in the cipher output. The committed
payload containing this envelope is the only representation used for hashing,
verification, and export.

## `audit_sensitive_field_key`

Key metadata is separate from the immutable event row.

| Field | Database shape | Purpose |
| --- | --- | --- |
| `key_id` | UUID primary key | Identifier referenced by the committed envelope |
| `audit_event_id` | `BIGINT` not null | Event containing the protected field |
| `json_pointer` | `VARCHAR` not null | Sensitive JSON Pointer path |
| `wrapped_key` | binary nullable | Per-field AES key wrapped by the configured master key; cleared on redaction |
| `wrapping_iv` | binary nullable | IV used to wrap the field key; cleared on redaction |
| `created_at` | `TIMESTAMPTZ` not null | Key creation time |
| `redacted_at` | `TIMESTAMPTZ` nullable | Redaction time |
| `redaction_reason` | `VARCHAR` nullable | Safe reason code or text |
| `redacted_by` | `VARCHAR` nullable | Authenticated administrator identity |

Unique constraint:

```text
(audit_event_id, json_pointer)
```

Before redaction, authorized event reads may unwrap the field key and return the
logical plaintext value. After `wrapped_key` is cleared, the response shows a
redacted marker. Key material must never be logged or returned.

## `audit_archive_marker`

Logical retention metadata.

| Field | Database shape | Purpose |
| --- | --- | --- |
| `audit_event_id` | `BIGINT` primary key | Archived event |
| `archived_at` | `TIMESTAMPTZ` not null | Marker creation time |
| `retention_days` | `INTEGER` not null | Policy value used for the run |
| `reason` | `VARCHAR` not null | Safe operational reason |

One marker per event makes retention idempotent. The original event row remains
unchanged.

## Hash Inputs

### `contentHash`

SHA-256 over canonical:

```text
eventType
actorId
resourceType
resourceId
committed payload
timestamp
```

### `previousHash`

The preceding record's `recordHash`; the first record uses:

```text
SHA-256("AUDIT_LOG_GENESIS_V1")
```

### `recordHash`

SHA-256 over canonical:

```text
hashVersion
id
contentHash
previousHash
```

## Canonicalization

- UTF-8
- fixed top-level field ordering
- recursively sorted JSON object keys
- preserved array order
- stable numbers, booleans, strings, and nulls
- UTC timestamps truncated to milliseconds
- no insignificant whitespace
- lowercase hexadecimal SHA-256 output

The hash is computed from application canonical bytes, not the textual JSONB
representation read from PostgreSQL.

## Export Bundle Model

The export response is not a database entity. It contains:

- `manifest`
- selected full committed records
- proof headers for all IDs from genesis through the captured head
- `bundleDigest`
- Ed25519 `signature`

Each proof header contains:

```text
id
contentHash
previousHash
recordHash
hashVersion
```

The public verification key is distributed through a trusted channel outside
the bundle.