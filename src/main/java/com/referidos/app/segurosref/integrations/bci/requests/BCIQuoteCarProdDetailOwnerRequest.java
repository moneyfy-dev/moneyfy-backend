package com.referidos.app.segurosref.integrations.bci.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"Int_Rut", "Str_Dv", "Int_Edad"})
public record BCIQuoteCarProdDetailOwnerRequest(
    Integer Int_Rut,
    String Str_Dv,
    Integer Int_Edad
) {

}
