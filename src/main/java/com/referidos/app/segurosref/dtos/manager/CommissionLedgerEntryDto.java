package com.referidos.app.segurosref.dtos.manager;

import java.time.LocalDateTime;

public record CommissionLedgerEntryDto(String transactionId, String quoteId, String userId,
        String userEmail, String userFullname, String status, long amount,
        LocalDateTime createdDate, String quoteStatus, boolean quoteMissing) {
}
