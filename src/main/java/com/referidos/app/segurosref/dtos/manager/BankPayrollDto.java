package com.referidos.app.segurosref.dtos.manager;

import com.referidos.app.segurosref.dtos.report.ReportAccountDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankPayrollDto {
    private String userId;
    private ReportAccountDto userAccount;
    private int totalPayment;
}
