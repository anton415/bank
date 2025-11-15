package com.serdyuchenko.bank.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.serdyuchenko.bank.api.dto.AccountBalanceDto;
import com.serdyuchenko.bank.domain.Account;
import com.serdyuchenko.bank.domain.User;
import com.serdyuchenko.bank.service.BankService;

/**
 * Verifies the {@link AccountController} wiring end-to-end via HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BankService bankService;

    private String passport;
    private String requisite;

    /**
     * Seeds a fresh user/account pair before each test so HTTP calls hit real data.
     */
    @BeforeEach
    void setUpAccount() {
        passport = "passport-" + UUID.randomUUID();
        requisite = "req-" + UUID.randomUUID();
        User user = new User(passport, "API Tester");
        bankService.addUser(user);
        bankService.addAccount(passport, new Account(requisite, 200D));
    }

    /**
     * Ensures a happy-path lookup responds with HTTP 200 and the expected DTO payload.
     */
    @Test
    void balanceEndpointReturnsCurrentBalance() {
        ResponseEntity<AccountBalanceDto> response = restTemplate.getForEntity(
            "/api/accounts/{passport}/{requisite}/balance",
            AccountBalanceDto.class,
            passport,
            requisite
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requisite()).isEqualTo(requisite);
        assertThat(response.getBody().balance()).isEqualTo(200D);
    }

    /**
     * Confirms the controller returns 404 when the account identifiers are unknown.
     */
    @Test
    void balanceEndpointReturns404WhenAccountMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/accounts/{passport}/{requisite}/balance",
            String.class,
            "missing-passport",
            "missing-requisite"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
