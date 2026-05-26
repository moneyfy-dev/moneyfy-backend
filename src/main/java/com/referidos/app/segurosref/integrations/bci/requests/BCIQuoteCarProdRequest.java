package com.referidos.app.segurosref.integrations.bci.requests;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"int_CodigoProducto", "Int_CantidadCuotas", "Int_Vigencia", "Lst_Vehiculos"})
public record BCIQuoteCarProdRequest(
    Integer int_CodigoProducto,
    Integer Int_CantidadCuotas,
    Integer Int_Vigencia,
    List<BCIQuoteCarProdDetailRequest> Lst_Vehiculos
) {

}
