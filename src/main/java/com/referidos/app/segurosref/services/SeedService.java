package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.requests.CityRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface SeedService {

    // Servicios para registrar información esencial a la API
    ResponseEntity<?> checkCities(CityRequest cityRequest, HttpServletRequest request);
    
}
