package com.referidos.app.segurosref.integrations.bci.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"str_Usuario", "str_Clave"})
public record BCITokenCreateRequest(
    String str_Usuario,
    String str_Clave
) {

}
