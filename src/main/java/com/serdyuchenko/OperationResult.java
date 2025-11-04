package com.serdyuchenko;

/**
 * Describes the result of a money movement operation and surfaces a user-facing message.
 */
public class OperationResult {
    private final boolean success;
    private final String message;
    private final Double resultingBalance;

    private OperationResult(boolean success, String message, Double resultingBalance) {
        this.success = success;
        this.message = message;
        this.resultingBalance = resultingBalance;
    }

    /**
     * Creates a successful result with the provided informational message and resulting balance.
     *
     * @param message readable confirmation for the caller.
     * @param resultingBalance balance after the successful operation (may be {@code null}).
     * @return success result representation.
     */
    public static OperationResult success(String message, Double resultingBalance) {
        return new OperationResult(true, message, resultingBalance);
    }

    /**
     * Creates a failure result with the provided user-facing message.
     *
     * @param message validation or business-rule failure message.
     * @return failure result representation.
     */
    public static OperationResult failure(String message) {
        return new OperationResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Double getResultingBalance() {
        return resultingBalance;
    }
}

