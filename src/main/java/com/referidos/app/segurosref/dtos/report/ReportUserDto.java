package com.referidos.app.segurosref.dtos.report;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Solo usar lo que yo diga
@JsonPropertyOrder(value = {"userId", "name", "email", "totalCommission", "voucher", "generalMessage", "account", "transactionData"})
public class ReportUserDto {

    @EqualsAndHashCode.Include // Incluir en equals/hashCode
    private String userId;
    @EqualsAndHashCode.Include // Incluir en equals/hashCode
    private String name;
    private String email;
    private int totalCommission;
    private String voucher;
    private String generalMessage;
    private ReportAccountDto account;
    private Set<ReportTransactionDataDto> transactionData;

    // Constructor personalizado
    public ReportUserDto(String userId, String name, String email, int totalCommission, String voucher, String generalMessage, ReportAccountDto account) {
        this.transactionData = new HashSet<>();
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.totalCommission = totalCommission;
        this.voucher = voucher;
        this.generalMessage = generalMessage;
        this.account = account;
    }
    
    // Personalizar método setter
    public ReportUserDto setTransactionData(Set<ReportTransactionDataDto> transactionsData) {
        this.transactionData = transactionsData;
        return this;
    }

    // Métodos de lógica, propios de la clase
    public ReportUserDto addTransactionData(ReportTransactionDataDto transactionData) {
        this.transactionData.add(transactionData);
        return this;
    }
    
}
