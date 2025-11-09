package com.serdyuchenko.bank.transaction;

/**
 * Optional contextual information attached to a transaction, e.g. correlation IDs or statements text.
 */
public class TransactionMetadata {
    private static final TransactionMetadata EMPTY = new TransactionMetadata();
    private final String transactionId;
    private final String description;

    /**
     * Private empty constructor to support {@link #empty()}.
     */
    private TransactionMetadata() {
        this.transactionId = null;
        this.description = "";
    }

    /**
     * Creates metadata with a mandatory correlation identifier.
     *
     * @param transactionId upstream or idempotency identifier
     * @param description optional human-readable note
     */
    public TransactionMetadata(String transactionId, String description) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId cannot be null or blank");
        }
        this.transactionId = transactionId;
        this.description = description == null ? "" : description;
    }

    public static TransactionMetadata empty() {
        return EMPTY;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getDescription() {
        return description;
    }
}
