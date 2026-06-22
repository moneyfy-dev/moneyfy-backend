package com.referidos.app.segurosref.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuoterAddressModel {

    private String region;
    private String commune;
    private String street;
    private String streetNumber;
    private String department;

}
