package com.serdyuchenko;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BankServiceTest {

    @Test
    void addUser() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        assertThat(bank.findByPassport("3434")).isEqualTo(user);
    }

    @Test
    void deleteUserIsTrue() {
        User first = new User("3434", "Anton Serdyuchenko");
        User second = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(first);
        bank.addUser(second);
        bank.deleteUser("3434");
        assertThat(bank.findByPassport(first.getPassport())).isNull();
    }

    @Test
    void deleteUserIsFalse() {
        User first = new User("3434", "Anton Serdyuchenko");
        User second = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(first);
        bank.addUser(second);
        bank.deleteUser("343434");
        assertThat(bank.findByPassport(first.getPassport())).isEqualTo(first);
    }

    @Test
    void whenEnterInvalidPassport() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        assertThat(bank.findByRequisite("34", "5546")).isNull();
    }

    @Test
    void addAccount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        assertThat(bank.findByRequisite("3434", "5546").getBalance()).isEqualTo(150D);
        AccountStatement statement = bank.getAccountStatement("3434", "5546");
        assertThat(statement.getClosingBalance()).isEqualTo(150D);
        assertThat(statement.getEntries()).hasSize(1);
        AccountStatement.Entry entry = statement.getEntries().get(0);
        assertThat(entry.getTransaction().getType()).isEqualTo(TransactionType.ACCOUNT_OPENING);
        assertThat(entry.getTransaction().getAmount()).isEqualTo(150D);
        assertThat(entry.getRunningBalance()).isEqualTo(150D);
    }

    @Test
    void addAccountIsInvalid() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount("4343", new Account("5546", 150D));
        assertThat(bank.getAccounts(user)).isEmpty();
    }

    @Test
    void addDuplicateAccount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("5546", 500D));
        assertThat(bank.getAccounts(user).size()).isEqualTo(1);
    }

    @Test
    void depositFundsSuccess() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.depositFunds(user.getPassport(), "5546", 100D);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Deposit completed successfully.");
        assertThat(result.getResultingBalance()).isEqualTo(250D);
        AccountStatement statement = bank.getAccountStatement(user.getPassport(), "5546");
        assertThat(statement.getEntries()).hasSize(2);
        AccountStatement.Entry depositEntry = statement.getEntries().get(1);
        assertThat(depositEntry.getTransaction().getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(depositEntry.getTransaction().getAmount()).isEqualTo(100D);
        assertThat(depositEntry.getRunningBalance()).isEqualTo(250D);
    }

    @Test
    void depositFundsRejectsNonPositiveAmount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.depositFunds(user.getPassport(), "5546", 0D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Deposit amount must be greater than zero.");
    }

    @Test
    void depositFundsFailsWhenAccountMissing() {
        BankService bank = new BankService();

        OperationResult result = bank.depositFunds("3434", "5546", 100D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Account not found for the provided identifiers.");
    }

    @Test
    void withdrawFundsSuccess() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.withdrawFunds(user.getPassport(), "5546", 50D);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Withdrawal completed successfully.");
        assertThat(result.getResultingBalance()).isEqualTo(100D);
        AccountStatement statement = bank.getAccountStatement(user.getPassport(), "5546");
        AccountStatement.Entry withdrawalEntry = statement.getEntries().get(statement.getEntries().size() - 1);
        assertThat(withdrawalEntry.getTransaction().getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(withdrawalEntry.getTransaction().getAmount()).isEqualTo(50D);
        assertThat(withdrawalEntry.getRunningBalance()).isEqualTo(100D);
    }

    @Test
    void withdrawFundsRejectsNonPositiveAmount() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.withdrawFunds(user.getPassport(), "5546", -10D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Withdrawal amount must be greater than zero.");
    }

    @Test
    void withdrawFundsRejectsOverdraft() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));

        OperationResult result = bank.withdrawFunds(user.getPassport(), "5546", 200D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Insufficient funds; balance cannot go below zero.");
    }

    @Test
    void withdrawFundsFailsWhenAccountMissing() {
        BankService bank = new BankService();

        OperationResult result = bank.withdrawFunds("3434", "5546", 50D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Account not found for the provided identifiers.");
    }

    @Test
    void transferMoneyOk() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("113", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "113", 150D);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Transfer completed successfully.");
        assertThat(bank.findByRequisite(user.getPassport(), "113").getBalance()).isEqualTo(200D);
        assertThat(bank.findByRequisite(user.getPassport(), "5546").getBalance()).isEqualTo(0D);
        AccountStatement sourceStatement = bank.getAccountStatement(user.getPassport(), "5546");
        AccountStatement.Entry transferOutEntry = sourceStatement.getEntries()
                .get(sourceStatement.getEntries().size() - 1);
        AccountStatement destinationStatement = bank.getAccountStatement(user.getPassport(), "113");
        AccountStatement.Entry transferInEntry = destinationStatement.getEntries()
                .get(destinationStatement.getEntries().size() - 1);
        assertThat(transferOutEntry.getTransaction().getType()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(transferOutEntry.getTransaction().getAmount()).isEqualTo(150D);
        assertThat(transferOutEntry.getTransaction().getCounterparty())
                .isEqualTo(AccountReference.of(user.getPassport(), "113"));
        assertThat(transferInEntry.getTransaction().getType()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(transferInEntry.getTransaction().getAmount()).isEqualTo(150D);
        assertThat(transferInEntry.getTransaction().getCounterparty())
                .isEqualTo(AccountReference.of(user.getPassport(), "5546"));
        assertThat(transferOutEntry.getTransaction().getCorrelationId())
                .isEqualTo(transferInEntry.getTransaction().getCorrelationId());
        assertThat(transferInEntry.getRunningBalance()).isEqualTo(200D);
    }

    @Test
    void transferMoneySourceNull() {
        User user = new User("3434", "Anton Serdyuchenko");
        BankService bank = new BankService();
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
        BankService bank = new BankService();
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
        BankService bank = new BankService();
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
        BankService bank = new BankService();
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
        BankService bank = new BankService();
        bank.addUser(user);
        bank.addAccount(user.getPassport(), new Account("5546", 150D));
        bank.addAccount(user.getPassport(), new Account("1131", 50D));

        OperationResult result = bank.transferMoney(user.getPassport(), "5546",
                user.getPassport(), "1131", 0D);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Transfer amount must be greater than zero.");
    }
}
