package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.requests.LogNotifyRequest;
import com.referidos.app.segurosref.requests.LogRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.LogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/logs")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Log Controller",
    description = "Controller to handle errors, information data through logs"
)
public class LogController {

    @Autowired
    private LogService logService;

    // Endpoint para la búsqueda de todos los logs de la aplicación, además de los logs de errores
    @PostMapping(value = "/find/all")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Search all the logs to verify the application status",
        description = "Search all the logs to verify the application status",
        tags = {"Log Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the key to see the application logs",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = LogRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The logs were recovered successfully",
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
    public ResponseEntity<?> findAllLogs(@RequestBody LogRequest logRequest) {
        return this.logService.findAllLogs(logRequest);
    }

    // Endpoint para notificar a usuarios que actualicen data necesaria o actualización de logs de error
    @PostMapping(value = "/notify/account/not-found")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Notify user that the bank account couldn't be found",
        description = "Notify user that the bank account couldn't be found",
        tags = {"Log Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the required data to continue",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = LogNotifyRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Users were successfully notified",
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
    public ResponseEntity<?> notifyAccountNotFound(@RequestBody LogNotifyRequest logRequest) {
        return this.logService.notifyAccountNotFound(logRequest);
    }

    @PutMapping(value = "/update")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Update error logs",
        description = "Update error logs",
        tags = {"Log Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the required data to continue",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = LogRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Users were successfully notified",
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
    public ResponseEntity<?> updateErrorLogs(@RequestBody LogRequest logRequest) {
        return this.logService.updateErrorLogs(logRequest);
    }
    
}
