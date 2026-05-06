package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"contractor", "payer"})
public record FDIDealUpRequest(
    FDIDealUpContractorRequest contractor,
    FDIDealUpPayerRequest payer
) {

}
