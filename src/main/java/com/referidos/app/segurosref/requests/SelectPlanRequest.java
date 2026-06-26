package com.referidos.app.segurosref.requests;

import java.math.BigDecimal;

public record SelectPlanRequest(
        String quoterId,

        String planId,
        String insurer,
        String insurerAlias,
        String planName,
        BigDecimal valueUF, // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
        BigDecimal grossPriceUF, // Este campo debería ser fijo
        int totalMonths, // Este campo debería ser fijo
        BigDecimal monthlyPriceUF, // Este campo debería ser fijo
        BigDecimal monthlyPrice, // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
        String deductibleDesc,
        BigDecimal discount,

        Integer intNroTarificacionBCI,
        String strNroCotizacionBCI,
        String dtFinVigenciaBCI,

        String dealTokenFDI,
        Integer itemIdFDI,
        Integer quotationIdFDI,
        String fidIdFDI,
        String expiryDateFDI,

        String ownerName,
        String ownerPaternalSur,
        String ownerMaternalSur,

        String region,
        String commune,
        String street,
        String streetNumber,
        String department) {

}
