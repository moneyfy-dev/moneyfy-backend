package com.referidos.app.segurosref.dtos.manager;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayQuotesReportResponse {
    private List<BankPayrollDto> bankPayroll;
    private List<UserQuotePaymentDto> backendPayload;
    private List<ConflictDto> conflicts;
}
