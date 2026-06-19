package com.referidos.app.segurosref.dtos.manager;

import java.util.Set;

import com.referidos.app.segurosref.dtos.report.ReportAccountDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotePaymentDto {
    private String userId;
    private String userTransactionStatus;
    private String userNote;
    private Set<String> transactions;
    private ReportAccountDto userAccount;
    private int userPayment;
    private String userVoucher;
}
