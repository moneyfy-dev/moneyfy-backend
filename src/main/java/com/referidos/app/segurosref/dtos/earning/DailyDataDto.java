package com.referidos.app.segurosref.dtos.earning;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"date", "totalCommission", "totalAmount", "commissions"})
public class DailyDataDto {

    private String date;
    private int totalCommission;
    private int totalAmount;
    private List<DailyCommissionDto> commissions;

    public List<DailyCommissionDto> addCommission(DailyCommissionDto commission) {
        this.commissions.add(commission);
        return this.commissions;
    }

}
