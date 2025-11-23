package com.serdyuchenko.bank.transaction;

import java.util.List;

import com.serdyuchenko.bank.domain.Money;

/**
 *
 * @author Anton Serdyuchenko
 */
public interface TransactionLedger {
    Transaction record(String accountId,
                       TransactionType type,
                       Money amount,
                       TransactionMetadata metadata);

    List<Transaction> getTransactions(String accountId);
}
