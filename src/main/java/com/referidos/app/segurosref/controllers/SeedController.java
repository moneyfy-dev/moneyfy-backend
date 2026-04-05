package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.requests.CityRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
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
@RequestMapping(value = "/seed")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Seedder Controller",
    description = "Controller to seed essential data"
)
public class SeedController {

    @Autowired
    private SeedService seedService;

    @PostMapping(value = "/cities")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Register or update the cities of the application",
        description = "Register or update the cities of the application",
        tags = {"Seedder Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the required data to continue",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = CityRequest.class)
            )
        ),
        parameters = {
            @Parameter(
                name = "Api-Key-MoneyFy",
                in = ParameterIn.HEADER,
                description = "Security parameter for some public endpoints",
                required = true
            )
        },
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
    public ResponseEntity<?> checkCities(@RequestBody CityRequest cityRequest, HttpServletRequest request) {
        return seedService.checkCities(cityRequest, request);
    }

}
