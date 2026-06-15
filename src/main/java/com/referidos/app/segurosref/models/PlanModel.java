package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.dtos.quotation.QuotationPlanCoverDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"planId", "insurer", "planName", "deductibleDesc", "stolenVehicle", "totalLoss",
        "damageThirdParty", "workshopType", "coverages", "details", "createdDate", "updatedDate"})
@Document(collection = "plans")
public class PlanModel {

    @Id
    private String planId;
    private String insurer;
    private String planName;
    private String deductibleDesc;
    private String stolenVehicle;
    private String totalLoss;
    private String damageThirdParty;
    private String workshopType;
    private Set<QuotationPlanCoverDto> coverages;
    private List<String> details;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Métodos de lógica, propios de la clase
    public List<String> addDetail(String detail) {
        this.details.add(detail);
        return this.details;
    }

}
