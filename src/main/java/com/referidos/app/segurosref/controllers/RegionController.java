package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.RegionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/regions")
@PreAuthorize(value = "denyAll()")
@Tag(name = "Region Controller", description = "Controller to recover the regions for the address")
public class RegionController {

        private final RegionService regionService;

        // Endpoints para recuperar y registrar regiones de la aplicación
        @GetMapping(value = "/find/all")
        @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
        @Operation(summary = "Search all the regions register in the application", description = "Search all the regions register in the application", tags = {
                        "Region Controller" }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "The regions were encountered successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        })
        public ResponseEntity<?> findAll(Authentication auth) {
                return regionService.findAll(auth.getPrincipal().toString());
        }

}
