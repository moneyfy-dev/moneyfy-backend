package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"userId", "userCommission", "commissionStatus", "paymentDate"})
public class TransactionComissionModel {

    private String userId;
    private int userCommission;
    private String commissionStatus;
    private LocalDateTime paymentDate;

}
