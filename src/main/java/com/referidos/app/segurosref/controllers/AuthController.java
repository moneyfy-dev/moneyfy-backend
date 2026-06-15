package com.referidos.app.segurosref.controllers;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.requests.ConfirmUserRequest;
import com.referidos.app.segurosref.requests.EmailRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.UserDetailsServiceImpl;
import com.referidos.app.segurosref.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth")
@Tag(name = "Authentication", description = "Controller to authenticate the system users")
public class AuthController {

    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @PostMapping(value = "/register")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Receive a confirmation code for common users")
    public ResponseEntity<GeneralResponse> register(@RequestBody UserRegisterRequest user,
            BindingResult bindingResult) {
        userService.validateSimpleUser(user, bindingResult);
        if (bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada",
                    DataHelper.buildErrorFields(bindingResult));
        }
        return userDetailsServiceImpl.userRegister(user);
    }

    @PostMapping(value = "/confirm/registration")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Complete your user registration")
    public ResponseEntity<GeneralResponse> confirmRegistration(@RequestBody ConfirmUserRequest confirm)
            throws JsonProcessingException {
        return userDetailsServiceImpl.confirmRegistration(confirm);
    }

    @PostMapping(value = "/log-in")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "User login")
    public ResponseEntity<GeneralResponse> login(@RequestBody UserLoginRequest requestUser)
            throws JsonProcessingException {
        return userDetailsServiceImpl.userLogin(requestUser);
    }

    @PostMapping(value = "/logout")
    @PreAuthorize(value = "isAuthenticated()")
    @Operation(summary = "User logout", description = "Logout from the application and revoke tokens")
    public ResponseEntity<GeneralResponse> logout(Authentication auth) {
        return userDetailsServiceImpl.logout(auth.getName());
    }

    @PostMapping(value = "/restore/password")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Get the authorization code to change your user's password")
    public ResponseEntity<GeneralResponse> restorePassword(@RequestBody EmailRequest emailRequest) {
        return userDetailsServiceImpl.restorePassword(emailRequest.email());
    }

    @PutMapping(value = "/confirm/password/reset")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Restore your user's password")
    public ResponseEntity<GeneralResponse> confirmPasswordReset(@RequestBody PasswordResetRequest passwordReset) {
        return userDetailsServiceImpl.confirmPasswordReset(passwordReset);
    }

    @PutMapping(value = "/resend/code")
    @PreAuthorize(value = "permitAll()")
    @Operation(summary = "Resending authorization code")
    public ResponseEntity<GeneralResponse> resendUserCode(@RequestBody EmailRequest emailRequest) {
        return userDetailsServiceImpl.resendUserCode(emailRequest.email(), emailRequest.type());
    }

    @DeleteMapping(value = "/disable/account")
    @PreAuthorize(value = "hasRole('USER')")
    @Operation(summary = "Disable user account")
    public ResponseEntity<GeneralResponse> disableAccount(Authentication auth) {
        return userDetailsServiceImpl.disableAccount(auth.getName());
    }
}
