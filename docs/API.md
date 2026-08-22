# API Contract

## Conventions

- Routes use `/audit/...` directly; the prototype does not add `/api` or `/v1`.
- Timestamps use ISO-8601 UTC at millisecond precision.
- The server assigns `timestamp`; create requests do not accept it.
- `from` is inclusive and `to` is exclusive.
- Errors use Spring `ProblemDetail`.
- Raw sensitive values, credentials, encryption keys, and signing keys must not
  appear in error responses or logs.

## Authentication And Authorization

The API uses stateless HTTP Basic authentication.

| Endpoint | Allowed roles |
| --- | --- |
| `POST /audit/events` | `AUDIT_WRITER`, `AUDIT_ADMIN` |
| `GET /audit/events` | `AUDIT_READER`, `AUDIT_ADMIN` |
| `GET /audit/verify` | `AUDIT_VERIFIER`, `AUDIT_ADMIN` |
| `POST /audit/retention/run` | `AUDIT_ADMIN` |
| `POST /audit/events/{id}/redactions` | `AUDIT_ADMIN` |
| `GET /audit/exports` | `COMPLIANCE_REVIEWER`, `AUDIT_ADMIN` |
| `GET /audit/compliance/client-account-access` | `COMPLIANCE_REVIEWER`, `AUDIT_ADMIN` |

The names above are logical role names. Spring Security represents them
internally using the standard `ROLE_` prefix.

## POST /audit/events

Creates one append-only audit event.

### Request

```json
{
  "eventType": "CLIENT_ACCOUNT_VIEWED",
  "actorId": "employee-101",
  "resourceType": "CLIENT_ACCOUNT",
  "resourceId": "account-501",
  "payload": {
    "accountNumber": "1234567890",
    "purpose": "CUSTOMER_SUPPORT",
    "outcome": "SUCCESS"
  },
  "sensitivePaths": [
    "/accountNumber"
  ]
}
```

Rules:

- `eventType`, `actorId`, `resourceType`, `resourceId`, and `payload` are
  required.
- `payload` must be a JSON object.
- `sensitivePaths` is optional and uses JSON Pointer syntax.
- `timestamp`, IDs, and hashes are server-controlled.

### Response

`201 Created`

```json
{
  "id": 1,
  "eventType": "CLIENT_ACCOUNT_VIEWED",
  "actorId": "employee-101",
  "resourceType": "CLIENT_ACCOUNT",
  "resourceId": "account-501",
  "payload": {
    "accountNumber": "1234567890",
    "purpose": "CUSTOMER_SUPPORT",
    "outcome": "SUCCESS"
  },
  "timestamp": "2026-08-22T10:15:30.123Z",
  "contentHash": "hex-content-hash",
  "previousHash": "hex-genesis-or-previous-hash",
  "recordHash": "hex-record-hash",
  "hashVersion": 1,
  "archived": false
}
```

The response exposes a logical payload view. Sensitive fields are decrypted only
while their separately stored key material exists. The committed encrypted
payload is used for hashing and persistence.

Possible status codes: `201`, `400`, `401`, `403`.

## GET /audit/events

Returns non-archived events matching any combination of:

- `actorId`
- `resourceType`
- `resourceId`
- `eventType`
- `from`
- `to`

Pagination parameters:

- `afterId` — exclusive cursor; omitted for the first request
- `limit` — default `50`, maximum `200`

Ordering is always `id ASC`.

### Example

```text
GET /audit/events?resourceType=CLIENT_ACCOUNT&from=2026-08-22T00:00:00.000Z&to=2026-08-23T00:00:00.000Z&limit=50
```

`200 OK`

```json
{
  "items": [
    {
      "id": 1,
      "eventType": "CLIENT_ACCOUNT_VIEWED",
      "actorId": "employee-101",
      "resourceType": "CLIENT_ACCOUNT",
      "resourceId": "account-501",
      "payload": {
        "accountNumber": "1234567890",
        "purpose": "CUSTOMER_SUPPORT",
        "outcome": "SUCCESS"
      },
      "timestamp": "2026-08-22T10:15:30.123Z",
      "recordHash": "hex-record-hash",
      "archived": false
    }
  ],
  "nextCursor": 1,
  "hasMore": false
}
```

After legitimate redaction, the same sensitive field is rendered as:

```json
{
  "accountNumber": {
    "redacted": true
  }
}
```

Possible status codes: `200`, `400`, `401`, `403`.

## GET /audit/verify

Verifies a snapshot of the complete chain and stops at the first inconsistency.

### Intact response

`200 OK`

```json
{
  "intact": true,
  "verifiedRecordCount": 42,
  "snapshotLastId": 42,
  "snapshotLastRecordHash": "hex-head",
  "firstInconsistentId": null,
  "violationType": null
}
```

### Broken response

`200 OK`

```json
{
  "intact": false,
  "verifiedRecordCount": 16,
  "snapshotLastId": 42,
  "snapshotLastRecordHash": "hex-head",
  "firstInconsistentId": 17,
  "violationType": "CONTENT_HASH_MISMATCH"
}
```

A broken chain is returned as a successful verification response rather than an
HTTP error. Authentication failures still return `401` or `403`.

## POST /audit/retention/run

Creates archive markers for eligible events using the configured global
retention window.

`200 OK`

```json
{
  "retentionDays": 90,
  "archivedCount": 12,
  "alreadyArchivedCount": 3
}
```

The operation is idempotent. It does not update or delete audit-event rows.

Possible status codes: `200`, `400`, `401`, `403`.

## POST /audit/events/{id}/redactions

Irreversibly removes the wrapped field-key material required to decrypt selected
sensitive fields.

### Request

```json
{
  "paths": [
    "/accountNumber"
  ],
  "reason": "DATA_PRIVACY_REQUEST"
}
```

### Response

`200 OK`

```json
{
  "id": 1,
  "redactedPaths": [
    "/accountNumber"
  ],
  "alreadyRedactedPaths": [],
  "payloadChanged": false
}
```

Rules:

- Redaction changes key metadata, not the committed encrypted payload.
- Repeating an approved redaction is idempotent.
- A path not declared sensitive during creation returns `409 Conflict`.
- After redaction, normal event responses show `{"redacted": true}` for the
  field.
- Full-chain verification remains intact.

Possible status codes: `200`, `400`, `401`, `403`, `404`, `409`.

## GET /audit/exports

Creates a self-contained verifiable bundle for exactly one selector.

```text
GET /audit/exports?actorId=employee-101
```

or:

```text
GET /audit/exports?resourceId=account-501
```

Supplying neither selector or both selectors returns `400 Bad Request`.

`200 OK`

```json
{
  "manifest": {
    "bundleVersion": 1,
    "selectorType": "actorId",
    "selectorValue": "employee-101",
    "exportedAt": "2026-08-22T12:00:00.000Z",
    "snapshotLastId": 42,
    "snapshotLastRecordHash": "hex-head",
    "selectedRecordCount": 4,
    "hashAlgorithm": "SHA-256",
    "signatureAlgorithm": "Ed25519",
    "signingKeyId": "local-export-key-1"
  },
  "selectedRecords": [],
  "chainProofHeaders": [],
  "bundleDigest": "hex-digest",
  "signature": "base64-signature"
}
```

Exports include archived matching records. Selected records contain committed
encrypted payloads, never deleted plaintext.

Possible status codes: `200`, `400`, `401`, `403`, `409`.

## GET /audit/compliance/client-account-access

Returns a time-bounded report for events where `resourceType` is
`CLIENT_ACCOUNT`.

Required:

- `from`
- `to`

Optional:

- `actorId`
- `resourceId`
- `eventType`
- `afterId`
- `limit`

The report includes archived history and excludes raw payloads.

`200 OK`

```json
{
  "from": "2026-08-22T10:00:00.000Z",
  "to": "2026-08-22T12:00:00.000Z",
  "snapshotLastId": 42,
  "snapshotLastRecordHash": "hex-head",
  "items": [
    {
      "id": 1,
      "eventType": "CLIENT_ACCOUNT_VIEWED",
      "actorId": "employee-101",
      "resourceId": "account-501",
      "timestamp": "2026-08-22T10:15:30.123Z",
      "contentHash": "hex-content-hash",
      "recordHash": "hex-record-hash",
      "archived": false
    }
  ],
  "nextCursor": 1,
  "hasMore": false
}
```

This report proves integrity of events stored by this service. It does not prove
that every upstream application submitted every required access event.

Possible status codes: `200`, `400`, `401`, `403`, `409`.

## ProblemDetail Example

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "limit must be between 1 and 200",
  "instance": "/audit/events"
}
```
