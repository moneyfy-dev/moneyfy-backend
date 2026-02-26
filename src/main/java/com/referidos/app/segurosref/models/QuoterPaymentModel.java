package com.referidos.app.segurosref.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuoterPaymentModel {

    private String holderName;
    private String type;
    private String cardNumber; // encriptar / desencriptar
    private String dueDate; // encriptar / desencriptar

}
