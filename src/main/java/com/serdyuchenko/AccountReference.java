package com.serdyuchenko;

import java.util.Objects;

/**
 * Value object that uniquely identifies an account within the in-memory model.
 */
public final class AccountReference {
    private final String passport;
    private final String requisite;

    private AccountReference(String passport, String requisite) {
        this.passport = passport;
        this.requisite = requisite;
    }

    public static AccountReference of(String passport, String requisite) {
        Objects.requireNonNull(passport, "passport must not be null");
        Objects.requireNonNull(requisite, "requisite must not be null");
        return new AccountReference(passport, requisite);
    }

    public String getPassport() {
        return passport;
    }

    public String getRequisite() {
        return requisite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccountReference that)) {
            return false;
        }
        return passport.equals(that.passport) && requisite.equals(that.requisite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passport, requisite);
    }

    @Override
    public String toString() {
        return "AccountReference{" + "passport='" + passport + '\'' + ", requisite='" + requisite + '\'' + '}';
    }
}
