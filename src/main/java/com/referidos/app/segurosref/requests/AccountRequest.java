package com.referidos.app.segurosref.requests;

public record AccountRequest(
    String accountId,
    String personalId,
    String holderName,
    String alias,
    String email,
    String bank,
    String accountType,
    String accountNumber
) {

}
