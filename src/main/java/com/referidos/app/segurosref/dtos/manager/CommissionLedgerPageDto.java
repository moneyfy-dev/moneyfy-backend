package com.referidos.app.segurosref.dtos.manager;

import java.time.LocalDate;
import java.util.List;

public record CommissionLedgerPageDto(List<CommissionLedgerEntryDto> content, int page, int size,
        long totalElements, int totalPages, long totalAmount, LocalDate dateFrom, LocalDate dateTo,
        String status) {
}
