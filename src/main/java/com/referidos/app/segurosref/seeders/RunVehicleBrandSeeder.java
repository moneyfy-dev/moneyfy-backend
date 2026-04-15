package com.referidos.app.segurosref.seeders;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.BrandRepository;

@Component
public class RunVehicleBrandSeeder implements CommandLineRunner {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SeedHelper seedHelper;

    @Override
    public void run(String... args) throws Exception {
        Object[] objBrands = seedHelper.updateBrands(brandRepository, false);
        String message = (String) objBrands[0];
        LOGGER_MESSAGES.info("----- SEEDING BRANDS -----");
        LOGGER_MESSAGES.info("Brands Message: " + message);
    }

}
