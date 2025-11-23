package com.serdyuchenko.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.serdyuchenko.bank.config.AppProperties;
import com.serdyuchenko.bank.domain.Account;
import com.serdyuchenko.bank.domain.User;
import com.serdyuchenko.bank.shared.OperationResult;
import com.serdyuchenko.bank.transaction.InMemoryTransactionLedger;
import com.serdyuchenko.bank.transaction.TransactionLedger;
import com.serdyuchenko.bank.workflow.WorkflowPort;

class BankServiceTest {

    @Test
    void addUser() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        assertThat(bank.findByPassport("3434")).isEqualTo(user);
    }

    @Test
    void deleteUserIsTrue() {
        User first = new User("3434", "Anton Serdyuchenko");
        User second = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(first);
        bank.addUser(second);
        bank.deleteUser("3434");
        assertThat(bank.findByPassport(first.getPassport())).isNull();
    }

    @Test
    void deleteUserIsFalse() {
        User first = new User("3434", "Anton Serdyuchenko");
        User second = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(first);
        bank.addUser(second);
        bank.deleteUser("343434");
        assertThat(bank.findByPassport(first.getPassport())).isEqualTo(first);
    }

    @Test
    void whenEnterInvalidPassport() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        assertThat(bank.findByRequisite("34", "5546")).isNull();
    }

    @Test
    void addAccount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        assertThat(bank.findByRequisite("3434", "5546").getBalance()).isEqualTo(150D);
    }

    @Test
    void addAccountIsInvalid() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount("4343", new Account("5546", 150D));
        assertThat(bank.getAccounts(user)).isEmpty();
    }

    @Test
    void addDuplicateAccount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("5546", 500D));
        assertThat(bank.getAccounts(user).size()).isEqualTo(1);
    }

    @Test
    void depositFundsSuccess() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.depositFunds(user.getPassport(), "5546", 100D);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Deposit completed successfully.");
        assertThat(result.getResultingBalance()).isEqualTo(250D);
    }

    @Test
    void depositFundsRejectsNonPositiveAmount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.depositFunds(user.getPassport(), "5546", 0D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Deposit amount must be greater than zero.");
    }

    @Test
    void depositFundsFailsWhenAccountMissing() {
        BankService bank = newBankService();

        OperationResult result = bank.depositFunds("3434", "5546", 100D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Account not found for the provided identifiers.");
    }

    @Test
    void withdrawFundsSuccess() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.withdrawFunds(user.getPassport(), "5546", 50D);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Withdrawal completed successfully.");
        assertThat(result.getResultingBalance()).isEqualTo(100D);
    }

    @Test
    void withdrawFundsRejectsNonPositiveAmount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.withdrawFunds(user.getPassport(), "5546", -10D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Withdrawal amount must be greater than zero.");
    }

    @Test
    void withdrawFundsRejectsOverdraft() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.withdrawFunds(user.getPassport(), "5546", 200D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Insufficient funds; balance cannot go below zero.");
    }

    @Test
    void withdrawFundsFailsWhenAccountMissing() {
        BankService bank = newBankService();

        OperationResult result = bank.withdrawFunds("3434", "5546", 50D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Account not found for the provided identifiers.");
    }

    @Test
    void transferMoneyOk() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("113", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "113", 150D);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Transfer completed successfully.");
        assertThat(bank.findByRequisite(user.getPassport(), "113").getBalance()).isEqualTo(200D);
        assertThat(bank.findByRequisite(user.getPassport(), "5546").getBalance()).isEqualTo(0D);
    }

    @Test
    void transferMoneySourceNull() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("113", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "554",
                user.getPassport(), "113", 150D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Source account not found for the provided identifiers.");
        assertThat(bank.findByRequisite(user.getPassport(), "113").getBalance()).isEqualTo(50D);
        assertThat(bank.findByRequisite(user.getPassport(), "5546").getBalance()).isEqualTo(150D);
    }

    @Test
    void transferMoneyDestinationIsNull() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("113", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "1131", 150D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Destination account not found for the provided identifiers.");
    }

    @Test
    void transferMoneyDontHaveEnoughMoney() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("113", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "113", 300D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Insufficient funds; balance cannot go below zero.");
    }

    @Test
    void transferNegativeAmountOfMoney() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("1131", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "1131", -150D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Transfer amount must be greater than zero.");
    }

    @Test
    void transferZeroAmountOfMoney() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("1131", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "1131", 0D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Transfer amount must be greater than zero.");
    }

    @Test
    void getAccountsReturnsDefensiveCopy() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = newBankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        List<Account> accounts = bank.getAccounts(user);

        assertThat(accounts).hasSize(1);
        assertThatThrownBy(() -> accounts.add(new Account("9999", 0)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void transactionsUseConfiguredCurrency() {
        User user = new User("3434", "Anton Serdyuchenko");
        TransactionLedger ledger = new InMemoryTransactionLedger();
        AppProperties properties = new AppProperties();
        properties.setDefaultCurrency("EUR");
        BankService bank = new BankService(ledger, properties, noopWorkflow());
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        bank.depositFunds(user.getPassport(), "5546", 50D);

        assertThat(ledger.getTransactions("5546")).hasSize(1);
        assertThat(ledger.getTransactions("5546").get(0).getAmount().getCurrency()).isEqualTo("EUR");
    }

    private BankService newBankService() {
        return new BankService(new InMemoryTransactionLedger(), defaultProperties(), noopWorkflow());
    }

    private AppProperties defaultProperties() {
        AppProperties properties = new AppProperties();
        properties.setDefaultCurrency("USD");
        return properties;
    }

    private WorkflowPort noopWorkflow() {
        return user -> {
            // no-op for tests
        };
    }
}
