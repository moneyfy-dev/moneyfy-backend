package com.referidos.app.segurosref.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"brandId", "brand", "models"})
public record VehicleBrandDto(
    String brandId,
    String brand,
    List<VehicleModelDto> models
) {

}
