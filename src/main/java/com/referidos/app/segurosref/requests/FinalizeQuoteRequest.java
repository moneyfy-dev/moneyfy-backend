package com.referidos.app.segurosref.requests;

public record FinalizeQuoteRequest(
    String quoterId,
    String transactionStatus
) {

}
