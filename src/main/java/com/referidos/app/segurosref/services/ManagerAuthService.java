package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;

public interface ManagerAuthService {
    ResponseEntity<GeneralResponse> login(UserLoginRequest request) throws JsonProcessingException;
}
