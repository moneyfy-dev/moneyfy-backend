package com.referidos.app.segurosref.requests;

public record SearchPlanRequest(
    String quoterId,
    String ppu,
    String brand,
    String model,
    String year,
    String insurerAlias,
    String requestType, // Tipo de la consulta, si se hizo de la forma "Manual" o "Auto"
    String purchaserId,
    String purchaserName,
    String purchaserPaternalSur,
    String purchaserMaternalSur,
    String purchaserEmail,
    String purchaserPhone,
    String ownerRelationOption
) {

}
