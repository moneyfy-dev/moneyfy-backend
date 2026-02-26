package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.requests.CityRequest;

public interface CityService {

    // Servicios para recuperar y registrar ciudades de la aplicación
    ResponseEntity<?> findAll(String emailAuth);
    ResponseEntity<?> registerCities(CityRequest cityRequest);

}
