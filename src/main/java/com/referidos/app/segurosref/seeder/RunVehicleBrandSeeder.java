package com.referidos.app.segurosref.seeder;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.QuoterHelper;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.repositories.BrandRepository;

@Component
public class RunVehicleBrandSeeder implements CommandLineRunner {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private QuoterHelper quoterHelper;

    @Override
    public void run(String... args) throws Exception {
        List<BrandModel> demoBrands = List.of(
                createBrand("CHEVROLET", "CAPTIVA"),
                createBrand("TOYOTA", "COROLLA"),
                createBrand("BMW", "3 SERIES"),
                createBrand("FORD", "FIESTA"),
                createBrand("MERCEDES-BENZ", "C-CLASS"),
                createBrand("OPEL", "CORSA")
        );

        List<BrandModel> brandsDB = quoterHelper.updateVehicleBrands(brandRepository, demoBrands);
        if (brandsDB == null) {
            LOGGER_MESSAGES.info("Vehicle Brand Seeder Message: no fue posible verificar las marcas de demo");
            return;
        }

        brandRepository.saveAll(brandsDB);
        LOGGER_MESSAGES.info("Vehicle Brand Seeder Message: marcas y modelos de demo verificados");
    }

    private BrandModel createBrand(String brand, String... models) {
        List<BrandDataModel> brandModels = new ArrayList<>();
        for (String model : models) {
            brandModels.add(new BrandDataModel(null, model, new ArrayList<>()));
        }

        return new BrandModel(brand, new ArrayList<>(), brandModels);
    }
}
