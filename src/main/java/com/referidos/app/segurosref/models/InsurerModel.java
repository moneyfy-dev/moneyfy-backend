package com.referidos.app.segurosref.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder(value = {"insurerId", "name", "alias"})
@Document(collection = "insurers")
public class InsurerModel {

    @Id
    private ObjectId insurerId;
    private String name;
    private String alias;

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getInsurerId() {
        return insurerId.toString();
    }
    
}
