package com.referidos.app.segurosref.dtos.earnings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"months", "finalCommissions", "finalAmount", "lastMonth"})
public class MonthlyEarningDto {

    private List<MonthlyDataDto> months;
    private int finalCommissions;
    private int finalAmount;
    private String lastMonth;

}
