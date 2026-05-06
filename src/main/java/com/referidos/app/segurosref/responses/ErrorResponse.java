package com.referidos.app.segurosref.responses;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"message", "internalErrorCode", "data"})
public record ErrorResponse<T> (
    String message,
    int internalErrorCode,
    T data
) {

}
