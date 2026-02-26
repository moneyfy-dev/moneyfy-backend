package com.referidos.app.segurosref.requests;

public record SelectPlanRequest(
    String quoterId,
    String planId,
    String insurer,
    String planName,
    double valueUF, // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
    double grossPriceUF, // Este campo debería ser fijo
    int totalMonths, // Este campo debería ser fijo
    double monthlyPriceUF, // Este campo debería ser fijo
    double monthlyPrice, // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
    int deductible,
    double discount,
    String ownerName,
    String ownerPaternalSur,
    String ownerMaternalSur,
    String street,
    String streetNumber,
    String department
) {

}
