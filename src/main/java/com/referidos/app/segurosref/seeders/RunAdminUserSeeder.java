package com.referidos.app.segurosref.seeders;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.models.ManagerModel;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.repositories.ManagerRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RunAdminUserSeeder implements CommandLineRunner {

    @Value("${moneyfy.admin.email}")
    private String adminEmail;

    @Value("${moneyfy.admin.password}")
    private String adminPassword;

    @Value("${moneyfy.admin.name}")
    private String adminName;

    @Value("${moneyfy.admin.surname}")
    private String adminSurname;

    private final AuthRepository authRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder pwdEncoder;

    @SuppressWarnings("null")
    @Override
    public void run(String... args) throws Exception {
        String email = adminEmail.toLowerCase();
        Optional<AuthModel> authOptional = authRepository.findByEmail(email);
        Optional<ManagerModel> managerOptional = managerRepository.findByEmail(email);

        if (authOptional.isPresent() && managerOptional.isPresent()) {
            ManagerModel manager = managerOptional.get();
            AuthModel auth = authOptional.get();
            boolean needsUpdate = false;

            if (!auth.isAccountConfirmed()) {
                auth.setAccountConfirmed(true);
                authRepository.save(auth);
                needsUpdate = true;
            }

            if (!"Activado".equals(manager.getStatus())) {
                manager.setStatus("Activado");
                managerRepository.save(manager);
                needsUpdate = true;
            }

            if (!needsUpdate) {
                LOGGER_MESSAGES.info("----- SEEDING ADMIN USER -----");
                LOGGER_MESSAGES.info("Admin seed skipped: user already exists for " + email);
                return;
            }
        }

        if (authOptional.isEmpty()) {
            AuthModel authModel = AuthModel.builder()
                    .email(email)
                    .pwd(pwdEncoder.encode(adminPassword))
                    .role("ROLE_ADMIN")
                    .accountConfirmed(true)
                    .tokenRevocationDate(DataHelper.deprecatedDateTime())
                    .codeAuth("")
                    .codeExpirationTime(DataHelper.deprecatedDateTime())
                    .build();
            authRepository.save(authModel);
        }

        if (managerOptional.isEmpty()) {
            ManagerModel managerModel = ManagerModel.builder()
                    .email(email)
                    .name(adminName)
                    .surname(adminSurname)
                    .status("Activado")
                    .build();
            managerRepository.save(managerModel);
        }

        LOGGER_MESSAGES.info("----- SEEDING ADMIN USER -----");
        LOGGER_MESSAGES.info("Admin seed ensured for " + email);
    }
}
