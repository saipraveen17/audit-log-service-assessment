# ADR-003: Signed Export, Prototype Security, And Compliance Reporting

## Status

Accepted for the prototype.

## Decision

### Export

Create an Ed25519-signed JSON bundle for exactly one `actorId` or one
`resourceId`. The bundle contains selected full committed records, chain proof
headers from genesis through a captured head, manifest, digest, and signature.
The signing private key is supplied through environment configuration. The
trusted public verification key must be distributed outside the bundle.

### Authentication And Authorization

Use stateless Spring Security HTTP Basic with environment-provided credentials.

| Endpoint | Allowed roles |
| --- | --- |
| `POST /audit/events` | `AUDIT_WRITER`, `AUDIT_ADMIN` |
| `GET /audit/events` | `AUDIT_READER`, `AUDIT_ADMIN` |
| `GET /audit/verify` | `AUDIT_VERIFIER`, `AUDIT_ADMIN` |
| `POST /audit/retention/run` | `AUDIT_ADMIN` |
| `POST /audit/events/{id}/redactions` | `AUDIT_ADMIN` |
| `GET /audit/exports` | `COMPLIANCE_REVIEWER`, `AUDIT_ADMIN` |
| `GET /audit/compliance/client-account-access` | `COMPLIANCE_REVIEWER`, `AUDIT_ADMIN` |

Spring Security adds the standard `ROLE_` prefix internally. CSRF is disabled
for this stateless non-browser API. TLS is required in production.

### Scenario C

Provide a time-bounded, cursor-paginated internal compliance report for events
where `resourceType = CLIENT_ACCOUNT`. It supports actor, resource, and event-type
filters, includes archived history, excludes raw payloads, and returns chain
references and the captured chain head.

The report proves integrity of events stored by this service, not completeness
of upstream event capture.

## Alternatives Considered

- unsigned JSON or CSV export
- checksum-only bundles
- HMAC-signed exports
- Merkle inclusion proofs or checkpoint-only proofs
- custom API-key authentication
- OAuth2/OIDC with an external identity provider
- regulator-facing UI or scheduled external delivery
- claiming proof that every upstream access was captured

## Rationale

A checksum can be replaced together with a modified bundle. An Ed25519 signature
provides strong post-export alteration detection and allows independent public-key
verification. Complete proof headers are straightforward to explain and validate
for a prototype.

HTTP Basic is built into Spring Security and demonstrates explicit role
boundaries without adding identity infrastructure. Environment-provided
credentials avoid committing secrets.

The Scenario C scope delivers a concrete report without inventing regulator
identity integration, legal formats, or cross-system completeness guarantees.

## Trade-Offs And Limitations

- proof headers from genesis can make exports large
- public-key trust depends on a separate trusted distribution channel
- HTTP Basic is safe only over TLS and lacks short-lived credentials
- local key and credential management is limited
- the compliance report excludes payload detail and does not prove upstream
  capture completeness
- compliance-report requests are not themselves audited in this prototype

## Production Evolution

With more time or stronger requirements:

- use KMS/HSM-backed signing keys and published certificate/key history
- replace full proof headers with Merkle or checkpoint proofs
- use OAuth2/OIDC, JWT scopes, short-lived credentials, and centralized identity
- add secrets-manager integration and credential rotation
- audit compliance-report access
- add scheduled, jurisdiction-specific, securely delivered reports only when
  explicitly required
- add upstream instrumentation controls when event-capture completeness must be
  demonstrated