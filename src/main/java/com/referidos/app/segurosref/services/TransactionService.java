package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;


public interface TransactionService {

    // Buscar transacción específica para saber si es necesario actualizarla
    ResponseEntity<?> findById(String transactionId);
    ResponseEntity<?> findAllByUserReferringFound();

}
