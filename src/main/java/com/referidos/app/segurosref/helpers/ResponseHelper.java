package com.referidos.app.segurosref.helpers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.dtos.ResultQuoteDto;
import com.referidos.app.segurosref.responses.GeneralResponses;

import jakarta.servlet.http.HttpServletResponse;

public class ResponseHelper {

    public static Map<String, Object> buildErrorFields(BindingResult result) {
        Map<String, Object> json = new HashMap<>();

        result.getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            json.put(fieldName, "The field " + fieldName + " " + error.getDefaultMessage());
        });
        
        return json;
    }

    public static ResponseEntity<GeneralResponses> response(String message, int status, Object data) {
        return ResponseEntity.status(status).body(new GeneralResponses(message, status, data));
    }

    public static ResponseEntity<GeneralResponses> ok(String message, Map<String, Object> info) {
        String buildMessage = "Solicitud realizada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.OK.value(), info);
    }

    public static ResponseEntity<GeneralResponses> ok(String message, ResultQuoteDto resultQuoteDto) {
        String buildMessage = "Solicitud realizada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.OK.value(), resultQuoteDto);
    }

    public static ResponseEntity<GeneralResponses> created(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "created");
        String buildMessage = "Recurso creado: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.CREATED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponses> accepted(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "accepted");
        String buildMessage = "Solicitud aceptada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.ACCEPTED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponses> imUsed(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "I'm used");
        String buildMessage = "Recurso en uso: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.IM_USED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponses> gone(String message, String info) {
        String buildInfo = (info != null) ? info : "gone";
        String buildMessage = "Solicitud expirada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.GONE.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponses> locked(String message, String info) {
        String buildInfo = (info != null) ? info : "locked";
        String buildMessage = "Solicitud retenida: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.LOCKED.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponses> failedDependency(String message, String info) {
        String buildInfo = (info != null) ? info : "failed dependency";
        String buildMessage = "Solicitud irreconocible: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.FAILED_DEPENDENCY.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponses> preconditionMap(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "precondition required");
        String buildMessage = "Precondición requerida: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.PRECONDITION_REQUIRED.value(), buildInfo);
    }

    public static void failedDependency(HttpServletResponse response, String message, String info) throws JsonProcessingException, IOException {
        String buildInfo = (info != null) ? info : "failed dependency";
        String buildMessage = "Solicitud irreconocible: " + message;
        GeneralResponses body = new GeneralResponses(buildMessage,
                HttpStatus.FAILED_DEPENDENCY.value(),
                Map.of("info", buildInfo));
        response.setStatus(HttpStatus.FAILED_DEPENDENCY.value());
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }

    public static void invalidJWT(HttpServletResponse response, String message, String info) throws JsonProcessingException, IOException {
        String buildInfo = (info != null) ? info : "expectation failed";
        String buildMessage = "JWT inválido: " + message;
        GeneralResponses body = new GeneralResponses(buildMessage,
                HttpStatus.EXPECTATION_FAILED.value(),
                Map.of("info", buildInfo));
        response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }

}
