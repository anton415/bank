package com.serdyuchenko;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read model that reconstructs account history from the transaction ledger.
 */
public final class AccountStatement {
    private static final double EPSILON = 0.0000001D;

    private final AccountReference accountReference;
    private final List<Entry> entries;
    private final double closingBalance;

    private AccountStatement(AccountReference accountReference, List<Entry> entries, double closingBalance) {
        this.accountReference = accountReference;
        this.entries = entries;
        this.closingBalance = closingBalance;
    }

    public static AccountStatement fromTransactions(AccountReference accountReference, List<Transaction> transactions) {
        double runningBalance = 0D;
        List<Entry> statementEntries = new ArrayList<>(transactions.size());
        for (Transaction transaction : transactions) {
            runningBalance += transaction.getSignedAmount();
            double resultingBalance = transaction.getResultingBalance();
            if (Math.abs(resultingBalance - runningBalance) > EPSILON) {
                runningBalance = resultingBalance;
            }
            statementEntries.add(new Entry(transaction, runningBalance));
        }
        return new AccountStatement(accountReference,
                Collections.unmodifiableList(statementEntries), runningBalance);
    }

    public AccountReference getAccountReference() {
        return accountReference;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public double getClosingBalance() {
        return closingBalance;
    }

    /**
     * Single line of the reconstructed statement, exposing the original transaction and the running balance.
     */
    public static final class Entry {
        private final Transaction transaction;
        private final double runningBalance;

        private Entry(Transaction transaction, double runningBalance) {
            this.transaction = transaction;
            this.runningBalance = runningBalance;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public double getRunningBalance() {
            return runningBalance;
        }
    }
}
