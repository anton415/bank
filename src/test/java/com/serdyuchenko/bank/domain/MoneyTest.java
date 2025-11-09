package com.serdyuchenko.bank.domain;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void createsInstanceWhenCurrencyAndAmountValid() {
        Money money = new Money("USD", BigDecimal.valueOf(25));

        assertThat(money.getCurrency()).isEqualTo("USD");
        assertThat(money.getAmount()).isEqualByComparingTo("25");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> new Money(null, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency");
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> new Money("   ", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Money("USD", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount");
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> new Money("USD", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount");
        assertThatThrownBy(() -> new Money("USD", BigDecimal.valueOf(-5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount");
    }
}
