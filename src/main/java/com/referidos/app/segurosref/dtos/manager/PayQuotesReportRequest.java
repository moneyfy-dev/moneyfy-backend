package com.referidos.app.segurosref.dtos.manager;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayQuotesReportRequest {
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
