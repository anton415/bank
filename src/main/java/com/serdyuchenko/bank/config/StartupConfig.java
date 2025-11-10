package com.serdyuchenko.bank.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.serdyuchenko.bank.service.BankService;

/**
 * Hosts infrastructure beans that should execute only when the application runs outside the {@code test} profile.
 * Keeps {@link com.serdyuchenko.bank.BankApplication} focused on bootstrapping while this class owns startup hooks.
 *
 * @author Anton Serdyuchenko
 */
@Configuration
public class StartupConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfig.class);

    /**
     * Logs a small diagnostic message once the {@link BankService} bean is ready, proving DI wiring succeeds.
     * Guarded by {@link Profile @Profile("!test")} so integration tests remain quiet.
     *
     * @param bankService injected bank domain service.
     * @return runner executed right after the Spring context starts.
     */
    @Bean
    @Profile("!test")
    @SuppressWarnings("unused") // Spring calls @Bean methods reflectively even if IDE sees no direct usage.
    CommandLineRunner startupRunner(BankService bankService) {
        return args -> LOGGER.info("Bank application ready. Service hash: {}", Integer.toHexString(System.identityHashCode(bankService)));
    }
}
