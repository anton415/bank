package com.serdyuchenko;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record of a single money movement affecting an account.
 */
public final class Transaction {
    private final UUID id;
    private final UUID correlationId;
    private final TransactionType type;
    private final AccountReference accountReference;
    private final double amount;
    private final double resultingBalance;
    private final Instant occurredAt;
    private final AccountReference counterparty;

    private Transaction(Builder builder) {
        this.id = builder.id;
        this.correlationId = builder.correlationId;
        this.type = builder.type;
        this.accountReference = builder.accountReference;
        this.amount = builder.amount;
        this.resultingBalance = builder.resultingBalance;
        this.occurredAt = builder.occurredAt;
        this.counterparty = builder.counterparty;
    }

    public static Transaction accountOpening(AccountReference accountReference, double amount,
                                             double resultingBalance, Instant occurredAt) {
        return create(accountReference, amount, resultingBalance, occurredAt,
                TransactionType.ACCOUNT_OPENING, null, null);
    }

    public static Transaction deposit(AccountReference accountReference, double amount,
                                      double resultingBalance, Instant occurredAt) {
        return create(accountReference, amount, resultingBalance, occurredAt,
                TransactionType.DEPOSIT, null, null);
    }

    public static Transaction withdrawal(AccountReference accountReference, double amount,
                                         double resultingBalance, Instant occurredAt) {
        return create(accountReference, amount, resultingBalance, occurredAt,
                TransactionType.WITHDRAWAL, null, null);
    }

    public static Transaction transferOut(AccountReference accountReference, double amount,
                                          double resultingBalance, Instant occurredAt,
                                          AccountReference counterparty, UUID correlationId) {
        return create(accountReference, amount, resultingBalance, occurredAt,
                TransactionType.TRANSFER_OUT, counterparty, correlationId);
    }

    public static Transaction transferIn(AccountReference accountReference, double amount,
                                         double resultingBalance, Instant occurredAt,
                                         AccountReference counterparty, UUID correlationId) {
        return create(accountReference, amount, resultingBalance, occurredAt,
                TransactionType.TRANSFER_IN, counterparty, correlationId);
    }

    private static Transaction create(AccountReference accountReference, double amount,
                                      double resultingBalance, Instant occurredAt,
                                      TransactionType type, AccountReference counterparty,
                                      UUID providedCorrelationId) {
        Objects.requireNonNull(accountReference, "accountReference must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (amount <= 0D) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        Builder builder = new Builder();
        builder.id = UUID.randomUUID();
        builder.correlationId = providedCorrelationId != null ? providedCorrelationId : builder.id;
        builder.type = type;
        builder.accountReference = accountReference;
        builder.amount = amount;
        builder.resultingBalance = resultingBalance;
        builder.occurredAt = occurredAt;
        builder.counterparty = counterparty;
        return new Transaction(builder);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public TransactionType getType() {
        return type;
    }

    public AccountReference getAccountReference() {
        return accountReference;
    }

    public double getAmount() {
        return amount;
    }

    public double getResultingBalance() {
        return resultingBalance;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AccountReference getCounterparty() {
        return counterparty;
    }

    /**
     * Signed delta that this transaction contributes to the running balance.
     *
     * @return signed value where positive numbers increase the balance.
     */
    public double getSignedAmount() {
        return type.applyTo(amount);
    }

    private static final class Builder {
        private UUID id;
        private UUID correlationId;
        private TransactionType type;
        private AccountReference accountReference;
        private double amount;
        private double resultingBalance;
        private Instant occurredAt;
        private AccountReference counterparty;
    }
}
