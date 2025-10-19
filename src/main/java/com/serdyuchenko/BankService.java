package com.serdyuchenko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main service.
 * @author antonserdyuchenko
 * @since 11.10.2025
 */
public class BankService {
    /**
     * All users and there's accounts.
     */
    private final Map<User, List<Account>> users = new HashMap<>();

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
     * Transfer money from one account to another.
     * @param sourcePassport            user's passport from which will be transfer.
     * @param sourceRequisite           account's requisite from which will be transfer.
     * @param destinationPassport       user's passport to which will be transfer.
     * @param destinationRequisite      account's requisite to which will be transfer.
     * @param amount                    amount of money that will be transferred.
     * @return                          true if transferred was successful.
     */
    public boolean transferMoney(String sourcePassport, String sourceRequisite,
                                 String destinationPassport, String destinationRequisite,
                                 double amount) {
        Account source = findByRequisite(sourcePassport, sourceRequisite);
        Account destination = findByRequisite(destinationPassport, destinationRequisite);
        // validation
        if (source == null || destination == null) {
            return false;
        }
        if (source.getBalance() < amount) {
            return false;
        }
        // transfer
        source.setBalance(source.getBalance() - amount);
        destination.setBalance(destination.getBalance() + amount);
        return true;
    }

    public List<Account> getAccounts(User user) {
        return users.get(user);
    }
}
