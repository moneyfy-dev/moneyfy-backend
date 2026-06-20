package com.referidos.app.segurosref.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.requests.ManagerRegisterRequest;
import com.referidos.app.segurosref.requests.EmailRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.ManagerAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/manager/auth")
@Tag(name = "Manager Authentication", description = "Controller to authenticate the admin users")
public class ManagerAuthController {

    private final ManagerAuthService managerAuthService;

    @PostMapping(value = "/log-in")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Manager login")
    public ResponseEntity<GeneralResponse> login(@RequestBody UserLoginRequest request) throws JsonProcessingException {
        return managerAuthService.login(request);
    }

    @PostMapping(value = "/logout")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @Operation(summary = "Manager logout", description = "Logout from the application and revoke tokens")
    public ResponseEntity<GeneralResponse> logout(Authentication auth) {
        return managerAuthService.logout(auth.getName());
    }

    @PostMapping(value = "/create")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @Operation(summary = "Create manager", description = "Creates a new manager (must be requested by an existing admin)")
    public ResponseEntity<GeneralResponse> createManager(@RequestBody ManagerRegisterRequest request) {
        return managerAuthService.createManager(request);
    }

    @PostMapping(value = "/restore/password")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Restore password for manager", description = "Sends a recovery code to the manager's email")
    public ResponseEntity<GeneralResponse> restorePassword(@RequestBody EmailRequest request) {
        return managerAuthService.restorePassword(request);
    }

    @PutMapping(value = "/confirm/password/reset")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Confirm password reset for manager", description = "Confirms password change or activates account")
    public ResponseEntity<GeneralResponse> confirmPasswordReset(@RequestBody PasswordResetRequest request)
            throws JsonProcessingException {
        return managerAuthService.confirmPasswordReset(request);
    }

    @PutMapping(value = "/resend/code")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Resend code for manager", description = "Resends the recovery code")
    public ResponseEntity<GeneralResponse> resendCode(@RequestBody EmailRequest request) {
        return managerAuthService.resendCode(request);
    }

}
