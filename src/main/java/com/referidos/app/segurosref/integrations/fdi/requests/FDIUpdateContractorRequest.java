package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"idNumber", "email", "address"})
public record FDIUpdateContractorRequest(
    String idNumber,
    String email,
    FDIUpdateAddressRequest address
) {

}
