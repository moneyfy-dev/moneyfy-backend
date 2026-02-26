package com.referidos.app.segurosref.dtos;

public record UserCommissionDto(
    String transactionId,
    String seller,
    String status,
    int commission,
    String createdDate,
    String observation
) {

}
