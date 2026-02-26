package com.referidos.app.segurosref.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"insurerId", "name", "alias", "darkLogo", "lightLogo"})
@Document(collection = "insurers")
public class InsurerModel {

    @Id
    private ObjectId insurerId;
    private String name;
    private String alias;
    @JsonProperty(access = Access.WRITE_ONLY) // Campo oculto
    private String endpoint;
    private String darkLogo;
    private String lightLogo;
    
    // Constructores personalizados
    public InsurerModel(String name, String alias, String endpoint, String darkLogo, String lightLogo) {
        this.name = name;
        this.alias = alias;
        this.endpoint = endpoint;
        this.darkLogo = darkLogo;
        this.lightLogo = lightLogo;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getInsurerId() {
        return insurerId.toString();
    }
    
}
