package com.referidos.app.segurosref.services.impl;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.integrations.email.providers.EmailAppProvider;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.models.NotificationDataModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.ConfirmUserRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final ReferredRepository referredRepository;
    private final EmailAppProvider emailAppProvider;
    private final ValidateInputHelper validateInputHelper;
    private final UserHelper userHelper;
    private final PasswordEncoder pwdEncoder;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String userEmail = email.toLowerCase();
        Optional<AuthModel> authOptional = authRepository.findByEmail(userEmail);

        if (authOptional.isEmpty()) {
            throw new UsernameNotFoundException(String.format("El usuario %s no ha sido encontrado", userEmail));
        }

        return this.buildUserDetails(authOptional.get());
    }

    private UserDetails buildUserDetails(AuthModel auth) {
        return new User(auth.getEmail(),
                auth.getPwd(),
                true, true, true, true,
                org.springframework.security.core.authority.AuthorityUtils
                        .commaSeparatedStringToAuthorityList(auth.getRole()));
    }

    public ResponseEntity<GeneralResponse> userRegister(UserRegisterRequest userRegister) {
        String[] userReferring = this.validateCodeToRefer(userRegister.codeToRefer());
        if (userReferring == null) {
            return ResponseHelper.locked("el código del referido es inválido", null);
        }

        UserDataModel userData = this.createUserData(userRegister.name().strip(), userRegister.surname().strip(),
                userRegister.email().toLowerCase().strip());
        WalletModel wallet = new WalletModel(0, 0, 0, 0);
        NotificationModel notifs = new NotificationModel(true, true, true,
                false, false, true, false, false, false, new ArrayList<>());

        return this.createUnconfirmedUser(userReferring, userData, wallet, notifs, userRegister.pwd());
    }

    @SuppressWarnings("null")
    @Transactional
    private ResponseEntity<GeneralResponse> createUnconfirmedUser(String[] userReferring, UserDataModel userData,
            WalletModel wallet, NotificationModel notifs, String rawPwd) {
        String email = userData.getEmail();
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(email);
        Optional<AuthModel> authOptional = authRepository.findByEmail(email);

        if (userOptional.isPresent() && authOptional.isPresent()) {
            UserModel userDB = userOptional.get();
            UserDataModel userDataDB = userDB.getPersonalData();
            AuthModel authDB = authOptional.get();

            if (authDB.isAccountConfirmed()) {
                String statusUserDB = userDataDB.getStatus();
                switch (statusUserDB) {
                    case "Activado" -> {
                        return ResponseHelper.gone("usuario existente", null);
                    }
                    case "Desactivado" -> {
                        if (!userHelper.makeUserObsolete(userRepository, referredRepository, userDB)) {
                            return ResponseHelper.gone("usuario existente", null);
                        }
                        break;
                    }
                    default -> {
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    }
                }
            } else {
                Optional<ReferredModel> referredOptional = referredRepository.findByReferred(email);
                if (referredOptional.isPresent()) {
                    referredRepository.delete(referredOptional.get());
                }
                userRepository.delete(userDB);
                authRepository.delete(authDB);
            }
        }

        String codeAuth = DataHelper.generateRandomCode();
        emailAppProvider.sendAuthCodeToRegisterUser(new String[] { email }, codeAuth);

        String userReferringState = (userReferring[0].equals("Sin usuario")) ? "Desactivado" : "Activado";
        LocalDateTime currenDateTime = LocalDateTime.now();

        // Crear AuthModel (Aun no confirmado, sin tokenRevocationDate ni refreshToken)
        AuthModel authModel = AuthModel.builder()
                .email(email)
                .pwd(pwdEncoder.encode(rawPwd))
                .role("ROLE_USER")
                .codeAuth(pwdEncoder.encode(codeAuth))
                .codeExpirationTime(currenDateTime)
                .accountConfirmed(false)
                .tokenRevocationDate(currenDateTime)
                .build();
        authRepository.save(authModel);

        UserModel userModel = new UserModel("", DataHelper.deprecatedDateTime(), userData, wallet, notifs);
        ReferredModel referredModel = new ReferredModel(userReferring[0], userReferring[1], email, userReferringState,
                "Desactivado", currenDateTime, currenDateTime);

        userRepository.save(userModel);
        referredRepository.save(referredModel);

        return ResponseHelper.ok(
                "el código de confirmación para finalizar el proceso de registro, ha sido enviado al email: " + email,
                Collections.singletonMap("info", (Object) "ok"));
    }

    @Transactional
    public ResponseEntity<GeneralResponse> confirmRegistration(ConfirmUserRequest confirm)
            throws JsonProcessingException {
        String userEmail = confirm.email().toLowerCase();
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        AuthModel authDB = authRepository.findByEmail(userEmail).orElseThrow();

        if (!authDB.isAccountConfirmed()) {
            boolean isCodeActive = isCodeActive(authDB.getCodeExpirationTime(), LocalDateTime.now(), 3);
            boolean codeMatches = pwdEncoder.matches(confirm.code(), authDB.getCodeAuth());

            if (isCodeActive && codeMatches) {
                return this.successfulRegistration(userDB, authDB);
            }
            return ResponseHelper.gone("el código ha expirado o no es correcto", null);
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    private ResponseEntity<GeneralResponse> successfulRegistration(UserModel userDB, AuthModel authDB)
            throws JsonProcessingException {
        UserDataModel userData = userDB.getPersonalData();
        String userEmail = userData.getEmail();
        String codeToRefer = DataHelper.generateCodeToRefer(userRepository);
        userDB.setCodeToRefer(codeToRefer);

        authDB.setAccountConfirmed(true);
        authRepository.save(authDB);

        String sessionToken = JwtConfig.createSessionToken(userEmail,
                org.springframework.security.core.authority.AuthorityUtils
                        .commaSeparatedStringToAuthorityList(authDB.getRole()));
        String refreshToken = JwtConfig.createRefreshToken(userEmail);

        userData.setStatus("Activado");
        userDB = userRepository.save(userDB);

        LocalDateTime currenDateTime = LocalDateTime.now();

        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(userEmail);
        if (referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferredStatus("Activado");
            referredByUserA.setUpdatedDate(currenDateTime);
            referredRepository.save(referredByUserA);

            if (referredByUserA.getUserReferringStatus().equals("Activado")) {
                try {
                    String fullNameReferredUser = userData.getName() + " " + userData.getSurname();
                    String userAEmail = referredByUserA.getUserReferring();
                    UserModel userA = userRepository.findByPersonalData_Email(userAEmail).orElseThrow();

                    String message = "El usuario " + fullNameReferredUser
                            + ", se ha acaba de registrar con tu código de referidos!";
                    NotificationModel userANotifPreference = userA.getNotifPreference();
                    NotificationDataModel newNotifUserA = DataHelper.novaNotification(message, "Usuario Referido",
                            currenDateTime);
                    userANotifPreference.addNotif(newNotifUserA);
                    userRepository.save(userA);

                    if (userANotifPreference.isByEmail() && userANotifPreference.isReferredRegistered()) {
                        String userACodeToRefer = userA.getCodeToRefer();
                        emailAppProvider.novaUserRegisteredByCodeToRefer(userAEmail, userACodeToRefer,
                                fullNameReferredUser);
                    }
                } catch (NoSuchElementException e) {
                    LOGGER_MESSAGES.info("No es posible identificar al usuario que ha referido");
                }
            }
        }

        Map<String, Object> responseData = DataHelper.buildUserAuthData(userDB, sessionToken, refreshToken);

        return ResponseHelper.created("usuario registrado exitosamente", responseData);
    }

    @Transactional
    public ResponseEntity<GeneralResponse> userLogin(UserLoginRequest requestUserLoginDto)
            throws JsonProcessingException {
        String email = requestUserLoginDto.email().toLowerCase();
        String pwd = requestUserLoginDto.pwd();

        Optional<AuthModel> authOptional = authRepository.findByEmail(email);
        if (authOptional.isPresent()) {
            AuthModel authDB = authOptional.isPresent() ? authOptional.get() : null;
            if (authDB != null && authDB.getRole() != null && authDB.getRole().contains("ROLE_USER")
                    && pwdEncoder.matches(pwd, authDB.getPwd())) {
                UserModel userDB = userRepository.findByPersonalData_Email(email).orElseThrow();
                UserDataModel userData = userDB.getPersonalData();
                String statusUserDB = userData.getStatus();

                switch (statusUserDB) {
                    case "Activado" -> {
                        String sessionToken = JwtConfig.createSessionToken(email,
                                org.springframework.security.core.authority.AuthorityUtils
                                        .commaSeparatedStringToAuthorityList(authDB.getRole()));
                        String refreshToken = JwtConfig.createRefreshToken(email);

                        Map<String, Object> responseData = DataHelper.buildUserAuthData(userDB, sessionToken,
                                refreshToken);
                        return ResponseHelper.ok("se ha iniciado sesión exitosamente", responseData);
                    }
                    case "Desactivado" -> {
                        UserModel activateUser = userHelper.checkUserAccount(userRepository, referredRepository,
                                userDB);
                        if (activateUser != null) {
                            emailAppProvider.userAccountActivated(email, "Desconocido", "Desconocido");
                            return ResponseHelper.accepted("el usuario se ha activado nuevamente",
                                    DataHelper.buildUser(activateUser));
                        } else {
                            return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                        }
                    }
                    default -> {
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    }
                }
            }
        }

        return ResponseHelper.locked("credenciales incorrectas", null);
    }

    @Transactional
    public ResponseEntity<GeneralResponse> logout(String email) {
        Optional<AuthModel> authOptional = authRepository.findByEmail(email);
        if (authOptional.isPresent()) {
            AuthModel auth = authOptional.get();
            // Invalida todos los tokens anteriores actualizando tokenRevocationDate a now()
            auth.setTokenRevocationDate(LocalDateTime.now());
            authRepository.save(auth);
            return ResponseHelper.ok("Sesión cerrada exitosamente en todos los dispositivos",
                    (Map<String, Object>) null);
        }
        return ResponseHelper.failedDependency("No fue posible cerrar la sesión", (String) null);
    }

    @Transactional
    public ResponseEntity<GeneralResponse> restorePassword(String email) {
        String userEmail = email.toLowerCase();
        Optional<AuthModel> authOptional = authRepository.findByEmail(userEmail);
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);

        if (authOptional.isPresent() && userOptional.isPresent()) {
            AuthModel authDB = authOptional.get();
            UserModel userDB = userOptional.get();
            UserDataModel userData = userDB.getPersonalData();

            if (authDB.isAccountConfirmed()) {
                String statusUserDB = userData.getStatus();
                String codeAuth = DataHelper.generateRandomCode();

                switch (statusUserDB) {
                    case "Activado" -> {
                        emailAppProvider.sendAuthCodeToRestorePassword(new String[] { userEmail }, codeAuth);
                    }
                    case "Desactivado" -> {
                        if (userHelper.makeUserObsolete(userRepository, referredRepository, userDB)) {
                            return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                        } else {
                            emailAppProvider.sendAuthCodeToRestorePassword(new String[] { userEmail }, codeAuth);
                        }
                    }
                    default -> {
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    }
                }

                authDB.setCodeAuth(pwdEncoder.encode(codeAuth));
                authRepository.save(authDB);
                return ResponseHelper.ok(
                        "se ha enviado un código de confirmación para restablecer la contraseña al email: " + userEmail,
                        Collections.singletonMap("info", (Object) "ok"));
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    public ResponseEntity<GeneralResponse> confirmPasswordReset(PasswordResetRequest passwordReset)
            throws JsonProcessingException {
        String userEmail = passwordReset.email().toLowerCase();
        Optional<AuthModel> authOptional = authRepository.findByEmail(userEmail);
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);

        if (authOptional.isPresent() && userOptional.isPresent()) {
            AuthModel authDB = authOptional.get();
            UserModel userDB = userOptional.get();
            UserDataModel userData = userDB.getPersonalData();

            if (pwdEncoder.matches(passwordReset.code(), authDB.getCodeAuth())
                    && isCodeActive(authDB.getCodeExpirationTime(), LocalDateTime.now(), 5)) {
                String newPwd = passwordReset.newPwd();
                String repeatedPwd = passwordReset.repeatedPwd();

                if (this.validateInputHelper.verifyPwd(newPwd).equals("") && !DataHelper.isNull(repeatedPwd) &&
                        newPwd.equals(repeatedPwd) && authDB.isAccountConfirmed()) {

                    String statusUserDB = userData.getStatus();
                    switch (statusUserDB) {
                        case "Activado" -> {
                            // Valid
                        }
                        case "Desactivado" -> {
                            userDB = userHelper.checkUserAccount(userRepository, referredRepository, userDB);
                            if (userDB != null) {
                                emailAppProvider.userAccountActivated(userEmail, "Desconocido", "Desconocido");
                            } else {
                                return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                            }
                        }
                        default -> {
                            return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                        }
                    }

                    authDB.setPwd(pwdEncoder.encode(newPwd));
                    authDB.setTokenRevocationDate(LocalDateTime.now());
                    authRepository.save(authDB);

                    String sessionToken = JwtConfig.createSessionToken(userEmail,
                            org.springframework.security.core.authority.AuthorityUtils
                                    .commaSeparatedStringToAuthorityList(authDB.getRole()));
                    String refreshToken = JwtConfig.createRefreshToken(userEmail);

                    return ResponseHelper.ok(
                            "se ha restablecido exitosamente la contraseña del usuario. Has iniciado sesión automáticamente.",
                            DataHelper.buildUserAuthData(userDB, sessionToken, refreshToken));
                }
            } else {
                return ResponseHelper.gone("el código ha expirado o no es correcto", null);
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    public ResponseEntity<GeneralResponse> resendUserCode(String email, String type) {
        String userEmail = email.toLowerCase();
        Optional<AuthModel> authOptional = authRepository.findByEmail(userEmail);
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);

        if (authOptional.isPresent() && userOptional.isPresent()) {
            AuthModel authDB = authOptional.get();
            UserModel userDB = userOptional.get();
            String userStatusDB = userDB.getPersonalData().getStatus();
            String code = DataHelper.generateRandomCode();
            boolean isValid = false;

            if (!DataHelper.isNull(type)) {
                switch (type) {
                    case "registerUser": {
                        if (!authDB.isAccountConfirmed() && userStatusDB.equals("Desactivado")) {
                            emailAppProvider.sendAuthCodeToRegisterUser(new String[] { userEmail }, code);
                            isValid = true;
                        }
                        break;
                    }
                    case "restorePassword": {
                        if (authDB.isAccountConfirmed()) {
                            emailAppProvider.sendAuthCodeToRestorePassword(new String[] { userEmail }, code);
                            isValid = true;
                        }
                        break;
                    }
                }

                if (isValid) {
                    authDB.setCodeExpirationTime(LocalDateTime.now());
                    authRepository.save(authDB);
                    return ResponseHelper.ok("el código de confirmación se ha vuelto ha enviar al email: " + userEmail,
                            Collections.singletonMap("info", (Object) "ok"));
                }
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    public ResponseEntity<GeneralResponse> disableAccount(String emailAuth) {
        UserModel userB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        if (userB.getWallet().getTotalBalance() > 0) {
            return ResponseHelper.locked(
                    "no es posible deshabilitar el usuario, porque tiene transacciones pendientes o aún hay circulación de dinero en el balance total",
                    null);
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        userB.getPersonalData().setStatus("Desactivado");
        userB.setDisableAccount(currentDateTime);
        userRepository.save(userB);

        Optional<AuthModel> authOptional = authRepository.findByEmail(emailAuth);
        if (authOptional.isPresent()) {
            AuthModel auth = authOptional.get();
            auth.setTokenRevocationDate(currentDateTime);
            authRepository.save(auth);
        }

        List<ReferredModel> updateTheReferreds = new ArrayList<>();
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(emailAuth);
        if (referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferredStatus("Desactivado");
            referredByUserA.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(referredByUserA);
        }
        List<ReferredModel> usersC = referredRepository.findAllByUserReferring(emailAuth);
        for (ReferredModel userC : usersC) {
            userC.setUserReferringStatus("Desactivado");
            userC.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(userC);
        }
        if (updateTheReferreds.size() > 0) {
            referredRepository.saveAll(updateTheReferreds);
        }
        emailAppProvider.userAccountDisabled(emailAuth, currentDateTime);
        return ResponseHelper.ok(
                "la cuenta del usuario se ha deshabilitado por un rango de 30 días, luego del tiempo estipulado, si no hay actividad la cuenta se eliminará definitivamente",
                Collections.singletonMap("info", (Object) "ok"));
    }

    // UTILIDADES
    private String[] validateCodeToRefer(String codeToRefer) {
        if (DataHelper.isNull(codeToRefer)) {
            return new String[] { "Sin usuario", "Sin usuario" };
        }
        Optional<UserModel> userOptional = userRepository.findByCodeToRefer(codeToRefer);
        if (userOptional.isPresent()) {
            UserDataModel userData = userOptional.get().getPersonalData();
            if (userData.getStatus().equals("Activado")) {
                String userReferring = userData.getEmail();
                return new String[] { userReferring, codeToRefer };
            }
        }
        return null;
    }

    private UserDataModel createUserData(String name, String surname, String email) {
        return new UserDataModel(name, surname, email, "", "", DataHelper.deprecatedDate(), "Desactivado", new byte[0]);
    }

    private boolean isCodeActive(LocalDateTime codeExpirationTime, LocalDateTime verificationDateTime,
            int expirationMinutes) {
        if (codeExpirationTime == null)
            return false;
        long minutesDifference = ChronoUnit.MINUTES.between(codeExpirationTime, verificationDateTime);
        if (minutesDifference < expirationMinutes) {
            return true;
        } else if (minutesDifference == expirationMinutes) {
            long secondsDifference = ChronoUnit.SECONDS.between(codeExpirationTime, verificationDateTime);
            return secondsDifference <= 0;
        }
        return false;
    }
}
