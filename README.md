# Audit Log Service Assessment

This repository contains an individual implementation of a confidential software
engineering assessment for an audit log service.

## Status

The repository now contains the baseline Spring Boot project setup, approved
architecture documentation, authenticated audit event creation, transactional
hash-chain append, the audit-event query API, full-chain verification, and
configurable retention using archive markers, plus sensitive-field encryption
and key-removal redaction. Export and compliance reporting remain pending.

## Working Context

Repository work is governed by:

- `AGENTS.md` for permanent engineering, security, Git, and AI usage rules.
- `docs/REQUIREMENTS.md` for normalized requirements when available.
- `docs/ARCHITECTURE.md` and `docs/adr/` for approved design decisions when
  available.
- `docs/TASK_PLAN.md` for implementation sequencing when available.

If those documents are missing, work should stay limited to the active task
prompt and should not invent requirements or design decisions.

## Approved Technical Direction

Unless later approved architecture decisions change it, the service will use:

- Java 21
- Spring Boot
- Maven with Maven Wrapper
- Spring Data JPA and Hibernate
- PostgreSQL
- JUnit 5 and Mockito
- Docker Compose for local dependencies and execution

## Local Development

Prerequisites:

- Java 21
- Docker with Docker Compose

Create local environment values from the example file:

```bash
cp .env.example .env
set -a
source .env
set +a
```

Replace placeholder HTTP Basic password hashes in `.env` before running the
application. Also replace `AUDIT_REDACTION_MASTER_KEY_BASE64` with a valid
Base64-encoded 32-byte key before startup. Do not commit `.env`.

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run tests:

```bash
./mvnw test
./mvnw verify
```

Run the application:

```bash
./mvnw spring-boot:run
```

The application uses PostgreSQL settings from environment variables with safe
local defaults. Open Session in View is disabled, timestamps are stored in UTC,
and SQL parameter logging is disabled.

## AI Assistance

Material AI-assisted work is recorded in `docs/ai/AI_USAGE_LOG.md`. Human review
and final validation remain pending until explicitly completed by the engineer.
