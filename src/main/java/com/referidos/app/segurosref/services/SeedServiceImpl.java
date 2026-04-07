package com.referidos.app.segurosref.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.CityRepository;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

public class SeedServiceImpl implements SeedService {

    @Value(value = "${api.key.moneyfy.seed}")
    private String apiKeyMF;
    
    @Autowired
    private SeedHelper seedHelper;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<?> checkCities(HttpServletRequest request) {
        if(!this.checkApiKeyMF(request.getParameter("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        seedHelper.updateCities(cityRepository);
        return ResponseHelper.ok("las ciudades se han actualizado", Map.of("info", "ok"));
    }

    @Override
    public ResponseEntity<?> checkUsers(HttpServletRequest request) {
        if(!this.checkApiKeyMF(request.getParameter("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Registrar usuarios
        String message = seedHelper.updateTestUsers(userRepository, referredRepository, deviceRepository, passwordEncoder);
        if(message == null) {
            return ResponseHelper.failedDependency("los usuarios de pruebas son incorrectos", null);
        }
        return ResponseHelper.ok(message, Map.of("info", "ok"));
    }

    private boolean checkApiKeyMF(String apiKeyParameter) {
        return apiKeyMF.equals(apiKeyParameter);
    }

}
