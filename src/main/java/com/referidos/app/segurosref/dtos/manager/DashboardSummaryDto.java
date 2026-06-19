package com.referidos.app.segurosref.dtos.manager;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = { "activeUsers", "paidCommissions", "pendingCommissions", "weeklyMetrics" })
public class DashboardSummaryDto {

    private int activeUsers;
    private int paidCommissions;
    private int pendingCommissions;
    private List<DashboardMetricPointDto> weeklyMetrics;
}
