package com.referidos.app.segurosref.helpers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.dtos.quotation.QuotationDto;
import com.referidos.app.segurosref.responses.GeneralResponse;

import jakarta.servlet.http.HttpServletResponse;

public class ResponseHelper {

    public static ResponseEntity<GeneralResponse> response(String message, int status, Object data) {
        return ResponseEntity.status(status).body(new GeneralResponse(message, status, data));
    }

    public static ResponseEntity<GeneralResponse> ok(String message, Map<String, Object> info) {
        String buildMessage = "Solicitud realizada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.OK.value(), info);
    }

    public static ResponseEntity<GeneralResponse> ok(String message, QuotationDto quotationDto) {
        String buildMessage = "Solicitud realizada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.OK.value(), quotationDto);
    }

    public static ResponseEntity<GeneralResponse> created(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "created");
        String buildMessage = "Recurso creado: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.CREATED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponse> accepted(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "accepted");
        String buildMessage = "Solicitud aceptada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.ACCEPTED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponse> imUsed(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "I'm used");
        String buildMessage = "Recurso en uso: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.IM_USED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponse> gone(String message, String info) {
        String buildInfo = (info != null) ? info : "gone";
        String buildMessage = "Solicitud expirada: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.GONE.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponse> locked(String message, String info) {
        String buildInfo = (info != null) ? info : "locked";
        String buildMessage = "Solicitud retenida: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.LOCKED.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponse> failedDependency(String message, String info) {
        String buildInfo = (info != null) ? info : "failed dependency";
        String buildMessage = "Solicitud irreconocible: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.FAILED_DEPENDENCY.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponse> failedDependency(String message, Map<String, Object> data) {
        String buildMessage = "Solicitud irreconocible: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.FAILED_DEPENDENCY.value(), data);
    }

    public static ResponseEntity<GeneralResponse> preconditionMap(String message, Map<String, Object> info) {
        Map<String, Object> buildInfo = (info != null) ? info : Map.of("info", "precondition required");
        String buildMessage = "Precondición requerida: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.PRECONDITION_REQUIRED.value(), buildInfo);
    }

    public static ResponseEntity<GeneralResponse> badRequest(String message, String info) {
        String buildInfo = (info != null) ? info : "bad request";
        String buildMessage = "Solicitud incorrecta: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.BAD_REQUEST.value(), Map.of("info", buildInfo));
    }

    public static ResponseEntity<GeneralResponse> notFound(String message) {
        String buildMessage = "Recurso no encontrado: " + message;
        return ResponseHelper.response(buildMessage, HttpStatus.NOT_FOUND.value(), Map.of("info", "not found"));
    }

    public static void failedDependency(HttpServletResponse response, String message, String info) throws JsonProcessingException, IOException {
        String buildInfo = (info != null) ? info : "failed dependency";
        String buildMessage = "Solicitud irreconocible: " + message;
        int status = HttpStatus.FAILED_DEPENDENCY.value();
        GeneralResponse body = new GeneralResponse(buildMessage,
                status,
                Map.of("info", buildInfo));
        response.setStatus(status);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }

    public static void invalidJWT(HttpServletResponse response, String message, String info) throws JsonProcessingException, IOException {
        String buildInfo = (info != null) ? info : "expectation failed";
        String buildMessage = "JWT inválido: " + message;
        int status = HttpStatus.EXPECTATION_FAILED.value();
        GeneralResponse body = new GeneralResponse(buildMessage,
                status,
                Map.of("info", buildInfo));
        response.setStatus(status);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }

}
