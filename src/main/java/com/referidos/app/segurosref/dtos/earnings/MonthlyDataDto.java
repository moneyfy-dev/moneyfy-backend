package com.referidos.app.segurosref.dtos.earnings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.dtos.commission.CommissionDataDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"month", "totalCommission", "totalAmount", "commissions"})
public class MonthlyDataDto {

    private String month;
    private int totalCommission;
    private int totalAmount;
    private List<CommissionDataDto> commissions;

    // Métodos de lógica, propios de la clase
    public List<CommissionDataDto> addCommission(CommissionDataDto commission) {
        this.commissions.add(commission);
        return this.commissions;
    }

}
