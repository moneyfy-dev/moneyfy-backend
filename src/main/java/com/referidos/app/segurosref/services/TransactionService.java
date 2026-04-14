package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

public interface TransactionService {

    // Buscar transacción específica para saber si es necesario actualizarla
    ResponseEntity<?> findById(String transactionId, HttpServletRequest request);
    ResponseEntity<?> findAllByUserReferringFound(HttpServletRequest request);

}
