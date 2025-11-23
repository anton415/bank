package com.serdyuchenko.bank.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.serdyuchenko.bank.domain.Money;

class TransactionLedgerTest {

    @Test
    void recordStoresTransactionPerAccount() {
        TransactionLedger ledger = new InMemoryTransactionLedger();

        Transaction recorded = ledger.record("ACC-1", TransactionType.DEPOSIT,
                money("25.00"), TransactionMetadata.empty());

        List<Transaction> transactions = ledger.getTransactions("ACC-1");

        assertThat(transactions).containsExactly(recorded);
        assertThat(recorded.getId()).isNotBlank();
        assertThat(recorded.getTimeStamp()).isNotNull();
    }

    @Test
    void getTransactionsIsImmutableSnapshot() {
        TransactionLedger ledger = new InMemoryTransactionLedger();
        ledger.record("ACC-1", TransactionType.DEPOSIT,
                money("10.00"), TransactionMetadata.empty());

        List<Transaction> snapshot = ledger.getTransactions("ACC-1");

        assertThat(snapshot).hasSize(1);
        assertThatThrownBy(() -> snapshot.add(snapshot.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void recordRejectsBlankAccountId() {
        TransactionLedger ledger = new InMemoryTransactionLedger();

        assertThatThrownBy(() -> ledger.record(" ", TransactionType.DEPOSIT,
                money("5.00"), TransactionMetadata.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account id");
    }

    private Money money(String amount) {
        return new Money("USD", new BigDecimal(amount));
    }
}
