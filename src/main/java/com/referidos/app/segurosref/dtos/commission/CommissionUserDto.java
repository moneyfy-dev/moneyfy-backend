package com.referidos.app.segurosref.dtos.commission;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"userId", "name", "email", "commission", "message", "account", "transactionIds"})
public class CommissionUserDto {

    private String userId;
    private String name;
    private String email;
    private int commission;
    private String message;
    private CommissionAccountDto account;
    private Set<String> transactionIds;

    // Constructor personalizado
    public CommissionUserDto(String userId, String name, String email, int commission, String message, CommissionAccountDto account) {
        this.transactionIds = new HashSet<>();
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.commission = commission;
        this.message = message;
        this.account = account;
    }
    
    // Métodos de lógica, propios de la clase
    public Set<String> addTransactionId(String transactionId) {
        this.transactionIds.add(transactionId);
        return this.transactionIds;
    }
    
}
