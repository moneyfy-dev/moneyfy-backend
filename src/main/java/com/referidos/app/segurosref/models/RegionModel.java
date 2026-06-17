package com.referidos.app.segurosref.models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder(value = { "regionId", "region", "locations" })
@Document(collection = "regions")
public class RegionModel {

    @Id
    private ObjectId regionId;
    private String region;
    private List<String> locations;

    // Constructor personalizado
    public RegionModel(String region) {
        this.locations = new ArrayList<>();
        this.region = region;
    }

    // Getter personalizado, para obtener el id sin la estructura de objeto.
    public String getRegionId() {
        return this.regionId.toString();
    }

    // Métodos de lógica, propios de la clase
    public RegionModel addLocation(String location) {
        this.locations.add(location);
        return this;
    }

}
