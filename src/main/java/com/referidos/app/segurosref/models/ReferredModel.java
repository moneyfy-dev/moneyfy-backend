package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "referrals")
public class ReferredModel {

    @Id
    private ObjectId referredId;
    private String userReferring;
    private String codeToRefer;
    private String referred;
    private String userReferringStatus;
    private String referredStatus;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Constructor personalizado
    public ReferredModel(String userReferring, String codeToRefer, String referred, String userReferringStatus,
            String referredStatus, LocalDateTime createdDate, LocalDateTime updatedDate) {
        this.userReferring = userReferring;
        this.codeToRefer = codeToRefer;
        this.referred = referred;
        this.userReferringStatus = userReferringStatus;
        this.referredStatus = referredStatus;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getReferredId() {
        return referredId.toString();
    }
    
}
