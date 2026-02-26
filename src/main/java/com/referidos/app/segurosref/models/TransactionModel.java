package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"transactionId", "planId", "userId", "quoterId", "status", "commissionTotal", "commissionScope",
        "observation", "commissions", "createdDate", "updatedDate", "approvalDate"})
@Document(collection = "transactions")
public class TransactionModel {

    @Id
    private String transactionId;
    private String planId;
    private String userId;
    private String quoterId;
    private String status;
    private int commissionTotal;
    private int commissionScope;
    private String observation;
    private List<TransactionComissionModel> commissions;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime approvalDate;

    // Constructor personalizado
    public TransactionModel(String transactionId, String planId, String userId, String quoterId, String status,
            int commissionTotal, int commissionScope, String observation, LocalDateTime createdDate,
            LocalDateTime updatedDate, LocalDateTime approvalDate) {
        this.commissions = new ArrayList<>();
        this.transactionId = transactionId;
        this.planId = planId;
        this.userId = userId;
        this.quoterId = quoterId;
        this.status = status;
        this.commissionTotal = commissionTotal;
        this.commissionScope = commissionScope;
        this.observation = observation;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.approvalDate = approvalDate;
    }

    // Métodos de lógica, propios de la clase
    public List<TransactionComissionModel> addCommission(TransactionComissionModel commission) {
        this.commissions.add(commission);
        return this.commissions;
    }

}
