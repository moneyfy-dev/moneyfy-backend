package com.referidos.app.segurosref.dtos.commission;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"userId", "message"})
public record CommissionConflictDto(
    String userId,
    String message
) {

}
