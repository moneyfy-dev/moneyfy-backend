package com.referidos.app.segurosref.dtos;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {
    "ppu", "brand", "model", "year", "type", "colour", "engineNum", "chassisNum", "manufacturer", "isFound"
})
public record VehicleDto(
    String ppu,
    String brand,
    String model,
    String year,
    String type,
    String colour,
    String engineNum,
    String chassisNum,
    String manufacturer,
    boolean isFound
) {

}
