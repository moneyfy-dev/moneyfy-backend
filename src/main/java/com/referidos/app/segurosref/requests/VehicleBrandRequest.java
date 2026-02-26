package com.referidos.app.segurosref.requests;

import java.util.List;

import com.referidos.app.segurosref.models.BrandModel;

public record VehicleBrandRequest(
    String key,
    List<BrandModel> brands
) {

}
