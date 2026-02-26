package com.referidos.app.segurosref.requests;

import java.util.List;

import com.referidos.app.segurosref.models.CityModel;

public record CityRequest(
    String key,
    List<CityModel> cities
) {

}
