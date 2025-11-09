package com.serdyuchenko.bank.transaction;

import java.time.Instant;

import com.serdyuchenko.bank.domain.Money;

/**
 * Immutable ledger entry describing a single money movement for an account.
 * Guarantees non-null identifiers, positive {@link Money}, and chronological stamp
 * so a ledger can be replayed deterministically.
 */
public class Transaction {
    private final String id;
    private final String accountId;
    private final TransactionType type;
    private final Money amount;
    private final Instant timeStamp;
    private final TransactionMetadata metadata;

    /**
     * Creates a transaction without additional metadata.
     *
     * @param id unique transaction identifier
     * @param accountId owning account identifier
     * @param amount validated monetary amount (must be positive)
     * @param type domain-specific transaction type
     * @param timeStamp instant when the transaction occurred
     */
    public Transaction(String id, String accountId, Money amount, TransactionType type, Instant timeStamp) {
        this(id, accountId, amount, type, timeStamp, TransactionMetadata.empty());
    }

    /**
     * Creates a transaction with optional metadata payload.
     *
     * @param id unique transaction identifier
     * @param accountId owning account identifier
     * @param amount validated monetary amount (must be positive)
     * @param type domain-specific transaction type
     * @param timeStamp instant when the transaction occurred
     * @param metadata contextual information describing how/why the entry exists
     */
    public Transaction(String id, String accountId, Money amount, TransactionType type, Instant timeStamp, TransactionMetadata metadata) {
        this.id = requireNonBlank(id, "Transaction id");
        this.accountId = requireNonBlank(accountId, "Account id");
        this.amount = requireNonNull(amount, "Amount");
        this.type = requireNonNull(type, "Transaction type");
        this.timeStamp = requireNonNull(timeStamp, "Timestamp");
        this.metadata = requireNonNull(metadata, "Metadata");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return value;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public Money getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public TransactionMetadata getMetadata() {
        return metadata;
    }
}
