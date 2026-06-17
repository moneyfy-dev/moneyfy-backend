package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

public interface RegionService {

    ResponseEntity<?> findAll(String emailAuth);

}
