package com.referidos.app.segurosref.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.SeedHelper;
import com.referidos.app.segurosref.repositories.CityRepository;

import jakarta.servlet.http.HttpServletRequest;

public class SeedServiceImpl implements SeedService {

    @Value(value = "${api.key.moneyfy.seed}")
    private String apiKeyMF;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private SeedHelper seedHelper;

    @Override
    public ResponseEntity<?> checkCities(HttpServletRequest request) {
        String requestApiKey = request.getParameter("Api-Key-MoneyFy");
        if(!requestApiKey.equals(requestApiKey)) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        seedHelper.updateCities(cityRepository);
        return ResponseHelper.ok("las ciudades se han actualizado", Map.of("info", "ok"));
    }

}
