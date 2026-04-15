package com.referidos.app.segurosref.dtos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"transactionId", "commissions"})
public class TransactionDto {

    private String transactionId;
    private List<TransactionCommissionDto> commissions;

    public TransactionDto(String transactionId) {
        this.commissions = new ArrayList<>();
        this.transactionId = transactionId;
    }

    public TransactionDto addTransactionCommissionDto(TransactionCommissionDto transactionCommissionDto) {
        this.commissions.add(transactionCommissionDto);
        return this;
    }
    
}
