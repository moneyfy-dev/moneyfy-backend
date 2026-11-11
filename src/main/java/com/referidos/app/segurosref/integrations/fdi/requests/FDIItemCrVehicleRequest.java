package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// Nombres del adaptador local: pendientes de validacion contra el contrato oficial FDI.
@JsonPropertyOrder(value={"modelId", "versionId", "typeId", "modelYear", "plateNumber", "chasisNumber", "engineNumber"})
public record FDIItemCrVehicleRequest(
    Integer modelId,
    Integer versionId,
    Integer typeId,
    Integer modelYear,
    String plateNumber,
    String chasisNumber,
    String engineNumber
) {

}
