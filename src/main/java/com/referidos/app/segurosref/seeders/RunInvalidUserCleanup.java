package com.referidos.app.segurosref.seeders;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.LogRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

@Component
@ConditionalOnProperty(name = "moneyfy.cleanup.invalid-users-enabled", havingValue = "true")
public class RunInvalidUserCleanup implements CommandLineRunner {

    private static final List<String> INVALID_USER_EMAILS = List.of(
            "alejandro.osses.r@gmail.com",
            "alejandro@fenrir.cl"
    );

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
    private SeedHelper seedHelper;

    @Override
    @Transactional
    public void run(String... args) {
        LOGGER_MESSAGES.info("----- CLEANUP INVALID USERS -----");
        for(String email : INVALID_USER_EMAILS) {
            Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(email);
            if(userOptional.isPresent()) {
                seedHelper.deleteUserAndDependencies(userOptional.get(), userRepository, referredRepository,
                        deviceRepository, transactionRepository, logRepository);
                LOGGER_MESSAGES.info("Invalid user deleted: " + email);
            } else {
                LOGGER_MESSAGES.info("Invalid user not found: " + email);
            }
        }
    }

}
