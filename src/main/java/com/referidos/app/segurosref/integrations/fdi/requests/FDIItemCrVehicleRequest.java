package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"vehicle", "policyHolder"})
public record FDIItemCrVehicleRequest(
    Integer brandId,
    Integer modelId,
    Integer modelYear,
    String plateNumber,
    String chasisNumber,
    String engineNumber
) {

}
