package com.referidos.app.segurosref.dtos.manager;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.models.AccountModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder(value = {
    "idUser", "userFullname", "userEmail", "userPhone", "activeAccount",
    "realizedCommissions", "pendingPayments", "ownCommissions", "referredCommissions",
    "totalCommissions", "paidCommissions"
})
public class MoneyfyerDto {
    private String idUser;
    private String userFullname;
    private String userEmail;
    private String userPhone;
    private AccountModel activeAccount;
    private int realizedCommissions;
    private int pendingPayments;
    private int ownCommissions;
    private int referredCommissions;
    private int totalCommissions;
    private int paidCommissions;
}
