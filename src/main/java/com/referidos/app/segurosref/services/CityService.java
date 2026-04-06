package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

public interface CityService {

    ResponseEntity<?> findAll(String emailAuth);

}
