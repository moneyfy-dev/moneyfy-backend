package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

public interface PlanService {

    // Servicio de búsqueda de plan
    ResponseEntity<?> findPlanById(String emailAuth, String planId);

}
