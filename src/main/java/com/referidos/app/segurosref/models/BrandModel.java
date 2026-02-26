package com.referidos.app.segurosref.models;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"brandId", "brand", "insurersId", "models"})
@Document(collection = "brands")
public class BrandModel {

    @Id
    private ObjectId brandId;
    private String brand;
    private List<BrandInsurerModel> insurersId;
    private List<BrandDataModel> models;
    
    // Constructor personalizado
    public BrandModel(String brand, List<BrandInsurerModel> insurersId, List<BrandDataModel> models) {
        this.brand = brand;
        this.insurersId = insurersId;
        this.models = models;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getBrandId() {
        return this.brandId.toString();
    }
    
    // Métodos de lógica, propios de la clase
    public List<BrandInsurerModel> addInsurerBrandId(BrandInsurerModel insurerBrandId) {
        this.insurersId.add(insurerBrandId);
        return this.insurersId;
    }
    public List<BrandDataModel> addModel(BrandDataModel model) {
        this.models.add(model);
        return this.models;
    }

}
