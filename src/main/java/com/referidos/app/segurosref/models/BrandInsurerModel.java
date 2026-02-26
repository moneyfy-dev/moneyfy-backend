package com.referidos.app.segurosref.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"id", "name"})
public class BrandInsurerModel {

    private int id;
    private String name;

}
