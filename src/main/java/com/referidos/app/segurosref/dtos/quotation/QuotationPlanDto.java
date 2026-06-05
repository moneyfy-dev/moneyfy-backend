package com.referidos.app.segurosref.dtos.quotation;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonPropertyOrder(value = {"uniquePlan", "planId", "insurer", "planName", "valueUF", "grossPriceUF",
        "totalMonths", "monthlyPriceUF", "monthlyPrice", "deductible", "deductibleDesc", "discount",
        "stolenVehicle", "totalLoss", "damageThirdParty", "workshopType", "quotationIdBCI",
        "expiryDateBCI", "dealTokenFDI", "itemIdFDI",
        "quotationIdFDI", "FIDId", "expiryDateFDI", "brokerageUfFDI", "vehicleReplacementFDI",
        "inspectionRequiredFDI", "monthlyPremiumFDI", "paymentPlanFDI", "quotationPeriodFDI",
        "paymentWayFDI", "coverages", "details"})
public class QuotationPlanDto {

    @EqualsAndHashCode.Include
    private String uniquePlan;
    @EqualsAndHashCode.Include
    private String planId;
    private String insurer;
    @EqualsAndHashCode.Include
    private String planName;
    private Double valueUF;
    private Double grossPriceUF;
    private Integer totalMonths;
    private Double monthlyPriceUF;
    private Double monthlyPrice;
    private Integer deductible;
    private String deductibleDesc;
    private Double discount;
    private String stolenVehicle;
    private String totalLoss;
    private String damageThirdParty;
    private String workshopType;
    
    private Integer quotationIdBCI;
    private String expiryDateBCI;

    private String dealTokenFDI;
    private Integer itemIdFDI;
    private Integer quotationIdFDI;
    private String FIDId;
    private String expiryDateFDI;
    private Double brokerageUfFDI;
    private String vehicleReplacementFDI;
    private Integer inspectionRequiredFDI;
    private Double monthlyPremiumFDI;
    private String paymentPlanFDI;
    private String quotationPeriodFDI;
    private String paymentWayFDI;

    private Set<QuotationPlanCoverDto> coverages;
    private List<String> details;
    
    // Métodos de lógica, propios de la clase
    public List<String> addDetail(String detail) {
        this.details.add(detail);
        return this.details;
    }

    // Métodos de lógica, propios de la clase
    public Set<QuotationPlanCoverDto> addCoverage(QuotationPlanCoverDto coverage) {
        this.coverages.add(coverage);
        return this.coverages;
    }

}
