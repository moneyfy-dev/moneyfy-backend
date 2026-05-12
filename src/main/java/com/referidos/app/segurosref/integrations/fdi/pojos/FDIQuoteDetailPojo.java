package com.referidos.app.segurosref.integrations.fdi.pojos;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuoteDetailPojo {

    @EqualsAndHashCode.Include
    private String planId;
    @EqualsAndHashCode.Include
    private Integer id;
    private String FIDId;
    private String expiryDate;
    private String policyInceptionDate;
    private String policyExpiryDate;
    private Integer policyPeriodVigency;
    private Integer deductible;
    private Double netPremium;
    private Double grossWrittenPremium;
    private Double brokerage;
    private Integer liabilityAmount;
    private String garageType;
    private String vehicleReplacement;
    private Integer inspectionRequired;
    private Double monthlyPremium;
    private Double valueUf;
    private FDIQuotePlanPojo plan;
    
}
