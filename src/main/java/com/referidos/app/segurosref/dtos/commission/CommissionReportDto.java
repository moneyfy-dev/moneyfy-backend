package com.referidos.app.segurosref.dtos.commission;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"payments", "conflicts", "transactionIds", "totalUsers", "totalPaymentTransactions",
        "totalPaymentUsers", "cutoffDate", "paymentDate"})
public class CommissionReportDto {

    private List<CommissionPaymentDto> payments;
    private List<CommissionConflictDto> conflicts;
    private List<String> transactionIds; 
    private int totalUsers;
    private int totalPaymentTransactions;
    private int totalPaymentUsers;
    private String cutoffDate;
    private String paymentDate;

    // Constructor personalizado
    public CommissionReportDto(int totalUsers, int totalPaymentTransactions, int totalPaymentUsers, String cutoffDate, String paymentDate) {
        this.payments = new ArrayList<>();
        this.conflicts = new ArrayList<>();
        this.transactionIds = new ArrayList<>();
        this.totalUsers = totalUsers;
        this.totalPaymentTransactions = totalPaymentTransactions;
        this.totalPaymentUsers = totalPaymentUsers;
        this.cutoffDate = cutoffDate;
        this.paymentDate = paymentDate;
    }
    
    // Métodos de lógica, propios de la clase
    public List<CommissionPaymentDto> addPayment(CommissionPaymentDto payment) {
        this.payments.add(payment);
        return this.payments;
    }

    public List<CommissionConflictDto> addConflict(CommissionConflictDto conflict) {
        this.conflicts.add(conflict);
        return this.conflicts;
    }

    public List<String> addTransactionId(String transactionId) {
        this.transactionIds.add(transactionId);
        return this.transactionIds;
    }
    
}
