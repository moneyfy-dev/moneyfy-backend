package com.referidos.app.segurosref.responses;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"message", "status", "internalCode", "data"})
public record GeneralResponses(
    String message,
    int status,
    Object data,
    Integer internalCode
) {

    // Constructor compacto para compatibilidad con versiones anteriores, luego refactorizar respuestas si es necesario
    public GeneralResponses(String message, int status, Object data) {
        this(message, status, data, null); 
    }

}
