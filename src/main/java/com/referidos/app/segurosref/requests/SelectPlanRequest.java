package com.referidos.app.segurosref.requests;

import java.math.BigDecimal;

public record SelectPlanRequest(
        String quoterId,
        String planId,
        String insurer,
        String planName,
        BigDecimal valueUF, // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
        BigDecimal grossPriceUF, // Este campo debería ser fijo
        int totalMonths, // Este campo debería ser fijo
        BigDecimal monthlyPriceUF, // Este campo debería ser fijo
        BigDecimal monthlyPrice, // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
        String deductibleDesc,
        BigDecimal discount,
        String ownerName,
        String ownerPaternalSur,
        String ownerMaternalSur,
        String street,
        String streetNumber,
        String department,
        String region,
        String commune) {

}
