package com.referidos.app.segurosref.requests;

import java.util.List;

public record FinalizeQuoteRequest(List<UserQuoteUpdate> usersQuotes) {

    public record UserQuoteUpdate(String userId, List<QuoteUpdate> quotes) {
    }

    public record QuoteUpdate(String quoterId, String transactionStatus) {
    }

}
