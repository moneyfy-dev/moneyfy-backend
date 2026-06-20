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

        boolean wasAuthCreatedOrUpdated = false;
        boolean wasManagerCreatedOrUpdated = false;

        // 1. Validar y/o Actualizar AuthModel
        if (authOptional.isPresent()) {
            AuthModel auth = authOptional.get();
            boolean updateAuth = false;

            if (!auth.isAccountConfirmed()) {
                auth.setAccountConfirmed(true);
                updateAuth = true;
            }

            if (!pwdEncoder.matches(adminPassword, auth.getPwd())) {
                auth.setPwd(pwdEncoder.encode(adminPassword));
                updateAuth = true;
            }

            if (updateAuth) {
                authRepository.save(auth);
                wasAuthCreatedOrUpdated = true;
            }
        } else {
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
            wasAuthCreatedOrUpdated = true;
        }

        // 2. Validar y/o Actualizar ManagerModel
        if (managerOptional.isPresent()) {
            ManagerModel manager = managerOptional.get();
            if (!"Activado".equals(manager.getStatus())) {
                manager.setStatus("Activado");
                managerRepository.save(manager);
                wasManagerCreatedOrUpdated = true;
            }
        } else {
            ManagerModel managerModel = ManagerModel.builder()
                    .email(email)
                    .name(adminName)
                    .surname(adminSurname)
                    .status("Activado")
                    .build();
            managerRepository.save(managerModel);
            wasManagerCreatedOrUpdated = true;
        }

        LOGGER_MESSAGES.info("----- SEEDING ADMIN USER -----");
        if (wasAuthCreatedOrUpdated || wasManagerCreatedOrUpdated) {
            LOGGER_MESSAGES.info("Admin seed ensured/updated for " + email);
        } else {
            LOGGER_MESSAGES.info("Admin seed skipped: user already exists and is up to date for " + email);
        }
    }
}