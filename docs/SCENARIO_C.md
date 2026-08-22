# Scenario C: Regulator Audit Clarification

## Purpose

Scenario C must be addressed before final submission. The working scope below
is approved for architecture and implementation planning. Specific technical
details may still be refined through architecture decisions and ADRs.

The result must include either an implementation or a documented partial
implementation with a clear and justified scope boundary.

## Clarification Questions

- Which client account data access events must be captured?
- What actor identifiers are required for human users and service actors?
- What resource identifiers are required for client accounts and related data?
- What access actions must be distinguishable, such as view, search, export,
  create, update, delete, or administrative access?
- Should failed or denied access attempts be included with successful access?
- Who can act as an authorized internal compliance reviewer?
- What filters must the compliance reviewer be able to use?
- What time range, timezone, and timestamp precision are required?
- What evidence must be included to prove report rows are tied to
  tamper-evident audit history?
- What payload fields must be hidden, redacted, summarized, or excluded from
  compliance-review reports?
- What authentication and authorization model is expected for internal
  compliance reviewers?
- What audit trail is required for compliance-review activity itself?
- Does the compliance requirement require proof that every upstream access was
  captured, or only proof that events stored by this service have not been
  modified?

## Approved Working Requirement

The system must allow an authorized internal compliance reviewer to retrieve a
time-bounded, filterable report of human and service access to client-account
data.

The report must minimize sensitive payload data and include integrity references
that connect reported events to the tamper-evident audit history. The
implemented scope proves the integrity of events stored by this service; it
does not by itself prove that every upstream access was captured.

## Working Assumptions Approved For Design

These assumptions are approved as the starting point for architecture and may
be refined through documented design decisions.

- Client account data access should be represented as audit events with clear
  actor, resource, action, timestamp, and request-context metadata.
- Both human users and service actors can access client account data.
- Compliance-review reports should not expose complete sensitive payloads unless
  an approved requirement explicitly allows it.
- Scenario C depends on Scenario A verification and may reuse Scenario B export
  or redaction behavior.
- Reviewers need enough proof material to connect report results to the
  tamper-evident chain without requiring direct database access.
- No real client, customer, employee, or regulator data should be used in
  examples, tests, or documentation.

## Expected Implementation

Scenario C is expected to include:

- A documented model for client-account data access audit events.
- A compliance-review report scoped by a required time range.
- Filter support appropriate to the approved scope, expected to include at
  least actor, resource, and access action where data is available.
- A report response that excludes, redacts, or summarizes sensitive payload
  contents.
- Verification metadata or references that connect report rows to the
  tamper-evident audit history.
- Tests or documented validation proving the implemented scope is filterable,
  tied to verification evidence, and does not expose disallowed sensitive
  fields.
- Security review of access controls, report contents, logging, error
  responses, and retention behavior.

This section describes expected implementation areas only. It does not approve
or choose technical design.

## Out Of Scope

- Regulator-facing user interface.
- Scheduled report delivery.
- Regulator identity-provider integration.
- Jurisdiction-specific regulator formats.
- Automated submission to a regulator portal.
- Real legal or compliance policy interpretation.
- Real client account data, real customer records, or production-like personal
  information.
- Cross-system evidence collection outside this audit log service unless
  explicitly required.

## Design Impact On Earlier Scenarios

- Scenario A may need to capture sufficient metadata for account-data access
  events before Scenario C is implemented.
- Scenario B redaction and export behavior may support compliance-review
  evidence packages.
- Scenario A verification responses may need to avoid exposing payload contents
  while still identifying inconsistent records.
- The final technical solution must be deferred until the Scenario C scope is
  reviewed.
