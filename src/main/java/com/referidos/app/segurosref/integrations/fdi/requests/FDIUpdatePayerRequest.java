package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"idNumber", "email", "phone", "address"})
public record FDIUpdatePayerRequest(
    String idNumber,
    String email,
    Integer phone,
    FDIUpdateAddressRequest address
) {

}
