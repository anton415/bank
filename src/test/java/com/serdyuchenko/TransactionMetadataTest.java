package com.serdyuchenko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TransactionMetadataTest {

    @Test
    void emptyReturnsSingletonWithNeutralValues() {
        TransactionMetadata metadata = TransactionMetadata.empty();

        assertThat(metadata.getTransactionId()).isNull();
        assertThat(metadata.getDescription()).isEmpty();
        assertThat(metadata).isSameAs(TransactionMetadata.empty());
    }

    @Test
    void createsMetadataWithDescription() {
        TransactionMetadata metadata = new TransactionMetadata("external-123", "ATM Withdrawal");

        assertThat(metadata.getTransactionId()).isEqualTo("external-123");
        assertThat(metadata.getDescription()).isEqualTo("ATM Withdrawal");
    }

    @Test
    void defaultsDescriptionWhenNull() {
        TransactionMetadata metadata = new TransactionMetadata("external-123", null);

        assertThat(metadata.getDescription()).isEmpty();
    }

    @Test
    void rejectsBlankTransactionId() {
        assertThatThrownBy(() -> new TransactionMetadata("  ", "note"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");
    }
}
