package com.referidos.app.segurosref.integrations.fdi;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;
import com.referidos.app.segurosref.models.FdiVehicleMapping;
import com.referidos.app.segurosref.repositories.FdiVehicleMappingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FdiVehicleMappingService {
    private final FdiVehicleMappingRepository repository;

    public FdiVehicleMapping resolve(String brand, String model) {
        var candidates = repository.findByBrandKeyAndModelKey(normalize(brand), normalize(model));
        if (candidates.size() != 1 || !candidates.get(0).usable()) {
            throw new IllegalArgumentException("Homologacion FDI ausente, incompleta o ambigua");
        }
        return candidates.get(0);
    }

    public static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
