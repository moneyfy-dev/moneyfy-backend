package com.referidos.app.segurosref.dtos;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"userId", "userEmail"})
public class TransactionCommissionDto {

    private String userId;
    private String userEmail;

}
