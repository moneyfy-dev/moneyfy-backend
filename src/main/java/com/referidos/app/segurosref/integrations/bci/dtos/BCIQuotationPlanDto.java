package com.referidos.app.segurosref.integrations.bci.dtos;

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
    private Integer planId;
    private String planName;
    private Integer deductible;
    private String deductibleDesc;
    private Double netValueUF;
    private Double grossValueUF;
    private Double taxValueUF;
    private Double monthlyPrice;
    private Double monthlyPriceUF;
    private Set<BCIQuotationPlanCoverDto> coverages;

}
