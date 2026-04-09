package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.requests.SeedRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface SeedService {

    // Servicios para registrar información esencial a la API
    ResponseEntity<?> checkCities(HttpServletRequest request, SeedRequest seedRequest);
    ResponseEntity<?> checkUsers(HttpServletRequest request, SeedRequest seedRequest);
    ResponseEntity<?> checkInsurers(HttpServletRequest request, SeedRequest seedRequest);
    ResponseEntity<?> checkBrands(HttpServletRequest request, SeedRequest seedRequest);
    
}
