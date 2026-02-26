package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.PlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/plans")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Plan Controller",
    description = "Controller to handle the different plans that exist in the data base"
)
public class PlanController {

    @Autowired
    private PlanService planService;

    // Endpoint de búsqueda de plan
    @GetMapping(value = "/{planId}")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Search an specific plan to get the details",
        description = "Search an specific plan to get the details",
        tags = {"Plan Controller"},
        parameters = {
            @Parameter(
                name = "planId",
                in = ParameterIn.PATH,
                description = "Enter the plan id to search it",
                required = true
            ),
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
                description = "The plan was encountered successfully",
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
    public ResponseEntity<?> findPlanById(@PathVariable String planId, Authentication auth) {
        return planService.findPlanById(auth.getPrincipal().toString(), planId);
    }

}
