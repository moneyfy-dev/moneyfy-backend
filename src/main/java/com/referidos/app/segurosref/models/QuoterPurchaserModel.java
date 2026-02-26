package com.referidos.app.segurosref.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuoterPurchaserModel {

    private String personalId;
    private String name;
    private String paternalSurname;
    private String maternalSurname;
    private String email;
    private String phone;
    private String ownerRelationOption;

}
