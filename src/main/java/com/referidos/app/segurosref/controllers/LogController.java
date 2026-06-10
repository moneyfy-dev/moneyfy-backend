package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.LogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/log")
@PreAuthorize(value = "denyAll()")
@Tag(name = "Log Controller", description = "Controller to handle errors, information data through logs")
public class LogController {

    @Autowired
    private LogService logService;

    // Endpoint para la búsqueda de todos los logs de la aplicación
    @GetMapping(value = "/find/all")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Search all the logs to verify the application status", description = "Search all the logs to verify the application status", tags = {
            "Log Controller" }, parameters = {
                    @Parameter(name = "Api-Key-MoneyFy", in = ParameterIn.HEADER, description = "Security parameter for some public endpoints", required = true)
            }, responses = {
                    @ApiResponse(responseCode = "200", description = "The logs were recovered successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                    @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
            })
    public ResponseEntity<?> findAllLogs(HttpServletRequest request) {
        return logService.findAllLogs(request);
    }

}
