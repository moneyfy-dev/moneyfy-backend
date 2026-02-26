package com.referidos.app.segurosref.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuoterCarModel {

    private String ppu;
    private String brand;
    private String model;
    private String year;
    private String colour;
    private String engineNum;
    private String chassisNum;
    private String manufacturer;
    
}
