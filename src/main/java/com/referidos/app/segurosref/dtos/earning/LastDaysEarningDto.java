package com.referidos.app.segurosref.dtos.earning;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"days", "finalCommissions", "finalAmount", "lastDay"})
public class LastDaysEarningDto {

    private List<DailyDataDto> days;
    private int finalCommissions;
    private int finalAmount;
    private String lastDay;

}
