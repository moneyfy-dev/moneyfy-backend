package com.referidos.app.segurosref.integrations.bci.requests;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"Int_FormaPago", "Int_RutCorredor", "Int_Comision", "int_RutCliente",
    "str_DvCliente", "int_RutEjecutivo", "Lst_Productos"})
public record BCIQuoteCarRequest(
    Integer Int_FormaPago,
    Integer Int_RutCorredor,
    Integer Int_Comision,
    Integer int_RutCliente,
    String str_DvCliente,
    Integer int_RutEjecutivo,
    List<BCIQuoteCarProdRequest> Lst_Productos
) {

}
