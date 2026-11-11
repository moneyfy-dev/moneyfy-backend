package com.referidos.app.segurosref.repositories;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.referidos.app.segurosref.models.FdiVehicleMapping;

public interface FdiVehicleMappingRepository extends MongoRepository<FdiVehicleMapping, String> {
    List<FdiVehicleMapping> findByBrandKeyAndModelKey(String brandKey, String modelKey);
}
