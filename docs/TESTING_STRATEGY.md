# Testing Strategy

## Approach

Testing is risk-based. Unit tests cover deterministic algorithms and validation;
PostgreSQL integration tests cover HTTP, Spring configuration, persistence,
transactions, locking, JSONB, authorization, and direct database tampering.
H2 is not used for database-dependent behavior.

## Unit Tests

Priority unit tests:

- server-assigned UTC timestamp using an injected `Clock`
- request validation and JSON-object payload checks
- JSON Pointer validation
- deterministic canonicalization with different object-key order
- genesis hash calculation
- `contentHash` and `recordHash` calculation
- changed event field produces a different content hash
- cursor parameter validation
- verification failure classification
- empty-chain verification
- retention eligibility
- AES-GCM encrypt/decrypt and authentication failure on changed ciphertext
- redaction behavior when key material is missing
- export digest and Ed25519 signature verification
- `ProblemDetail` mapping without sensitive data
- Scenario C response shaping without payload disclosure

## PostgreSQL Integration Tests

Critical integration flows:

1. **Append and chain creation**
  - initialize the `GLOBAL` chain-state row
  - create events through the API
  - assign contiguous service IDs
  - persist correct previous and record hashes

2. **Concurrent append**
  - submit multiple writes concurrently
  - assert unique contiguous IDs
  - assert every event points to the immediately preceding record hash

3. **Filtering and cursor pagination**
  - exercise each required filter independently and in combination
  - verify `afterId`, `limit`, and ascending ordering
  - verify no duplicates or skipped matching records

4. **Verification and tampering**
  - verify an intact chain
  - update a historical field directly with SQL and detect the first mismatch
  - delete a middle event and detect an ID gap
  - modify chain-state head and detect a head mismatch

5. **Retention**
  - create archive markers idempotently
  - exclude archived events from normal queries
  - include archived events in verification, export, and compliance reporting
  - confirm original audit-event rows are unchanged

6. **Redaction**
  - create an event with a sensitive path
  - return the logical value before redaction
  - remove wrapped key material through the redaction API
  - return a redacted marker afterward
  - keep committed payload and chain verification unchanged
  - treat repeated redaction as idempotent

7. **Export**
  - export all records for exactly one actor or resource
  - verify signature, selected content hashes, proof linkage, and captured head
  - modify the exported bundle and confirm verification fails

8. **Scenario C**
  - require `from` and `to`
  - return only `CLIENT_ACCOUNT` events
  - apply optional filters and cursor pagination
  - include archived history
  - exclude raw payloads

9. **Security**
  - reject missing or invalid credentials
  - reject authenticated users without the required role
  - permit `AUDIT_ADMIN` on all approved administrative and operational routes
  - ensure errors do not expose payloads, passwords, or key material

## Manual End-To-End Demonstration

A demonstration script or documented command sequence should show:

1. Start PostgreSQL and the application.
2. Create several events.
3. Query with combined filters and cursor pagination.
4. Verify an intact chain.
5. Modify an event directly in PostgreSQL.
6. Verify again and show the first inconsistency.
7. Recreate a clean state.
8. Run retention and verify the chain remains intact.
9. Redact a sensitive field and verify both privacy behavior and chain integrity.
10. Export and verify a bundle.
11. Generate the Scenario C compliance report.

## Quality Gates

For every implementation task:

- review the uncommitted diff
- run relevant unit and integration tests
- do not remove or weaken failing tests to obtain a green build
- record commands and actual results in the AI usage log
- document uncovered cases and time-boxed limitations

Final validation should include a clean `mvn verify`, local startup, the manual
tamper demonstration, a secret/confidential-file scan, and review of the full Git
history.

## Time-Boxed Limitations

Performance, load, chaos, backup-restore, key-rotation, multi-node deployment,
and external penetration testing are not required for the prototype. They should
be identified as production follow-up rather than represented as completed.