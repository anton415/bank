# Domain Model (current)

- Related: [ADR-0001 — Domain-first](./decisions/ADR-0001-domain-first.md)

## Glossary
- **Account** — holds a balance for a customer; supports deposit/withdraw.  
- **Money** — value object with `amount` and (later) `currency`.  
- **Transaction** — append-only record of money movement persisted in ledger.  
- **AccountStatement** — read model derived from the ledger to rebuild balances.

## Account (exists)
**State (today):**
- id: `AccountId`
- balance: number (later `Money`)

**Operations (tests drive behavior):**
- `deposit(amount > 0)` → increases balance and appends ledger row
- `withdraw(amount > 0, balance >= amount)` → decreases balance and appends ledger row
- `transfer(amount > 0, balance >= amount)` → coordinated withdraw/deposit with paired ledger entries
- `getAccountStatement()` → reconstructs balance history from ledger

**Invariants:**
- Amounts must be positive
- No overdraft unless explicitly allowed
- Ledger entries record the resulting balance for replay/auditability

## Transaction (exists)
**State:**
- id: `UUID`
- correlationId: `UUID` (shared by both legs of a transfer)
- type: `TransactionType` (`ACCOUNT_OPENING`, `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`)
- account: `AccountReference(passport, requisite)`
- amount: positive number (later `Money`)
- resultingBalance: account balance after applying the transaction
- occurredAt: `Instant`
- counterparty: optional `AccountReference` for transfer context

**Behavior & invariants:**
- Amount must be strictly positive.
- Transfer legs share the same `correlationId` and point to each other via `counterparty`.
- Transactions are immutable once appended to the in-memory ledger.

## AccountStatement (read model)
- Built from the ordered ledger entries for an account.
- Computes a running balance and exposes each transaction alongside the reconstructed total.
- Tolerates replay to validate current balance vs. ledger-derived balance.

## BankService (exists)
- Holds user -> account relationships in-memory.
- Provides `depositFunds`, `withdrawFunds`, and `transferMoney` orchestration methods.
- Each successful operation appends a `Transaction` to the ledger, ensuring append-only audit trail.
- Exposes `getAccountStatement` to recreate balances directly from the ledger.
- Each operation returns an `OperationResult` carrying a success flag, the user-facing message defined in the business doc, and (when successful) the resulting balance.
- Validation order: locate account(s) → assert positive amount → assert sufficient funds (withdraw/transfer) → mutate state → append ledger entry.
