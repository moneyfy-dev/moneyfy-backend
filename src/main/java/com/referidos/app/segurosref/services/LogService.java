package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

public interface LogService {

    ResponseEntity<?> findAllLogs(HttpServletRequest request);

}
