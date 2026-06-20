package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.requests.SeedRequest;


public interface SeedService {

    // Servicios para registrar información esencial a la API
    ResponseEntity<?> checkRegions(SeedRequest seedRequest);

    ResponseEntity<?> checkInsurers(SeedRequest seedRequest);

    ResponseEntity<?> checkBrands(SeedRequest seedRequest);

}
