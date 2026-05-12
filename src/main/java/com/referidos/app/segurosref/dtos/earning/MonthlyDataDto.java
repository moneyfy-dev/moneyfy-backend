package com.referidos.app.segurosref.dtos.earning;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"month", "totalCommission", "totalAmount", "commissions"})
public class MonthlyDataDto {

    private String month;
    private int totalCommission;
    private int totalAmount;
    private List<MonthlyCommissionDto> commissions;

    // Métodos de lógica, propios de la clase
    public List<MonthlyCommissionDto> addCommission(MonthlyCommissionDto commission) {
        this.commissions.add(commission);
        return this.commissions;
    }

}
