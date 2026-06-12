package com.referidos.app.segurosref.seeders;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.BrandRepository;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "moneyfy.seeders.auto-enabled", havingValue = "true")
public class RunVehicleBrandSeeder implements CommandLineRunner {

    private final BrandRepository brandRepository;

    private final SeedHelper seedHelper;

    @Override
    public void run(String... args) throws Exception {
        Object[] objBrands = seedHelper.updateBrands(brandRepository, false);
        String message = (String) objBrands[0];
        LOGGER_MESSAGES.info("----- SEEDING BRANDS -----");
        LOGGER_MESSAGES.info("Brands Message: " + message);
    }

}
