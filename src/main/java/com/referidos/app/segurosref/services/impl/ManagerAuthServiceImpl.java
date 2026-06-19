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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerAuthServiceImpl implements ManagerAuthService {

    private final ManagerRepository managerRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder pwdEncoder;

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
                            "token", sessionToken,
                            "refreshToken", refreshToken
                    );

                    return ResponseHelper.ok("se ha iniciado sesión exitosamente", responseData);
                } else {
                    return ResponseHelper.failedDependency("cuenta de administrador desactivada", "failed dependency");
                }
            }
        }

        return ResponseHelper.locked("credenciales incorrectas", null);
    }
}
