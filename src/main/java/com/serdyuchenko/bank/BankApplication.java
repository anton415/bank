package com.serdyuchenko.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entrypoint for the Bank application. Boots a Spring context so service beans and future adapters
 * (REST, Camunda, persistence) can be auto-configured.
 *
 * @author Anton Serdyuchenko
 */
@SpringBootApplication
public class BankApplication {

    /**
     * Boots the Spring container using SpringApplication.
     *
     * @param args command-line arguments forwarded to Spring Boot.
     */
    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }
}
