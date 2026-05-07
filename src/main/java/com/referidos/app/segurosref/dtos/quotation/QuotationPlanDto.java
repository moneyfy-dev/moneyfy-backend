package com.referidos.app.segurosref.dtos.quotation;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"planId", "insurer", "planName", "valueUF", "grossPriceUF", "totalMonths", "monthlyPriceUF",
        "monthlyPrice", "deductible", "deductibleDesc", "discount", "stolenVehicle", "totalLoss", "damageThirdParty", "workshopType",
        "dealToken", "itemId", "quotationId", "FIDId", "expiryDate", "brokerageUF", "vehicleReplacement",
        "inspectionRequired", "monthlyPremium", "paymentPlan", "quotationPeriod", "paymentWay", "coverages", "details"})
public class QuotationPlanDto {

    private String uniquePlan;
    private String planId;
    private String insurer;
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
    private String dealToken;
    private Integer itemId;
    private Integer quotationId;
    private String FIDId;
    private String expiryDate;
    private Double brokerageUF;
    private String vehicleReplacement;
    private Integer inspectionRequired;
    private Double monthlyPremium;
    private String paymentPlan;
    private String quotationPeriod;
    private String paymentWay;
    private Set<QuotationPlanCoverDto> coverages;
    private Set<Object> details;

    // Constructor personalizado
    public QuotationPlanDto(String planId, String insurer, String planName, double valueUF, double grossPriceUF,
            int totalMonths, double monthlyPriceUF, double monthlyPrice, int deductible, String deductibleDesc,
            double discount, String stolenVehicle, String totalLoss, String damageThirdParty, String workshopType) {
        this.coverages = new HashSet<>(); // Iniciamos la lista de detalles a vacío
        this.details = new HashSet<>(); // Iniciamos la lista de detalles a vacío
        this.planId = planId;
        this.insurer = insurer;
        this.planName = planName;
        this.valueUF = valueUF;
        this.grossPriceUF = grossPriceUF;
        this.totalMonths = totalMonths;
        this.monthlyPriceUF = monthlyPriceUF;
        this.monthlyPrice = monthlyPrice;
        this.deductible = deductible;
        this.deductibleDesc = deductibleDesc;
        this.discount = discount;
        this.stolenVehicle = stolenVehicle;
        this.totalLoss = totalLoss;
        this.damageThirdParty = damageThirdParty;
        this.workshopType = workshopType;
    }
    
    // Métodos de lógica, propios de la clase
    public Set<Object> addDetail(Object detail) {
        this.details.add(detail);
        return this.details;
    }

    // Métodos de lógica, propios de la clase
    public Set<QuotationPlanCoverDto> addCoverage(QuotationPlanCoverDto coverage) {
        this.coverages.add(coverage);
        return this.coverages;
    }

}
