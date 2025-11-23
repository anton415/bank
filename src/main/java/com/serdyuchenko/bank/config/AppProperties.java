package com.serdyuchenko.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author Anton Serdyuchenko
 */
@ConfigurationProperties("app")
public class AppProperties {
    /**
     * Currency code used whenever no explicit account currency is set.
     */
    private String defaultCurrency = "USD";

    // Add more fields here as you expand the YAML.

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
}
