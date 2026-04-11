package com.referidos.app.segurosref.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LogServiceImpl implements LogService {

    @Value(value = "${api.key.moneyfy}")
    private String apiKeyMF;

    @Override
    public ResponseEntity<?> findAllLogs(HttpServletRequest request) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        return null;
    }

}
