package com.referidos.app.segurosref.dtos.commission;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"rut", "holderName", "email", "bank", "accountType", "accountNumber"})
public record CommissionAccountDto(
    String rut,
    String holderName,
    String email,
    String bank,
    String accountType,
    String accountNumber
) {

}
