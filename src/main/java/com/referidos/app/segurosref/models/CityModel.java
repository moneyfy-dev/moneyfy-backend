package com.referidos.app.segurosref.models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = {"cityId", "city", "locations"})
@Document(collection = "cities")
public class CityModel {

    @Id
    private ObjectId cityId;
    private String city;
    private List<String> locations;

    // Constructor personalizado
    public CityModel(String city) {
        this.locations = new ArrayList<>();
        this.city = city;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getCityId() {
        return this.cityId.toString();
    }

    // Métodos de lógica, propios de la clase
    public List<String> addLocation(String location) {
        this.locations.add(location);
        return this.locations;
    }

}
