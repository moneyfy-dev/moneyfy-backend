package com.referidos.app.segurosref.seeder;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.repositories.InsurerRepository;

@Component
public class RunInsurerSeeder implements CommandLineRunner {

    private static final String DARK_TEMPLATE =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 120 40\">"
            + "<rect width=\"120\" height=\"40\" rx=\"8\" fill=\"#111827\"/>"
            + "<text x=\"60\" y=\"25\" font-size=\"12\" text-anchor=\"middle\" fill=\"#ffffff\">%s</text>"
            + "</svg>";

    private static final String LIGHT_TEMPLATE =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 120 40\">"
            + "<rect width=\"120\" height=\"40\" rx=\"8\" fill=\"#f3f4f6\" stroke=\"#d1d5db\"/>"
            + "<text x=\"60\" y=\"25\" font-size=\"12\" text-anchor=\"middle\" fill=\"#111827\">%s</text>"
            + "</svg>";

    @Autowired
    private InsurerRepository insurerRepository;

    @Override
    public void run(String... args) throws Exception {
        seedInsurer("Tractor Seguros Automotriz", "aseguradora1", "TRACTOR");
        seedInsurer("Seguros Alameda", "aseguradora2", "ALAMEDA");
        seedInsurer("Los Alamos Seguros Automotriz", "aseguradora3", "ALAMOS");
        seedInsurer("BCI", "aseguradora4", "BCI");

        LOGGER_MESSAGES.info("Insurer Seeder Message: aseguradoras de prueba verificadas");
    }

    private void seedInsurer(String name, String alias, String label) {
        if (insurerRepository.findByAlias(alias).isPresent()) {
            return;
        }

        InsurerModel insurer = new InsurerModel(
                name,
                alias,
                "",
                String.format(DARK_TEMPLATE, label),
                String.format(LIGHT_TEMPLATE, label)
        );
        insurerRepository.save(insurer);
    }
}
