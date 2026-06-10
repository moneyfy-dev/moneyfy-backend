package com.referidos.app.segurosref.integrations.fdi.dtos;

import java.math.BigDecimal;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuotationPlanDto {

    @EqualsAndHashCode.Include
    private String uniquePlan; // Id plan más deducible, si hay deducibles en el plan
    private String planName;
    private String planId;
    private Integer quotationId;
    private String FIDId;
    private String expiryDate;
    private String policyInceptionDate;
    private String policyExpiryDate;
    private Integer policyPeriodVigency;
    private BigDecimal netPremiumUF;
    private BigDecimal grossWrittenPremiumUF;
    private BigDecimal brokerageUF;
    private Integer liabilityAmount;
    private String garageType;
    private String vehicleReplacement;
    private Integer inspectionRequired;
    private BigDecimal monthlyPremium;
    private BigDecimal monthlyPriceUF;
    private BigDecimal monthlyPrice;
    private BigDecimal valueUF;
    private Integer totalMonths;
    private Integer deductibleUF;
    private String deductibleDesc;
    private BigDecimal discount;
    private String paymentPlan;
    private String paymentPipeline;
    private String quotationPeriod;
    private String paymentWay;
    private Set<FDIQuotationPlanCoverDto> coverages;

}
