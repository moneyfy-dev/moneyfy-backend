package com.referidos.app.segurosref.seeders;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.integrations.fdi.FdiVehicleMappingService;
import com.referidos.app.segurosref.models.FdiVehicleMapping;
import com.referidos.app.segurosref.repositories.FdiVehicleMappingRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fdi.homologation.import-enabled", havingValue = "true")
public class FdiVehicleMappingImporter implements CommandLineRunner {
    private final FdiVehicleMappingRepository repository;
    private final ObjectMapper mapper;

    public static List<FdiVehicleMapping> readCatalog(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("catalogs/fdi-vehicle-mappings.json").getInputStream()) {
            List<FdiVehicleMapping> catalog = mapper.readValue(input, new TypeReference<>() { });
            var keys = new HashSet<String>();
            for (var row : catalog) {
                if (row.portalBrandId() == null || row.portalBrandId() <= 0
                        || row.portalModelId() == null || row.portalModelId() <= 0
                        || !((row.portalBrandId() + "_" + row.portalModelId()).equals(row.portalCode()))
                        || !keys.add(row.portalCode())
                        || FdiVehicleMappingService.normalize(row.brand()).isBlank()
                        || FdiVehicleMappingService.normalize(row.model()).isBlank()
                        || !FdiVehicleMappingService.normalize(row.brand()).equals(row.brandKey())
                        || !FdiVehicleMappingService.normalize(row.model()).equals(row.modelKey())
                        || (row.enabled() && !row.usable())) {
                    throw new IllegalArgumentException("Catalogo FDI invalido; no se importa ninguna fila");
                }
            }
            return catalog;
        }
    }

    @Override
    public void run(String... args) throws IOException {
        // _id = portalCode: repetir la importacion actualiza, no duplica. No borra otros registros.
        repository.saveAll(readCatalog(mapper));
    }
}
