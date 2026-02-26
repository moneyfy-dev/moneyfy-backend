package com.referidos.app.segurosref.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"quoterPlanId", "insurer", "planName", "valueUF", "grossPriceUF", "totalMonths",
        "monthlyPriceUF", "monthlyPrice", "deductible", "discount"})
public class QuoterPlanModel {

    private String quoterPlanId;
    private String insurer;
    private String planName;
    private double valueUF; // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
    private double grossPriceUF; // Este campo debería ser fijo
    private int totalMonths; // Este campo debería ser fijo
    private double monthlyPriceUF; // Este campo debería ser fijo
    private double monthlyPrice; // Este campo varía según el valor del UF del día / COMO ACTUALIZARLO
    private int deductible;
    private double discount;

}
