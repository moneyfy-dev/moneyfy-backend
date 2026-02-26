package com.referidos.app.segurosref.dtos;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"modelId", "model"})
public record VehicleModelDto(
    String modelId,
    String model
) {
    
}
