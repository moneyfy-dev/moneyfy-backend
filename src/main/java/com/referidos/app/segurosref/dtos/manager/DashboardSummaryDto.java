package com.referidos.app.segurosref.dtos.manager;

import java.util.List;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = { "activeUsers", "paidCommissions", "pendingCommissions", "pendingApprovalCommissions", "conflictCommissions", "weeklyMetrics", "dateFrom", "dateTo" })
public class DashboardSummaryDto {

    private int activeUsers;
    private long paidCommissions;
    private long pendingCommissions;
    private long pendingApprovalCommissions;
    private long conflictCommissions;
    private List<DashboardMetricPointDto> weeklyMetrics;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
