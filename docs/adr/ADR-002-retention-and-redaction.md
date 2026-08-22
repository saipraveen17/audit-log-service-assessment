# ADR-002: Archive Markers And Cryptographic Redaction

## Status

Accepted for the prototype.

## Decision

Use a separate archive-marker table for retention. The global retention window
is configurable, marker creation is idempotent, and original audit-event rows
are never updated or deleted. Normal event queries exclude archived records;
verification, export, and compliance reporting include them.

Protect sensitive payload fields at event creation using per-field AES-256-GCM
encryption. The create request identifies eligible fields with JSON Pointer
`sensitivePaths`. The committed payload stores encrypted envelopes, while each
field key is wrapped by an environment-provided master key and stored
separately.

Redaction clears the wrapped key material and records non-secret metadata such
as path, time, reason, and administrator. The committed encrypted payload remains
unchanged, so verification remains intact.

## Alternatives Considered

- update an `archived` flag on the audit event
- physically delete or move historical rows
- replace sensitive values in JSONB after creation
- store only a hash of sensitive values
- response-layer masking without key destruction
- accept sensitive paths only at redaction time
- use an external KMS/HSM immediately

## Rationale

Archive markers preserve the immutable chain input and avoid false verification
breaks. They also provide a clear distinction between business history and
retention state.

Plaintext removal would invalidate the original content hash. Encrypting before
hashing means the immutable ciphertext remains verifiable, while destroying the
separate key makes the logical value unavailable. AES-GCM supplies confidentiality
and authenticated encryption.

## Trade-Offs And Limitations

- logical archival does not reduce primary table size
- ciphertext remains stored after redaction
- fields must be classified during event creation
- redaction strength depends on preventing copies of field keys
- backup lifecycle can retain older usable key material
- application-managed master-key handling is weaker than managed KMS/HSM
- authorized readers can view sensitive values before redaction

## Production Evolution

With stronger requirements:

- use KMS/HSM-backed envelope encryption and key rotation
- add backup-aware cryptographic-erasure procedures
- enforce separation of duties and approval workflows for redaction
- add auditable key lifecycle operations
- use policy/schema-based sensitive-field classification
- move archived data to cold or WORM storage while preserving signed checkpoints