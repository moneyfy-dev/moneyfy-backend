package com.referidos.app.segurosref.responses;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"message", "status", "data"})
public record GeneralResponses(
    String message,
    int status,
    Object data
) {

}
