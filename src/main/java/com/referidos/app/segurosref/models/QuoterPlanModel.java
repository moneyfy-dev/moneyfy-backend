package com.referidos.app.segurosref.models;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = { "quoterPlanId", "insurer", "insurerAlias", "planName", "valueUF", "grossPriceUF",
        "totalMonths",
        "monthlyPriceUF", "monthlyPrice", "deductibleDesc", "discount", "intNroTarificacionBCI",
        "strNroCotizacionBCI", "dtFinVigenciaBCI", "dealTokenFDI", "itemIdFDI", "quotationIdFDI", "fidIdFDI",
        "expiryDateFDI" })
public class QuoterPlanModel {

    private String quoterPlanId;
    private String insurer;
    private String insurerAlias;
    private String planName;
    private BigDecimal valueUF;
    private BigDecimal grossPriceUF;
    private int totalMonths;
    private BigDecimal monthlyPriceUF;
    private BigDecimal monthlyPrice;
    private String deductibleDesc;
    private BigDecimal discount;

    private Integer intNroTarificacionBCI;
    private String strNroCotizacionBCI;
    private String dtFinVigenciaBCI;

    private String dealTokenFDI;
    private Integer itemIdFDI;
    private Integer quotationIdFDI;
    private String fidIdFDI;
    private String expiryDateFDI;

}
