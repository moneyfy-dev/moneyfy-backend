package com.referidos.app.segurosref.controllers;

import java.time.DateTimeException;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.responses.GeneralResponse;

// CONTROLADOR IMPLEMENTADO PARA EL MANEJO DE EXCEPCIONES
@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<GeneralResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseHelper.failedDependency("no se ha podido identificar el recurso", ex.getMessage());
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    public ResponseEntity<GeneralResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        return ResponseHelper.response(
                "JWT inválido: credenciales incorrectas",
                HttpStatus.EXPECTATION_FAILED.value(),
                Map.of("info", ex.getMessage()));
    }

    @ExceptionHandler(value = NoSuchElementException.class)
    public ResponseEntity<GeneralResponse> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseHelper.failedDependency("no se ha podido encontrar el elemento", ex.getMessage());
    }

    @ExceptionHandler(value = JsonProcessingException.class)
    public ResponseEntity<GeneralResponse> handleJsonProcessingException(JsonProcessingException ex) {
        return ResponseHelper.failedDependency("el objeto no pudo ser procesado", ex.getMessage());
    }

    @ExceptionHandler(value = MultipartException.class)
    public ResponseEntity<GeneralResponse> handleMultipartException(MultipartException ex) {
        return ResponseHelper.failedDependency("el archivo no cumple con los requerimientos solicitados",
                ex.getMessage());
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<GeneralResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseHelper.failedDependency("la solicitud no pudo ser procesada", ex.getMessage());
    }

    @ExceptionHandler(value = NotReadablePropertyException.class)
    public ResponseEntity<GeneralResponse> handleNotReadablePropertyException(NotReadablePropertyException ex) {
        return ResponseHelper.failedDependency("el objeto vinculante no pudo ser enlazado a los campos proveídos",
                ex.getMessage());
    }

    @ExceptionHandler(value = NullPointerException.class)
    public ResponseEntity<GeneralResponse> handleNullPointerException(NullPointerException ex) {
        return ResponseHelper.failedDependency("el objeto no posee valor", ex.getMessage());
    }

    @ExceptionHandler(value = DateTimeException.class)
    public ResponseEntity<GeneralResponse> handleDateTimeException(DateTimeException ex) {
        return ResponseHelper.failedDependency("no se ha podido entregar formato a la fecha", ex.getMessage());
    }

}
