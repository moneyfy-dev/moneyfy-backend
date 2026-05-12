package com.referidos.app.segurosref.dtos.report;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"transactionId", "commission", "message"})
public record ReportTransactionDataDto(
    String transactionId,
    int commission,
    String message
) {

}
