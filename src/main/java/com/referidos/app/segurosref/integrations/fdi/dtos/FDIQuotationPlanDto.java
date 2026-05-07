package com.referidos.app.segurosref.integrations.fdi.dtos;

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
    private Double netPremiumUF;
    private Double grossWrittenPremiumUF;
    private Double brokerageUF;
    private Integer liabilityAmount;
    private String garageType;
    private String vehicleReplacement;
    private Integer inspectionRequired;
    private Double monthlyPremium;
    private Double monthlyPriceUF;
    private Double monthlyPrice;
    private Double valueUF;
    private Integer totalMonths;
    private Integer deductibleUF;
    private String deductibleDesc;
    private Double discount;
    private String paymentPlan;
    private String paymentPipeline;
    private String quotationPeriod;
    private String paymentWay;
    private Set<FDIQuotationPlanCoverDto> coverages;

}
