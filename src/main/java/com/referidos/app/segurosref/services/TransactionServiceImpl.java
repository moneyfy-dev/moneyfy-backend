package com.referidos.app.segurosref.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.repositories.TransactionRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Value(value = "${api.key.moneyfy}")
    private String apiKeyMF;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> findById(String transactionId, HttpServletRequest request) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        if(transactionId == null) {
            return ResponseHelper.failedDependency("no se ha podido identificar el recurso", "failed dependency");
        }
        Optional<TransactionModel> transactionOptional = transactionRepository.findById(transactionId);
        if(transactionOptional.isPresent()) {
            return ResponseHelper.ok("La transacción se ha recuperado", Map.of("transaction", transactionOptional.get()));
        }
        return ResponseHelper.failedDependency("no se ha podido identificar el recurso", "failed dependency");
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> findAllByUserReferringFound(HttpServletRequest request) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        List<TransactionModel> transactionsDB = transactionRepository.findAllByUserReferringFound(false);
        return ResponseHelper.ok("Las transacciones se han podido recuperar", Map.of("transactionsDB", transactionsDB));
    }

}
