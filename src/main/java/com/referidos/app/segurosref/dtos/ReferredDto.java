package com.referidos.app.segurosref.dtos;

public record ReferredDto(
    String email,
    String name,
    String surname,
    String status,
    int totalReferreds,
    long totalIncome
) {

}
