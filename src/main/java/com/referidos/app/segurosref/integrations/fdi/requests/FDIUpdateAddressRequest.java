package com.referidos.app.segurosref.integrations.fdi.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value={"streetName", "building", "countryCode", "regionCode", "communeCode"})
public record FDIUpdateAddressRequest(
    String streetName,
    Integer building,
    String countryCode,
    String regionCode,
    String communeCode
) {

}
