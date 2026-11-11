package com.referidos.app.segurosref.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "fdi_vehicle_mappings")
public record FdiVehicleMapping(@Id String portalCode, Integer portalBrandId, Integer portalModelId,
        String brand, String model, String vehicleType, String brandKey, String modelKey,
        Integer fdiModelId, Integer fdiVersionId, Integer fdiTypeId, boolean enabled) {

    public boolean usable() {
        return enabled && positive(fdiModelId) && positive(fdiVersionId) && positive(fdiTypeId);
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }
}
