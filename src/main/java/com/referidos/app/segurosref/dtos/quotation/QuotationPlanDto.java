package com.referidos.app.segurosref.dtos.quotation;

import java.math.BigDecimal;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonPropertyOrder(value = { "uniquePlan", "planId", "insurer", "planName", "valueUF", "grossPriceUF",
        "totalMonths", "monthlyPriceUF", "monthlyPrice", "deductible", "deductibleDesc", "discount",
        "workshopType", "intNroTarificacionBCI", "strNroCotizacionBCI",
        "dtFinVigenciaBCI", "dealTokenFDI", "itemIdFDI",
        "quotationIdFDI", "fidIdFDI", "expiryDateFDI", "brokerageUfFDI", "vehicleReplacementFDI",
        "inspectionRequiredFDI", "monthlyPremiumFDI", "paymentPlanFDI", "quotationPeriodFDI",
        "paymentWayFDI", "coverages" })
public class QuotationPlanDto {

    @EqualsAndHashCode.Include
    private String uniquePlan;
    @EqualsAndHashCode.Include
    private String planId;
    @EqualsAndHashCode.Include
    private String planName;
    private BigDecimal valueUF;
    private BigDecimal grossPriceUF;
    private Integer totalMonths;
    private BigDecimal monthlyPriceUF;
    private BigDecimal monthlyPrice;
    private Integer deductible;
    private String deductibleDesc;
    private BigDecimal discount;
    private String stolenVehicle;
    private String totalLoss;
    private String damageThirdParty;
    private String workshopType;

    private Integer intNroTarificacionBCI;
    private String strNroCotizacionBCI;
    private String dtFinVigenciaBCI;

    private String dealTokenFDI;
    private Integer itemIdFDI;
    private Integer quotationIdFDI;
    private String fidIdFDI;
    private String expiryDateFDI;

    private BigDecimal brokerageUfFDI;
    private String vehicleReplacementFDI;
    private Integer inspectionRequiredFDI;
    private BigDecimal monthlyPremiumFDI;
    private String paymentPlanFDI;
    private String quotationPeriodFDI;
    private String paymentWayFDI;

    private Set<QuotationPlanCoverDto> coverages;

    // Métodos de lógica, propios de la clase
    public Set<QuotationPlanCoverDto> addCoverage(QuotationPlanCoverDto coverage) {
        this.coverages.add(coverage);
        return this.coverages;
    }

}
