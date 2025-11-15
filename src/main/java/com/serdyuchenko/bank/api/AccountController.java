package com.serdyuchenko.bank.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.serdyuchenko.bank.api.dto.AccountBalanceDto;
import com.serdyuchenko.bank.domain.Account;
import com.serdyuchenko.bank.service.BankService;

/**
 *
 * @author Anton Serdyuchenko
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final BankService bankService;

    /**
     * Constructs the controller with the domain service dependency injected by Spring.
     *
     * @param bankService application service that owns account orchestration logic
     */
    public AccountController(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * Returns the current balance for the account identified by passport/requisite if present.
     *
     * @param passport customer identifier
     * @param requisite account identifier
     * @return {@link ResponseEntity} containing the balance or 404 when no account matches
     */
    @GetMapping("/{passport}/{requisite}/balance")
    public ResponseEntity<AccountBalanceDto> balance(@PathVariable String passport,
                                                     @PathVariable String requisite) {
        Account account = bankService.findByRequisite(passport, requisite);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new AccountBalanceDto(account.getRequisite(), account.getBalance()));
    }
}
