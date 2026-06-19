package com.referidos.app.segurosref.dtos.manager;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = { "date", "label", "commissions", "sales", "users" })
public class DashboardMetricPointDto {

    private String date;
    private String label;
    private int commissions;
    private int sales;
    private int users;
}
