package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"planId", "insurer", "planName", "deductible", "stolenVehicle", "totalLoss",
        "damageThirdParty", "workshopType", "details", "createdDate", "updatedDate"})
@Document(collection = "plans")
public class PlanModel {

    @Id
    private String planId;
    private String insurer;
    private String planName;
    private int deductible; // El plan puede ser distinto dependiendo del deducible
    private String stolenVehicle;
    private String totalLoss;
    private String damageThirdParty;
    private String workshopType;
    private Set<Object> details;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Métodos de lógica, propios de la clase
    public Set<Object> addDetail(Object detail) {
        this.details.add(detail);
        return this.details;
    }

}
