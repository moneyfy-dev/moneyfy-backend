package com.referidos.app.segurosref.models;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuoterModel {

    private ObjectId quoterId;
    private String quoterStatus;
    private QuoterOwnerModel quoterOwnerData;
    private QuoterCarModel quoterCarData;
    private QuoterPurchaserModel quoterPurchaserData;
    private QuoterPlanModel quoterPlanData;
    private QuoterAddressModel quoterAddressData;
    private QuoterPaymentModel quoterPayment;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getQuoterId() {
        return quoterId.toString();
    }
    
}
