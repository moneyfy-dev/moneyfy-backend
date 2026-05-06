package com.referidos.app.segurosref.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.models.LogModel;
import com.referidos.app.segurosref.repositories.LogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LogServiceImpl implements LogService {

    @Value(value = "${moneyfy.api-key}")
    private String apiKeyMF;

    @Autowired
    private LogRepository logRepository;

    @Override
    public ResponseEntity<?> findAllLogs(HttpServletRequest request) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        List<LogModel> logsDB = logRepository.findAll();
        return ResponseHelper.ok("Se han recuperados los logs de la API", Map.of("logs", logsDB));
    }

}
