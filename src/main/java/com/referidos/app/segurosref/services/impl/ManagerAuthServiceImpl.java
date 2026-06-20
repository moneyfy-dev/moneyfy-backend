package com.referidos.app.segurosref.services.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.models.ManagerModel;
import com.referidos.app.segurosref.dtos.manager.ManagerDto;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.repositories.ManagerRepository;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.ManagerAuthService;
import com.referidos.app.segurosref.integrations.email.providers.EmailAppProvider;
import com.referidos.app.segurosref.requests.ManagerRegisterRequest;
import com.referidos.app.segurosref.requests.EmailRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import java.time.LocalDateTime;
import com.referidos.app.segurosref.helpers.DataHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerAuthServiceImpl implements ManagerAuthService {

    private final ManagerRepository managerRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder pwdEncoder;
    private final EmailAppProvider emailAppProvider;

    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> login(UserLoginRequest request) throws JsonProcessingException {
        String email = request.email().toLowerCase();
        String pwd = request.pwd();

        Optional<AuthModel> authOptional = authRepository.findByEmail(email);
        if (authOptional.isPresent()) {
            AuthModel authDB = authOptional.get();
            if (pwdEncoder.matches(pwd, authDB.getPwd()) && "ROLE_ADMIN".equals(authDB.getRole())) {
                ManagerModel managerDB = managerRepository.findByEmail(email).orElseThrow();

                if ("Activado".equals(managerDB.getStatus())) {
                    String sessionToken = JwtConfig.createSessionToken(email,
                            Collections.singletonList(new SimpleGrantedAuthority(authDB.getRole())));
                    String refreshToken = JwtConfig.createRefreshToken(email);

                    // Devolvemos la data relevante del administrador
                    ManagerDto managerDto = ManagerDto.builder()
                            .managerId(managerDB.getManagerId())
                            .name(managerDB.getName())
                            .surname(managerDB.getSurname())
                            .email(managerDB.getEmail())
                            .status(managerDB.getStatus())
                            .build();

                    Map<String, Object> responseData = Map.of(
                            "manager", managerDto,
                            "sessionToken", sessionToken,
                            "refreshToken", refreshToken);

                    return ResponseHelper.ok("se ha iniciado sesión exitosamente", responseData);
                } else {
                    return ResponseHelper.failedDependency("cuenta de administrador desactivada", "failed dependency");
                }
            }
        }

        return ResponseHelper.locked("credenciales incorrectas", null);
    }

    @Transactional
    @Override
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

    @SuppressWarnings("null")
    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> createManager(ManagerRegisterRequest request) {
        String email = request.email().toLowerCase();
        Optional<AuthModel> authOpt = authRepository.findByEmail(email);
        Optional<ManagerModel> managerOpt = managerRepository.findByEmail(email);

        if (authOpt.isPresent() && managerOpt.isPresent()) {
            ManagerModel manager = managerOpt.get();
            if ("Activado".equals(manager.getStatus())) {
                return ResponseHelper.ok("El usuario ya existe con comando", (Map<String, Object>) null);
            } else if ("Desactivado".equals(manager.getStatus())) {
                return ResponseHelper.ok("El usuario ya existe pero tiene que activar la cuenta",
                        (Map<String, Object>) null);
            }
        }

        if (authOpt.isEmpty() && managerOpt.isEmpty()) {
            // Si no existe, lo creamos
            AuthModel newAuth = AuthModel.builder()
                    .email(email)
                    .pwd("") // contraseña vacía por ahora
                    .role("ROLE_ADMIN")
                    .accountConfirmed(false)
                    .build();
            authRepository.save(newAuth);

            ManagerModel newManager = ManagerModel.builder()
                    .name(request.name())
                    .surname(request.surname())
                    .email(email)
                    .status("Desactivado")
                    .build();
            managerRepository.save(newManager);

            return ResponseHelper.ok("Usuario administrador creado", (Map<String, Object>) null);
        }

        return ResponseHelper.badRequest(
                "Error de integridad: el correo ya se encuentra registrado con otro rol o estado", (String) null);
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> restorePassword(EmailRequest request) {
        String email = request.email().toLowerCase();
        Optional<ManagerModel> managerOpt = managerRepository.findByEmail(email);

        if (managerOpt.isPresent()) {
            Optional<AuthModel> authOpt = authRepository.findByEmail(email);
            if (authOpt.isPresent()) {
                AuthModel auth = authOpt.get();
                String codeAuth = DataHelper.generateRandomCode();
                auth.setCodeAuth(codeAuth);
                auth.setCodeExpirationTime(LocalDateTime.now().plusMinutes(15));
                authRepository.save(auth);

                emailAppProvider.sendAuthCodeToRestorePassword(new String[] { email }, codeAuth);

                return ResponseHelper.ok(
                        "se envío el código de reestablecimiento de contraseña para el usuario administrador",
                        (Map<String, Object>) null);
            }
        }

        return ResponseHelper.notFound("no se encontro ningún usuario administrador con las credenciales mencionadas");
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> confirmPasswordReset(PasswordResetRequest request)
            throws JsonProcessingException {
        String email = request.email().toLowerCase();
        String code = request.code();
        String pwd = request.newPwd();
        String confirmPwd = request.repeatedPwd();

        if (pwd == null || confirmPwd == null || pwd.trim().isEmpty()) {
            return ResponseHelper.badRequest("Error de formato: las contraseñas no pueden estar vacías", (String) null);
        }

        if (!pwd.equals(confirmPwd)) {
            return ResponseHelper.badRequest("Error de formato: las contraseñas no coinciden", (String) null);
        }

        Optional<ManagerModel> managerOpt = managerRepository.findByEmail(email);
        Optional<AuthModel> authOpt = authRepository.findByEmail(email);

        if (managerOpt.isPresent() && authOpt.isPresent()) {
            AuthModel auth = authOpt.get();
            ManagerModel manager = managerOpt.get();

            if (auth.getCodeAuth() == null || !auth.getCodeAuth().equals(code)) {
                return ResponseHelper.badRequest("Error de código: el código de confirmación es inválido",
                        (String) null);
            }
            if (auth.getCodeExpirationTime() != null && auth.getCodeExpirationTime().isBefore(LocalDateTime.now())) {
                return ResponseHelper.badRequest("Error de código: el código ha expirado", (String) null);
            }

            // Cambiar la contraseña y confirmar cuenta
            auth.setPwd(pwdEncoder.encode(pwd));
            auth.setAccountConfirmed(true);
            auth.setCodeAuth(null);
            auth.setCodeExpirationTime(null);
            authRepository.save(auth);

            String message;
            if ("Desactivado".equals(manager.getStatus())) {
                manager.setStatus("Activado");
                managerRepository.save(manager);
                message = "el usuario se activo y se le creo la contraseña";
            } else {
                message = "se reestablecio su contraseña";
            }

            String sessionToken = JwtConfig.createSessionToken(email,
                    Collections.singletonList(new SimpleGrantedAuthority(auth.getRole())));
            String refreshToken = JwtConfig.createRefreshToken(email);

            ManagerDto managerDto = ManagerDto.builder()
                    .managerId(manager.getManagerId())
                    .name(manager.getName())
                    .surname(manager.getSurname())
                    .email(manager.getEmail())
                    .status(manager.getStatus())
                    .build();

            Map<String, Object> responseData = Map.of(
                    "manager", managerDto,
                    "sessionToken", sessionToken,
                    "refreshToken", refreshToken);

            return ResponseHelper.ok(message, responseData);
        }

        return ResponseHelper.notFound("no se encontro usuario administrador con las credenciales mencionadas");
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> resendCode(EmailRequest request) {
        String email = request.email().toLowerCase();
        Optional<ManagerModel> managerOpt = managerRepository.findByEmail(email);
        Optional<AuthModel> authOpt = authRepository.findByEmail(email);

        if (managerOpt.isPresent() && authOpt.isPresent()) {
            AuthModel auth = authOpt.get();
            String codeAuth = DataHelper.generateRandomCode();
            auth.setCodeAuth(codeAuth);
            auth.setCodeExpirationTime(LocalDateTime.now().plusMinutes(15));
            authRepository.save(auth);

            emailAppProvider.sendAuthCodeToRestorePassword(new String[] { email }, codeAuth);

            return ResponseHelper.ok("se reenvio el código para reestrablecer la contraseña del usuario",
                    (Map<String, Object>) null);
        }

        return ResponseHelper
                .notFound("no se encontro usuario administrador para reenviar código para restablecer contraseña");
    }

}
