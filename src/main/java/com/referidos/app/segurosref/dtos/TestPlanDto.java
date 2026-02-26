package com.referidos.app.segurosref.dtos;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"planId", "insurer", "planName", "valueUF", "grossPriceUF", "totalMonths", "monthlyPriceUF",
        "monthlyPrice", "deductible", "deductibleDesc", "discount", "stolenVehicle", "totalLoss", "damageThirdParty", "workshopType",
        "details"})
public class TestPlanDto {

    private String planId;
    private String insurer;
    private String planName;
    private double valueUF;
    private double grossPriceUF;
    private int totalMonths;
    private double monthlyPriceUF;
    private double monthlyPrice;
    private int deductible;
    private String deductibleDesc;
    private double discount;
    private String stolenVehicle;
    private String totalLoss;
    private String damageThirdParty;
    private String workshopType;
    private Set<Object> details;

    // Constructor personalizado
    public TestPlanDto(String planId, String insurer, String planName, double valueUF, double grossPriceUF,
            int totalMonths, double monthlyPriceUF, double monthlyPrice, int deductible, String deductibleDesc,
            double discount, String stolenVehicle, String totalLoss, String damageThirdParty, String workshopType) {
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

}
