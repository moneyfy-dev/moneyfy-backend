package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.dtos.commission.CommissionAccountDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"paymentId", "userId", "account", "payment", "availableBalanceAfterPayment", "voucher",
        "paymentDate", "createdDate", "updatedDate"})
@Document(collection = "payments")
public class PaymentModel {

    @Id
    private ObjectId paymentId;
    private String userId;
    private CommissionAccountDto account;
    private int payment;
    private int availableBalanceAfterPayment;
    private String voucher;
    private String paymentDate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getPaymentId() {
        return this.paymentId.toString();
    }

}
