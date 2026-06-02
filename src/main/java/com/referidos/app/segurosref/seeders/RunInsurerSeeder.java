package com.referidos.app.segurosref.seeders;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.InsurerRepository;

@Component
@ConditionalOnProperty(name = "moneyfy.seeders.auto-enabled", havingValue = "true")
public class RunInsurerSeeder implements CommandLineRunner {

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private SeedHelper seedHelper;

    @Override
    public void run(String... args) throws Exception {
        Object[] objInsurers = seedHelper.updateInsurers(insurerRepository, false);
        String message = (String) objInsurers[0];
        LOGGER_MESSAGES.info("----- SEEDING INSURERS -----");
        LOGGER_MESSAGES.info("Insurer Message: " + message);
    }

}
