package com.referidos.app.segurosref.integrations.fdi.pojos;

import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FDIQuotePlanPojo {

    @EqualsAndHashCode.Include
    private Integer id;
    @EqualsAndHashCode.Include
    private Integer siseId;
    private String name;
    private String paymentPlan;
    private String paymentPipeline;
    private String quotationPeriod;
    private String paymentWay;
    private Set<FDIQuotePlanParamPojo> parameters;
    private Set<FDIQuotePlanCoverPojo> coverages;


}
