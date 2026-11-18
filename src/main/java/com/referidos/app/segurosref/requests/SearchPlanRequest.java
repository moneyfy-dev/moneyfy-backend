package com.referidos.app.segurosref.requests;

import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpAddressRequest;

public record SearchPlanRequest(
        String quoterId,
        String ppu,
        String brand,
        String model,
        String year,
        String insurerAlias,
        String purchaserId,
        String purchaserName,
        String purchaserPaternalSur,
        String purchaserMaternalSur,
        String purchaserEmail,
        String purchaserPhone,
        String ownerRelationOption,
        FDIDealUpAddressRequest fdiAddress) {

    // Compatibilidad con los consumidores Java anteriores; BCI no requiere fdiAddress.
    public SearchPlanRequest(String quoterId, String ppu, String brand, String model, String year,
            String insurerAlias, String purchaserId, String purchaserName, String purchaserPaternalSur,
            String purchaserMaternalSur, String purchaserEmail, String purchaserPhone, String ownerRelationOption) {
        this(quoterId, ppu, brand, model, year, insurerAlias, purchaserId, purchaserName,
                purchaserPaternalSur, purchaserMaternalSur, purchaserEmail, purchaserPhone, ownerRelationOption, null);
    }

}
