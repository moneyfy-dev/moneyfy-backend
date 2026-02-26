package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountModel {

    private ObjectId accountId;
    private String personalId;
    private String holderName;
    private String alias;
    private String email;
    private String bank;
    private String accountType;
    private String accountNumber;
    private boolean selected;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getAccountId() {
        return accountId.toString();
    }

}
