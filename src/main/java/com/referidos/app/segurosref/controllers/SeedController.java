package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.requests.SeedRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.SeedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/seed")
@PreAuthorize(value = "denyAll()")
@Tag(name = "Seed Controller", description = "Controller to seed essential data")
public class SeedController {

        private final SeedService seedService;

        @PostMapping(value = "/regions")
        @PreAuthorize(value = "permitAll()")
        @Operation(summary = "Register or update the regions of the application", description = "Register or update the regions of the application", tags = {
                        "Seed Controller" }, parameters = {
                                        @Parameter(name = "X-Moneyfy-Api-Key", in = ParameterIn.HEADER, description = "Security parameter for some public endpoints", required = true)
                        }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh All Data Again", required = true, content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = SeedRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "The regions were registered successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        })
        public ResponseEntity<?> checkRegions(HttpServletRequest request, @RequestBody SeedRequest seedRequest) {
                return seedService.checkRegions(request, seedRequest);
        }

        @PostMapping(value = "/insurers")
        @PreAuthorize(value = "permitAll()")
        @Operation(summary = "Register or update the insurers of the application", description = "Register or update the insurers of the application", tags = {
                        "Seed Controller" }, parameters = {
                                        @Parameter(name = "X-Moneyfy-Api-Key", in = ParameterIn.HEADER, description = "Security parameter for some public endpoints", required = true)
                        }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh All Data Again", required = true, content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = SeedRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "The test and default users were updated successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        })
        public ResponseEntity<?> checkInsurers(HttpServletRequest request, @RequestBody SeedRequest seedRequest) {
                return seedService.checkInsurers(request, seedRequest);
        }

        @PostMapping(value = "/brands")
        @PreAuthorize(value = "permitAll()")
        @Operation(summary = "Register the brands to quote your vehicle", description = "Register the brands to quote your vehicle", tags = {
                        "Seed Controller" }, parameters = {
                                        @Parameter(name = "X-Moneyfy-Api-Key", in = ParameterIn.HEADER, description = "Security parameter for some public endpoints", required = true)
                        }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh All Data Again", required = true, content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = SeedRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "The vehicle brands were created successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        })
        public ResponseEntity<?> checkBrands(HttpServletRequest request, @RequestBody SeedRequest seedRequest) {
                return seedService.checkBrands(request, seedRequest);
        }

}
