package com.referidos.app.segurosref.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.requests.CityRequest;

import jakarta.servlet.http.HttpServletRequest;

public class SeedServiceImpl implements SeedService {

    @Value(value = "${api.key.moneyfy.seed}")
    private String apiKeyMF;

    @Override
    public ResponseEntity<?> checkCities(CityRequest cityRequest, HttpServletRequest request) {
        String requestApiKey = request.getParameter("Api-Key-MoneyFy");
        // TODO Auto-generated method stub
        return null;
    }

}
