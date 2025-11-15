package com.serdyuchenko.bank.api.dto;

/**
 * Response payload for balance lookups.
 * @author Anton Serdyuchenko
 */
public record AccountBalanceDto(String requisite, double balance) {

}
