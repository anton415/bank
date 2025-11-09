package com.serdyuchenko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void createsTransactionWithMetadata() {
        TransactionMetadata metadata = new TransactionMetadata("ext-001", "Initial deposit");
        Transaction transaction = new Transaction(
                "txn-1",
                "acct-1",
                usd(100),
                TransactionType.DEPOSIT,
                Instant.EPOCH,
                metadata
        );

        assertThat(transaction.getId()).isEqualTo("txn-1");
        assertThat(transaction.getAccountId()).isEqualTo("acct-1");
        assertThat(transaction.getAmount().getAmount()).isEqualByComparingTo("100");
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.getTimeStamp()).isEqualTo(Instant.EPOCH);
        assertThat(transaction.getMetadata()).isSameAs(metadata);
    }

    @Test
    void assignsEmptyMetadataWhenOmitted() {
        Transaction transaction = new Transaction(
                "txn-2",
                "acct-2",
                usd(50),
                TransactionType.WITHDRAWAL,
                Instant.EPOCH
        );

        assertThat(transaction.getMetadata()).isSameAs(TransactionMetadata.empty());
    }

    @Test
    void rejectsBlankAccountId() {
        assertThatThrownBy(() -> new Transaction(
                "txn-3",
                " ",
                usd(25),
                TransactionType.DEPOSIT,
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account id");
    }

    @Test
    void rejectsNullMetadata() {
        assertThatThrownBy(() -> new Transaction(
                "txn-4",
                "acct-4",
                usd(25),
                TransactionType.DEPOSIT,
                Instant.now(),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Metadata");
    }

    private static Money usd(double amount) {
        return new Money("USD", BigDecimal.valueOf(amount));
    }
}
