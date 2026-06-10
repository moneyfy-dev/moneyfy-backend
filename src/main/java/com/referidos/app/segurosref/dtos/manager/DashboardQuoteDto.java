package com.referidos.app.segurosref.dtos.manager;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardQuoteDto {
    // Base mandatory data
    private String idUser;
    private String userFullname;
    private String userEmail;
    private String quoteId;
    private String quoteStatus;
    private String quoterCarPpu;
    private String inicialDate;

    // Owner optional data
    private String quoterOwnerPersonalId;
    private String quoterOwnerFullname;

    // Car optional data
    private String quoterCarBrand;
    private String quoterCarModel;
    private String quoterCarYear;
    private String quoterCarType;

    // Buyer optional data
    private String quoterBuyerPersonalId;
    private String quoterBuyerFullname;
    private String quoterBuyerEmail;
    private String quoterBuyerPhone;

    // Plan optional data
    private String quoterPlanInsurer;
    private String quoterPlanName;
    private String quoterPlanUf;
    private BigDecimal quoterPlanMonthlyPriceUF;
    private BigDecimal quoterPlanMonthlyPrice;
    private int quoterPlanMonths;

    // Address optional data
    private String quoterAddressStreet;
    private String quoterAddressStreetNumber;

    // Transaction optional data
    private int transactionTotalCommission;
    private int transactionTotalScope;
}
