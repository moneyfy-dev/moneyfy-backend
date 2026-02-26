package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.requests.AccountRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/accounts")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Accounts",
    description = "Controller to handle the user accounts"
)
public class AccountController {

    @Autowired
    private AccountService accountService;

    // ENDPOINTS RELACIONADOS CON EL MANEJO DE LAS CUENTAS BANCARIAS DEL USUARIO
    @PostMapping(value = "/create")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Create a new account",
        description = "Create a new account for your user authenticated",
        tags = {"Accounts"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Put the information required",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = AccountRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "The user account was created successfully",
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
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<GeneralResponses> create(@RequestBody AccountRequest account, BindingResult bindingResult, Authentication auth) {
        accountService.validate(account, bindingResult, true);
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return accountService.create(account, auth.getPrincipal().toString());
    }

    @PutMapping(value = "/update")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Update your account",
        description = "Update your account for your user authenticated",
        tags = {"Accounts"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Put the information required",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = AccountRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The user account was updated successfully",
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
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<GeneralResponses> update(@RequestBody AccountRequest account, BindingResult bindingResult, Authentication auth) {
        accountService.validate(account, bindingResult, false);
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return accountService.update(account, auth.getPrincipal().toString());
    }

    @DeleteMapping(value = "/delete/{accountId}")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Delete your account",
        description = "Delete your account for your user authenticated",
        tags = {"Accounts"},
        parameters = {
            @Parameter(
                name = "accountId",
                in = ParameterIn.PATH,
                description = "Enter the user account id for the deletion",
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
                description = "The user account was deleted successfully",
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
    public ResponseEntity<GeneralResponses> delete(@PathVariable String accountId, Authentication auth) {
        if(!ObjectId.isValid(accountId)) {
            return ResponseHelper.failedDependency("no se ha podido identificar el recurso", null);
        }
        return accountService.delete(accountId, auth.getPrincipal().toString());
    }

    @PostMapping(value = "/select/{accountId}")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Select the account",
        description = "Select the account that will receive the income",
        tags = {"Accounts"},
        parameters = {
            @Parameter(
                name = "accountId",
                in = ParameterIn.PATH,
                description = "Enter the user account id for the selection",
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
                description = "The user account was selected successfully",
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
    public ResponseEntity<GeneralResponses> select(@PathVariable String accountId, Authentication auth) {
        if(!ObjectId.isValid(accountId)) {
            return ResponseHelper.failedDependency("no se ha podido identificar el recurso", null);
        }
        return accountService.select(accountId, auth.getPrincipal().toString());
    }

}
