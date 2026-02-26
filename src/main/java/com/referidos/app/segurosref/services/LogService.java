package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.requests.LogNotifyRequest;
import com.referidos.app.segurosref.requests.LogRequest;

public interface LogService {

    // Servicio para la búsqueda de todos los logs de la aplicación, además de los logs de errores
    ResponseEntity<?> findAllLogs(LogRequest logRequest);

    // Servicios para notificar a usuarios que actualicen data necesaria o actualización de logs de error
    ResponseEntity<?> notifyAccountNotFound(LogNotifyRequest logRequest);
    ResponseEntity<?> updateErrorLogs(LogRequest logRequest);

}
