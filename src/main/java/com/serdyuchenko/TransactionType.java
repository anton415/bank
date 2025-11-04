package com.serdyuchenko;

/**
 * Enumeration of supported transaction categories and their effect on the balance.
 */
public enum TransactionType {
    ACCOUNT_OPENING(1),
    DEPOSIT(1),
    WITHDRAWAL(-1),
    TRANSFER_IN(1),
    TRANSFER_OUT(-1);

    private final int balanceDirection;

    TransactionType(int balanceDirection) {
        this.balanceDirection = balanceDirection;
    }

    /**
     * Translates a positive amount into a signed delta according to the transaction semantics.
     *
     * @param amount amount captured by the transaction (must be positive).
     * @return signed delta to apply to the running balance.
     */
    public double applyTo(double amount) {
        return balanceDirection * amount;
    }

    public int getBalanceDirection() {
        return balanceDirection;
    }
}
