package com.serdyuchenko;

import java.math.BigDecimal;

/**
 * Value object representing an amount in a specific currency with basic invariants.
 */
public class Money {
    private final String currency;
    private final BigDecimal amount;

    /**
     * Creates a positive monetary amount.
     *
     * @param currency ISO-like currency code
     * @param amount positive numeric value
     */
    public Money(String currency, BigDecimal amount) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value");
        }
        this.currency = currency;
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
