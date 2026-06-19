package com.referidos.app.segurosref.seeders;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "moneyfy.seeders.admin-user-enabled", havingValue = "true")
public class RunAdminUserSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "moneyfyapp@gmail.com";
    private static final String ADMIN_PASSWORD = "moneyfy.2026.!";
    private static final String ADMIN_NAME = "Moneyfy";
    private static final String ADMIN_SURNAME = "Admin";

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder pwdEncoder;

    @Override
    public void run(String... args) throws Exception {
        String email = ADMIN_EMAIL.toLowerCase();
        Optional<AuthModel> authOptional = authRepository.findByEmail(email);
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(email);

        if (authOptional.isPresent() && userOptional.isPresent()) {
            LOGGER_MESSAGES.info("----- SEEDING ADMIN USER -----");
            LOGGER_MESSAGES.info("Admin seed skipped: user already exists for " + email);
            return;
        }

        if (authOptional.isEmpty()) {
            AuthModel authModel = AuthModel.builder()
                    .email(email)
                    .pwd(pwdEncoder.encode(ADMIN_PASSWORD))
                    .role("ROLE_ADMIN")
                    .accountConfirmed(true)
                    .tokenRevocationDate(DataHelper.deprecatedDateTime())
                    .codeAuth("")
                    .codeExpirationTime(DataHelper.deprecatedDateTime())
                    .build();
            authRepository.save(authModel);
        }

        if (userOptional.isEmpty()) {
            UserDataModel userData = new UserDataModel(
                    ADMIN_NAME,
                    ADMIN_SURNAME,
                    email,
                    "",
                    "",
                    DataHelper.deprecatedDate(),
                    "Activado",
                    new byte[0]);
            WalletModel wallet = new WalletModel(0, 0, 0, 0);
            NotificationModel notifications = new NotificationModel(
                    true,
                    true,
                    true,
                    false,
                    false,
                    true,
                    false,
                    false,
                    false,
                    new ArrayList<>());
            UserModel userModel = new UserModel(
                    DataHelper.generateCodeToRefer(userRepository),
                    DataHelper.deprecatedDateTime(),
                    userData,
                    wallet,
                    notifications);
            userRepository.save(userModel);
        }

        LOGGER_MESSAGES.info("----- SEEDING ADMIN USER -----");
        LOGGER_MESSAGES.info("Admin seed ensured for " + email);
    }
}
