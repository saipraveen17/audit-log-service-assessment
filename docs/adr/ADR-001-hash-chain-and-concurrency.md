# ADR-001: Global Hash Chain And Concurrent Appends

## Status

Accepted for the prototype.

## Decision

Use one global append-only SHA-256 hash chain. A service-assigned `BIGINT id`
serves as event identity, chain position, verification reference, and cursor.
The ID is allocated as `chainState.lastId + 1` while holding a pessimistic write
lock on the singleton `GLOBAL` chain-state row. Audit events do not use
`@GeneratedValue` or a database sequence.

Store `contentHash`, `previousHash`, `recordHash`, and `hashVersion` on every
event. The first previous hash is `SHA-256("AUDIT_LOG_GENESIS_V1")`.

Verification captures the current chain head and verifies only through that
snapshot without holding the append lock for the complete scan.

## Alternatives Considered

- database-generated ID plus a separate chain sequence
- UUID identity plus a separate chain position
- optimistic head update with retry
- per-actor, per-resource, or per-tenant chains
- a queue or external append sequencer
- holding the chain lock for the full verification scan

## Rationale

A linear chain requires every append to depend on the immediately preceding
record hash. The chain-state lock prevents concurrent requests from creating a
fork. Reusing one ID keeps the model small and makes deletion gaps easy to
identify. Snapshot verification allows appends to continue while preserving a
stable verification result.

The lock is limited to the final append transaction; validation, timestamp
assignment, sensitive-field encryption, and content hashing happen before the
lock where practical.

## Trade-Offs And Limitations

- global appends serialize on one row
- full-chain verification is O(n)
- a complete database administrator could rewrite all records and the chain
  state if there is no external trust anchor
- deterministic canonicalization is part of the security boundary
- manually assigned contiguous IDs require all appends to pass through this
  transaction

## Production Evolution

With more time, scale, or security requirements:

- partition chains by tenant or bounded domain
- use a dedicated append coordinator or optimistic retry protocol
- add formal RFC-style canonical JSON
- publish signed periodic chain-head checkpoints outside the database
- use WORM/object-lock storage or external ledger anchoring
- use Merkle/checkpoint structures for scalable partial verification
- introduce versioned database migrations