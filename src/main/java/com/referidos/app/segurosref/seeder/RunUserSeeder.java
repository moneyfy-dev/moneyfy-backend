package com.referidos.app.segurosref.seeder;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.LogRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

// Clase que se comporta como servicio, al levantarse la aplicación para inyectar la data por defecto
@Component
public class RunUserSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private PasswordEncoder pwdEncoder;

    @Autowired
    private SeedHelper seedHelper;

    // PROCESO QUE SE EJECUTA AL LEVANTARSE LA APLICACIÓN
    @Override
    public void run(String... args) throws Exception {
        LOGGER_MESSAGES.info("----- SEEDING USERS -----");
        LOGGER_MESSAGES.info("Test User Message: " + seedHelper.updateTestUsers(userRepository, referredRepository, deviceRepository, transactionRepository, logRepository, pwdEncoder, true));
        LOGGER_MESSAGES.info("Default User Message: " + seedHelper.updateDefaultUsers(userRepository, referredRepository, deviceRepository, transactionRepository, logRepository, pwdEncoder));
    }

}
