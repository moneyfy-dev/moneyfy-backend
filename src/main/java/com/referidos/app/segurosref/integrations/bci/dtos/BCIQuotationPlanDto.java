package com.referidos.app.segurosref.integrations.bci.dtos;

import java.math.BigDecimal;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BCIQuotationPlanDto {

    @EqualsAndHashCode.Include
    private String uniquePlan;
    private String planId;
    private String planName;
    private Integer deductible;
    private String deductibleDesc;
    private BigDecimal netValueUF;
    private BigDecimal grossValueUF;
    private BigDecimal taxValueUF;
    private BigDecimal monthlyPrice;
    private BigDecimal monthlyPriceUF;
    private Set<BCIQuotationPlanCoverDto> coverages;

}
