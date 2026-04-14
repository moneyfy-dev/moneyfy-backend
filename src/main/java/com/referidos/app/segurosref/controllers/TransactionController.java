package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/transaction")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Transactions",
    description = "Controller to handle the transactions with problems"
)
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping(value = "/{transactionId}")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Find transaction",
        description = "Find transaction by id",
        tags = {"Transactions"},
        parameters = {
            @Parameter(
                name = "transactionId",
                in = ParameterIn.PATH,
                description = "Enter the transaction id for the searching",
                required = true
            ),
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
                description = "The trasaction was found successfully",
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
    public ResponseEntity<?> findById(@PathVariable String transactionId, HttpServletRequest request) {
        return transactionService.findById(transactionId, request);
    }

    @GetMapping(value = "/check/referring")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Find all transaction that have issues referring",
        description = "Find all transaction that have issues referring",
        tags = {"Transactions"},
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
                description = "The trasactions were found successfully",
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
    public ResponseEntity<?> findAllByUserReferringFound(HttpServletRequest request) {
        return transactionService.findAllByUserReferringFound(request);
    }

}
