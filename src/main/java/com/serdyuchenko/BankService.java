package com.serdyuchenko;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main service.
 * @author antonserdyuchenko
 * @since 11.10.2025
 */
public class BankService {
    private final Clock clock;
    private final Map<User, List<Account>> users = new HashMap<>();
    private final List<Transaction> ledger = new ArrayList<>();

    public BankService() {
        this(Clock.systemUTC());
    }

    public BankService(Clock clock) {
        this.clock = clock;
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
        for (User user : users.keySet()) {
            if (passport.equals(user.getPassport())) {
                users.remove(user);
            }
        }
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
                if (account.getBalance() > 0D) {
                    AccountReference reference = AccountReference.of(passport, account.getRequisite());
                    recordTransaction(Transaction.accountOpening(reference, account.getBalance(),
                            account.getBalance(), Instant.now(clock)));
                }
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
        double sourceBalanceAfter = source.getBalance() - amount;
        double destinationBalanceAfter = destination.getBalance() + amount;

        source.setBalance(sourceBalanceAfter);
        destination.setBalance(destinationBalanceAfter);

        AccountReference sourceRef = AccountReference.of(sourcePassport, sourceRequisite);
        AccountReference destinationRef = AccountReference.of(destinationPassport, destinationRequisite);
        UUID correlationId = UUID.randomUUID();
        Instant occurredAt = Instant.now(clock);

        recordTransaction(Transaction.transferOut(sourceRef, amount, sourceBalanceAfter, occurredAt,
                destinationRef, correlationId));
        recordTransaction(Transaction.transferIn(destinationRef, amount, destinationBalanceAfter, occurredAt,
                sourceRef, correlationId));
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
        double resultingBalance = account.getBalance() + amount;
        account.setBalance(resultingBalance);

        AccountReference reference = AccountReference.of(passport, requisite);
        recordTransaction(Transaction.deposit(reference, amount, resultingBalance, Instant.now(clock)));
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
        double resultingBalance = account.getBalance() - amount;
        account.setBalance(resultingBalance);

        AccountReference reference = AccountReference.of(passport, requisite);
        recordTransaction(Transaction.withdrawal(reference, amount, resultingBalance, Instant.now(clock)));
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
            return null;
        }
        return Collections.unmodifiableList(accounts);
    }

    /**
     * Reconstructs an account statement solely from the append-only ledger.
     *
     * @param passport owner's passport.
     * @param requisite account requisite.
     * @return read model comprised of the recorded ledger entries.
     */
    public AccountStatement getAccountStatement(String passport, String requisite) {
        AccountReference reference = AccountReference.of(passport, requisite);
        List<Transaction> transactions = new ArrayList<>();
        for (Transaction transaction : ledger) {
            if (transaction.getAccountReference().equals(reference)) {
                transactions.add(transaction);
            }
        }
        return AccountStatement.fromTransactions(reference, transactions);
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

    private void recordTransaction(Transaction transaction) {
        ledger.add(transaction);
    }
}
