package com.serdyuchenko.bank.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.serdyuchenko.bank.config.AppProperties;
import com.serdyuchenko.bank.domain.Account;
import com.serdyuchenko.bank.domain.Money;
import com.serdyuchenko.bank.domain.User;
import com.serdyuchenko.bank.shared.OperationResult;
import com.serdyuchenko.bank.transaction.TransactionLedger;
import com.serdyuchenko.bank.transaction.TransactionMetadata;
import com.serdyuchenko.bank.transaction.TransactionType;

/**
 * Main service.
 * @author antonserdyuchenko
 * @since 11.10.2025
 */
@Service
public class BankService {
    private final TransactionLedger ledger;
    /**
     * All users and there's accounts.
     */
    private final Map<User, List<Account>> users = new HashMap<>();
    private final AppProperties properties;

    /**
     * Creates a service with an injected ledger dependency for better testability/extensibility.
     *
     * @param ledger ledger instance to record transactions in
     */
    public BankService(TransactionLedger ledger, AppProperties properties) {
        this.ledger = Objects.requireNonNull(ledger, "TransactionLedger cannot be null");
        this.properties = Objects.requireNonNull(properties, "AppProperties cannot be null");
    }

    /**
     * Add user.
     * @param user  user that would be added.
     */
    public void addUser(User user) {
        users.put(user, new ArrayList<>());
    }

    /**
     * Delete user.
     * @param passport  passport of user that would be deleted.
     */
    public void deleteUser(String passport) {
        users.entrySet().removeIf(entry -> passport.equals(entry.getKey().getPassport()));
    }

    /**
     * Add new account to user.
     * @param passport  passport of user that would have new account.
     * @param account   new account.
     */
    public void addAccount(String passport, Account account) {
        User user = findByPassport(passport);
        if (user != null) {
            List<Account> accounts = users.get(user);
            if (findByRequisite(passport, account.getRequisite()) == null) {
                accounts.add(account);
            }
        }
    }

    /**
     * Find user by passport.
     * @param passport  passport of user.
     * @return          user.
     */
    public User findByPassport(String passport) {
        for (User user : users.keySet()) {
            if (passport.equals(user.getPassport())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Find account by passport and requisite.
     * @param passport      user's passport.
     * @param requisite     account's requisite.
     * @return              account.
     */
    public Account findByRequisite(String passport, String requisite) {
        User user = findByPassport(passport);
        List<Account> accounts = users.get(user);
        if (accounts != null) {
            for (Account account : accounts) {
                if (requisite.equals(account.getRequisite())) {
                    return account;
                }
            }
        }
        return null;
    }

    /**
     * Transfer money from one account to another, enforcing positive amounts and no overdraft.
     *
     * @param sourcePassport user's passport from which funds will be transferred.
     * @param sourceRequisite account requisite from which funds will be transferred.
     * @param destinationPassport user's passport receiving the funds.
     * @param destinationRequisite account requisite receiving the funds.
     * @param amount amount of money to transfer.
     * @return {@link OperationResult} describing success or the validation failure.
     */
    public OperationResult transferMoney(String sourcePassport, String sourceRequisite,
                                         String destinationPassport, String destinationRequisite,
                                         double amount) {
        Account source = findByRequisite(sourcePassport, sourceRequisite);
        if (source == null) {
            return OperationResult.failure("Source account not found for the provided identifiers.");
        }
        Account destination = findByRequisite(destinationPassport, destinationRequisite);
        if (destination == null) {
            return OperationResult.failure("Destination account not found for the provided identifiers.");
        }
        OperationResult validation = validatePositiveAmount(amount, "Transfer");
        if (validation != null) {
            return validation;
        }
        if (source.getBalance() < amount) {
            return OperationResult.failure("Insufficient funds; balance cannot go below zero.");
        }
        // Apply debit and credit atomically from the perspective of the in-memory model.
        source.setBalance(source.getBalance() - amount);
        destination.setBalance(destination.getBalance() + amount);
        String transferId = UUID.randomUUID().toString();
        Money money = toMoney(amount);
        ledger.record(
            source.getRequisite(),
            TransactionType.TRANSFER,
            money,
            metadata(transferId, "Transfer to account " + destination.getRequisite())
        );
        ledger.record(
            destination.getRequisite(),
            TransactionType.TRANSFER,
            money,
            metadata(transferId, "Transfer from account " + source.getRequisite())
        );
        return OperationResult.success("Transfer completed successfully.", source.getBalance());
    }

    /**
     * Deposits funds into the account identified by passport and requisite.
     *
     * @param passport user's passport.
     * @param requisite account requisite.
     * @param amount amount of money to deposit.
     * @return {@link OperationResult} describing success or the validation failure.
     */
    public OperationResult depositFunds(String passport, String requisite, double amount) {
        Account account = findByRequisite(passport, requisite);
        if (account == null) {
            return OperationResult.failure("Account not found for the provided identifiers.");
        }
        OperationResult validation = validatePositiveAmount(amount, "Deposit");
        if (validation != null) {
            return validation;
        }
        account.setBalance(account.getBalance() + amount);
        ledger.record(
            account.getRequisite(),
            TransactionType.DEPOSIT,
            toMoney(amount),
            metadata("Deposit into account " + account.getRequisite())
        );
        return OperationResult.success("Deposit completed successfully.", account.getBalance());
    }

    /**
     * Withdraws funds from the account identified by passport and requisite.
     *
     * @param passport user's passport.
     * @param requisite account requisite.
     * @param amount amount of money to withdraw.
     * @return {@link OperationResult} describing success or the validation failure.
     */
    public OperationResult withdrawFunds(String passport, String requisite, double amount) {
        Account account = findByRequisite(passport, requisite);
        if (account == null) {
            return OperationResult.failure("Account not found for the provided identifiers.");
        }
        OperationResult validation = validatePositiveAmount(amount, "Withdrawal");
        if (validation != null) {
            return validation;
        }
        if (account.getBalance() < amount) {
            return OperationResult.failure("Insufficient funds; balance cannot go below zero.");
        }
        account.setBalance(account.getBalance() - amount);
        ledger.record(
            account.getRequisite(),
            TransactionType.WITHDRAWAL,
            toMoney(amount),
            metadata("Withdrawal from account " + account.getRequisite())
        );
        return OperationResult.success("Withdrawal completed successfully.", account.getBalance());
    }

    /**
     * Exposes read-only view of the accounts list for a given user.
     *
     * @param user target user.
     * @return accounts registered for the user; {@code null} when the user was not added.
     */
    public List<Account> getAccounts(User user) {
        List<Account> accounts = users.get(user);
        if (accounts == null) {
            return List.of();
        }
        return List.copyOf(accounts);
    }

    /**
     * Validates that the provided amount is positive for the given operation.
     *
     * @param amount monetary amount to inspect.
     * @param operationName name of the calling operation for error context.
     * @return failure {@link OperationResult} when the amount is invalid; {@code null} otherwise.
     */
    private OperationResult validatePositiveAmount(double amount, String operationName) {
        if (amount <= 0) {
            return OperationResult.failure(operationName + " amount must be greater than zero.");
        }
        return null;
    }

    private Money toMoney(double amount) {
        return new Money(properties.getDefaultCurrency(), BigDecimal.valueOf(amount));
    }

    private TransactionMetadata metadata(String description) {
        return metadata(UUID.randomUUID().toString(), description);
    }

    private TransactionMetadata metadata(String transactionId, String description) {
        return new TransactionMetadata(transactionId, description);
    }
}
