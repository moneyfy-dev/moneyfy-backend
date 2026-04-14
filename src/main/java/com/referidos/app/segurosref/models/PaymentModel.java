package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.dtos.report.ReportAccountDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"paymentId", "userId", "account", "payment", "voucher", "paymentDate", "transactionsId", "createdDate", "updatedDate"})
@Document(collection = "payments")
public class PaymentModel {

    @Id
    private ObjectId paymentId;
    private String userId;
    private ReportAccountDto account;
    private int payment;
    private String voucher;
    private String paymentDate;
    private Set<String> transactionsId;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getPaymentId() {
        return this.paymentId.toString();
    }
    // Métodos de lógica, propios de la clase
    public Set<String> addTransactionId(String transactionId) {
        this.transactionsId.add(transactionId);
        return this.transactionsId;
    }

}
