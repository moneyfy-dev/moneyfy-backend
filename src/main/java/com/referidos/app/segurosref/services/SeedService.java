package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

public interface SeedService {

    // Servicios para registrar información esencial a la API
    ResponseEntity<?> checkCities(HttpServletRequest request);
    ResponseEntity<?> checkUsers(HttpServletRequest request);
    
}
