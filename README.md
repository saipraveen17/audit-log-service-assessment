# Audit Log Service Assessment

This repository contains an individual implementation of a confidential software
engineering assessment for an audit log service.

## Status

The service implements all three assessment scenarios, including append-only
audit events, tamper-evident hash-chain verification, retention, structured
redaction, verifiable export, and client-account access compliance reporting.

Security review is complete. Final manual validation and submission
documentation remain pending.

## Working Context

Repository work is governed by:

* `AGENTS.md` for permanent engineering, security, Git, and AI usage rules.
* `docs/REQUIREMENTS.md` for normalized requirements.
* `docs/ARCHITECTURE.md` and `docs/adr/` for approved design decisions.
* `docs/TASK_PLAN.md` for implementation sequencing.

Detailed API behavior, data model, testing strategy, trade-offs, and AI
traceability are available under `docs/`.

## Approved Technical Direction

The service uses:

* Java 21
* Spring Boot
* Maven with Maven Wrapper
* Spring Data JPA and Hibernate
* PostgreSQL
* JUnit 5 and Mockito
* Testcontainers
* Docker Compose for local dependencies and execution

## Local Development

Prerequisites:

* Java 21
* Docker with Docker Compose
* OpenSSL

Create local environment values from the example file:

```bash
cp .env.example .env
```

`.env` is used only for local configuration and is ignored by Git. Do not commit
it.

For local development, the application automatically reads `.env` from the
repository root. Real environment variables take precedence over `.env` values.

### Local Authentication

Replace the HTTP Basic password placeholders in `.env`.

For disposable local testing, a password may use Spring Security's `{noop}`
encoder:

```text
AUDIT_SECURITY_USERS_0_PASSWORD={noop}AuditTest123!
```

The corresponding plaintext password used in Postman or `curl` is:

```text
AuditTest123!
```

`{noop}` is intended only for local testing. Deployed environments should use
strong password encoding and TLS.

### Redaction Master Key

Generate a synthetic 32-byte AES master key:

```bash
openssl rand -base64 32 | tr -d '\n'
```

Set the output in `.env`:

```text
AUDIT_REDACTION_MASTER_KEY_BASE64=<generated-value>
```

The application fails startup if the configured redaction key is missing,
invalid Base64, or does not decode to exactly 32 bytes.

### Export Signing Key

Generate a local Ed25519 private key:

```bash
openssl genpkey -algorithm ED25519 \
  -out /tmp/audit-export-ed25519-private.pem
```

Convert it to Base64 PKCS#8 DER:

```bash
openssl pkcs8 \
  -topk8 \
  -nocrypt \
  -in /tmp/audit-export-ed25519-private.pem \
  -outform DER \
  | base64 -w0
```

Set the output in `.env`:

```text
AUDIT_EXPORT_SIGNING_KEY_ID=local-validation-key-1
AUDIT_EXPORT_PRIVATE_KEY_BASE64=<generated-private-key>
```

To obtain the corresponding public key for independent verification:

```bash
openssl pkey \
  -in /tmp/audit-export-ed25519-private.pem \
  -pubout \
  -outform DER \
  | base64 -w0
```

The private key must not be committed. The public key should be distributed to
offline verifiers through a trusted channel.

## Start PostgreSQL

```bash
docker compose up -d postgres
```

Check the container:

```bash
docker compose ps
```

## Run Tests

```bash
./mvnw test
./mvnw verify
```

## Run the Application

```bash
./mvnw spring-boot:run
```

The service runs at:

```text
http://localhost:8080
```

The same local `.env` configuration can be used when running
`AuditLogApplication` directly from IntelliJ, provided the working directory is
the repository root.

No Spring profile is required.

The application uses PostgreSQL settings from environment variables with safe
local defaults. Local database credentials are development-only and must be
replaced for deployed environments.

Open Session in View is disabled, timestamps are stored in UTC, and SQL
parameter logging is disabled.

API errors use generic `ProblemDetail` responses and do not echo payload values,
credentials, key material, ciphertext, or internal exception details.

HTTP Basic authentication must be used behind TLS/HTTPS in a production
deployment.

## AI Assistance

Material AI-assisted work is recorded in `docs/ai/AI_USAGE_LOG.md`.

The engineer remains responsible for requirement interpretation, architecture
approval, code review, testing, Git commits, production-readiness decisions, and
final submission validation.
