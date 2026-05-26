package com.referidos.app.segurosref.integrations.bci.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"Int_Marca", "Int_Modelo", "Int_Ano", "Int_TipoVehiculo", "Int_UsoVehiculo",
    "Propietario"})
public record BCIQuoteCarProdDetailRequest(
    Integer Int_Marca,
    Integer Int_Modelo,
    Integer Int_Ano,
    Integer Int_TipoVehiculo,
    Integer Int_UsoVehiculo,
    BCIQuoteCarProdDetailOwnerRequest Propietario
) {

}
