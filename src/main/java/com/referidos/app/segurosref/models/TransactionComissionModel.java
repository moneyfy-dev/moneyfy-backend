package com.referidos.app.segurosref.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"userId", "userCommission", "commissionStatus"})
public class TransactionComissionModel {

    private String userId;
    private int userCommission;
    private String commissionStatus;

}
