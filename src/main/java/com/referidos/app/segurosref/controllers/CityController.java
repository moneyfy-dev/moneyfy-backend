package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.requests.CityRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.CityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/cities")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "City Controller",
    description = "Controller to recover the cities for the address"
)
public class CityController {

    @Autowired
    private CityService cityService;

    // Endpoints para recuperar y registrar ciudades de la aplicación
    @GetMapping(value = "/find/all")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Search all the cities register in the application",
        description = "Search all the cities register in the application",
        tags = {"City Controller"},
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The cities were encountered successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> findAll(Authentication auth) {
        return cityService.findAll(auth.getPrincipal().toString());
    }
    
    @PostMapping(value = "/register")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Register or update the cities of the application",
        description = "Register or update the cities of the application",
        tags = {"City Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the required data to continue",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = CityRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The cities were registered successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> registerCities(@RequestBody CityRequest cityRequest) {
        return cityService.registerCities(cityRequest);
    }

}
