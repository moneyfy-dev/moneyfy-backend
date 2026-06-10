package com.referidos.app.segurosref.integrations.fdi.pojos;

import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true) // Compara usando la clase padre (internalErrorCode)
public class FDIQuoteDealPojo extends BaseIntegrationResponse {

    @EqualsAndHashCode.Exclude // Ignora la colección para el equals/hashCode
    private List<Item> items;

    public FDIQuoteDealPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Item {
        @EqualsAndHashCode.Include
        private Integer itemId;
        private Set<Detail> quotations;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Detail {
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
        private BigDecimal netPremium;
        private BigDecimal grossWrittenPremium;
        private BigDecimal brokerage;
        private Integer liabilityAmount;
        private String garageType;
        private String vehicleReplacement;
        private Integer inspectionRequired;
        private BigDecimal monthlyPremium;
        private BigDecimal valueUf;
        private Plan plan;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Plan {
        @EqualsAndHashCode.Include
        private Integer id;
        @EqualsAndHashCode.Include
        private Integer siseId;
        private String name;
        private String paymentPlan;
        private String paymentPipeline;
        private String quotationPeriod;
        private String paymentWay;
        private Set<PlanParam> parameters;
        private Set<PlanCover> coverages;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class PlanParam {
        @EqualsAndHashCode.Include
        private Integer id;
        private String name;
        private String type;
        private Object value_0;
        private String valueType_0;
        private Set<String> rangeDescriptions;
        private List<PlanParamRange> ranges;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class PlanParamRange {
        @EqualsAndHashCode.Include
        private Integer id;
        private String name;
        private Set<PlanParamRangeValue> values;
    }

    @Data
    @NoArgsConstructor
    public static class PlanParamRangeValue {
        private String type;
        private Object value;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class PlanCover {
        @EqualsAndHashCode.Include
        private Integer id;
        private String name;
        private String mainDescription;
        private String generalDescription;
        private Integer isMain;
        private Integer isParam;
        private String valueDescription;
        private String polCad;
        private String value;
    }
}
