package com.referidos.app.segurosref.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = { "quoterPlanId", "insurer", "planName", "valueUF", "grossPriceUF", "totalMonths",
        "monthlyPriceUF", "monthlyPrice", "deductibleDesc", "discount", "insurerAlias", "externalQuotationId",
        "expiryDate", "dealTokenFDI", "itemIdFDI" })
public class QuoterPlanModel {

    private String quoterPlanId;
    private String insurer;
    private String planName;
    private BigDecimal valueUF;
    private BigDecimal grossPriceUF;
    private int totalMonths;
    private BigDecimal monthlyPriceUF;
    private BigDecimal monthlyPrice;
    private String deductibleDesc;
    private BigDecimal discount;

    private String insurerAlias;
    private String externalQuotationId;
    private LocalDate expiryDate;
    private String dealTokenFDI;
    private Integer itemIdFDI;

}
