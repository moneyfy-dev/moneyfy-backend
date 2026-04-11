package com.referidos.app.segurosref.dtos.commission;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.dtos.earnings.MonthlyCommissionDto;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"userId", "status", "account", "payment", "voucher", "commissions"})
public class CommissionPaymentDto {

    private String userId;
    private String status;
    private CommissionAccountDto account;
    private int payment;
    private String voucher;
    private List<MonthlyCommissionDto> commissions;

    // Constructor personalizado
    public CommissionPaymentDto(String userId, String status, CommissionAccountDto account, int payment, String voucher) {
        this.commissions = new ArrayList<>();
        this.userId = userId;
        this.status = status;
        this.account = account;
        this.payment = payment;
        this.voucher = voucher;
    }

    // Métodos de lógica, propios de la clase
    public List<MonthlyCommissionDto> addCommission(MonthlyCommissionDto commission) {
        this.commissions.add(commission);
        return this.commissions;
    }

}
