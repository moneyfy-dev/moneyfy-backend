package com.referidos.app.segurosref.dtos.manager;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedPaymentDto {
    private String userId;
    private Set<String> transactions;
    private String message;
}
