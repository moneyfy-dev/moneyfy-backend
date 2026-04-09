package com.referidos.app.segurosref.seeder;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.InsurerRepository;

@Component
public class RunInsurerSeeder implements CommandLineRunner {

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private SeedHelper seedHelper;

    @Override
    public void run(String... args) throws Exception {
        LOGGER_MESSAGES.info("----- SEEDING INSURERS -----");
        LOGGER_MESSAGES.info("Insurer Message: " + seedHelper.updateInsurers(insurerRepository, false));
    }

}
