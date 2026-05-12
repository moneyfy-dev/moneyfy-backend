package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"vehicle", "policyHolder"})
public record FDIItemCrRequest(
    FDIItemCrVehicleRequest vehicle,
    FDIDealUpPayerRequest policyHolder
) {

}
