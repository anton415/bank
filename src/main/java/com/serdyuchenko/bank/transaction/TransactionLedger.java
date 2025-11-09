package com.serdyuchenko.bank.transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.serdyuchenko.bank.domain.Money;

/**
 * In-memory append-only ledger keyed by account identifier.
 *
 * @author Anton Serdyuchenko
 */
public class TransactionLedger {
    private final Map<String, List<Transaction>> ledger = new HashMap<>();

    /**
     * Records a transaction entry for the given account.
     *
     * @param accountId identifier of the account that owns the entry
     * @param type transaction classification
     * @param amount positive monetary amount
     * @param metadata optional metadata; {@link TransactionMetadata#empty()} when {@code null}
     * @return materialized {@link Transaction}
     */
    public synchronized Transaction record(String accountId,
                                           TransactionType type,
                                           Money amount,
                                           TransactionMetadata metadata) {
        String normalizedAccountId = requireAccountId(accountId);
        TransactionType safeType = Objects.requireNonNull(type, "Transaction type cannot be null");
        Money safeAmount = Objects.requireNonNull(amount, "Money cannot be null");
        TransactionMetadata safeMetadata = metadata == null ? TransactionMetadata.empty() : metadata;

        Transaction transaction = new Transaction(
            UUID.randomUUID().toString(),
            normalizedAccountId,
            safeAmount,
            safeType,
            Instant.now(),
            safeMetadata
        );
        ledger.computeIfAbsent(normalizedAccountId, key -> new ArrayList<>()).add(transaction);
        return transaction;
    }

    /**
     * Returns the immutable list of recorded transactions for the given account.
     *
     * @param accountId identifier tied to the ledger entries
     * @return immutable snapshot ordered by insertion time
     */
    public synchronized List<Transaction> getTransactions(String accountId) {
        List<Transaction> entries = ledger.get(requireAccountId(accountId));
        if (entries == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    private String requireAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account id cannot be null or blank");
        }
        return accountId;
    }
}
