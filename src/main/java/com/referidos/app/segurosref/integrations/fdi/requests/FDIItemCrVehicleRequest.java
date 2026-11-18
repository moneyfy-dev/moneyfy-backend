package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// FDI QA acepta modelId; versionId y typeId se conservan en el catalogo, no en este JSON.
@JsonPropertyOrder(value={"modelId", "modelYear", "plateNumber", "chasisNumber", "engineNumber"})
public record FDIItemCrVehicleRequest(
    Integer modelId,
    Integer modelYear,
    String plateNumber,
    String chasisNumber,
    String engineNumber
) {

}
