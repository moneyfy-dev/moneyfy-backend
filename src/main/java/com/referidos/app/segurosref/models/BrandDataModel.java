package com.referidos.app.segurosref.models;

import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"modelId", "model", "type", "insurersId"})
public class BrandDataModel {

    // Id del modelo
    private ObjectId modelId;
    private String model;
    private String type;
    private List<BrandInsurerModel> insurersId;

    public String getModelId() {
        return this.modelId.toString();
    }

    // Métodos de lógica, propios de la clase
    public BrandDataModel addInsurerModelId(BrandInsurerModel insurerModelId) {
        this.insurersId.add(insurerModelId);
        return this;
    }

}
