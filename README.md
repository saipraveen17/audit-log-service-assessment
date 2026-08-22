# Audit Log Service Assessment

This repository contains an individual implementation of a confidential software
engineering assessment for an audit log service.

## Status

Initial repository structure is being prepared. System requirements,
architecture decisions, and implementation tasks will be documented before
application code is added.

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

Build, test, and run commands will be added after the project skeleton is
created.

## AI Assistance

Material AI-assisted work is recorded in `docs/ai/AI_USAGE_LOG.md`. Human review
and final validation remain pending until explicitly completed by the engineer.
