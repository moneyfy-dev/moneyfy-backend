package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.requests.ManagerRegisterRequest;
import com.referidos.app.segurosref.requests.EmailRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;

public interface ManagerAuthService {
    ResponseEntity<GeneralResponse> login(UserLoginRequest request) throws JsonProcessingException;

    ResponseEntity<GeneralResponse> logout(String email);

    ResponseEntity<GeneralResponse> createManager(ManagerRegisterRequest request);

    ResponseEntity<GeneralResponse> restorePassword(EmailRequest request);

    ResponseEntity<GeneralResponse> confirmPasswordReset(PasswordResetRequest request) throws JsonProcessingException;

    ResponseEntity<GeneralResponse> resendCode(EmailRequest request);
}
