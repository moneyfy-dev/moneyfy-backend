package com.referidos.app.segurosref.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.ManagerAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/manager/auth")
@Tag(name = "Manager Authentication", description = "Controller to authenticate the admin users")
public class ManagerAuthController {

    private final ManagerAuthService managerAuthService;

    @PostMapping(value = "/log-in")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Manager login")
    public ResponseEntity<GeneralResponse> login(@RequestBody UserLoginRequest request) throws JsonProcessingException {
        return managerAuthService.login(request);
    }

}
