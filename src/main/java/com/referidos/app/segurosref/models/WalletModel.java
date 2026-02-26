package com.referidos.app.segurosref.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;

@Data
public class WalletModel {

    private int totalBalance;
    private int outstandingBalance;
    private int availableBalance;
    private int paymentBalance;
    
    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private List<String> transactionIds;

    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private List<String> paymentIds;

    // Constructor personalizado
    public WalletModel(int totalBalance, int outstandingBalance, int availableBalance, int paymentBalance) {
        // Asignamos los demás valores
        this.transactionIds = new ArrayList<>();
        this.paymentIds = new ArrayList<>();
        this.totalBalance = totalBalance;
        this.outstandingBalance = outstandingBalance;
        this.availableBalance = availableBalance;
        this.paymentBalance = paymentBalance;
    }

    // Métodos de lógica, propios de la clase
    public List<String> addTransactionId(String transactionId) {
        this.transactionIds.add(transactionId);
        return this.transactionIds;
    }

    public List<String> addPaymentId(String paymentId) {
        this.paymentIds.add(paymentId);
        return this.paymentIds;
    }

}
