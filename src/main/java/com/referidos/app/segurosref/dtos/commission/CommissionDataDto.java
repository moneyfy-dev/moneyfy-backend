package com.referidos.app.segurosref.dtos.commission;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"transactionId", "commission"})
public record CommissionDataDto(
    String transactionId,
    int commission
) {

}
