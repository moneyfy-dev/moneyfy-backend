package com.referidos.app.segurosref.dtos.manager;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingQuoteDto {
    private String userId;
    private String userFullName;
    private String userEmail;
    
    private String quotationId;
    private String quotationDate;
    private String quotationStatus;
    
    private String insurer;
    private String planId;
    private String planName;
    
    private String vehiclePlate;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    
    private String ownerRut;
    private String ownerFullName;
    
    private String buyerRut;
    private String buyerFullName;
    private String buyerEmail;
    private String buyerPhone;
    
    private String region;
    private String commune;
    private String street;
    private String streetNumber;

    // BCI Specific fields
    private Integer intNroTarificacionBCI;
    private String strNroCotizacionBCI;
    private String dtFinVigenciaBCI;

    // FDI Specific fields
    private String dealTokenFDI;
    private Integer itemIdFDI;
    private Integer quotationIdFDI;
    private String fidIdFDI;
    private String expiryDateFDI;
}
