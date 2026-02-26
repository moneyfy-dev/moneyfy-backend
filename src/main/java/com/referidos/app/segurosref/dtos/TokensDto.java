package com.referidos.app.segurosref.dtos;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"jwtSession", "jwtRefresh"})
public record TokensDto(
    String jwtSession,
    String jwtRefresh
) {

}
