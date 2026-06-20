package com.referidos.app.segurosref.services.impl;
import com.referidos.app.segurosref.services.TransactionService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.repositories.TransactionRepository;

import org.springframework.security.core.context.SecurityContextHolder;
import com.referidos.app.segurosref.models.ManagerModel;
import com.referidos.app.segurosref.dtos.manager.ManagerDto;
import com.referidos.app.segurosref.repositories.ManagerRepository;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final ManagerRepository managerRepository;

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> findById(String transactionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        ManagerModel managerDB = managerRepository.findByEmail(email).orElse(null);
        if (managerDB == null) {
            return ResponseHelper.unauthorized("no autorizado");
        }
        ManagerDto managerDto = ManagerDto.builder()
                .managerId(managerDB.getManagerId())
                .name(managerDB.getName())
                .surname(managerDB.getSurname())
                .email(managerDB.getEmail())
                .status(managerDB.getStatus())
                .build();
        if (transactionId == null) {
            return ResponseHelper.failedDependency("no se ha podido identificar el recurso", "failed dependency");
        }
        Optional<TransactionModel> transactionOptional = transactionRepository.findById(transactionId);
        if (transactionOptional.isPresent()) {
            return ResponseHelper.ok("La transacción se ha recuperado",
                    Map.of("transaction", transactionOptional.get(), "manager", managerDto));
        }
        return ResponseHelper.failedDependency("no se ha podido identificar el recurso", "failed dependency");
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> findAllByUserReferringFound() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        ManagerModel managerDB = managerRepository.findByEmail(email).orElse(null);
        if (managerDB == null) {
            return ResponseHelper.unauthorized("no autorizado");
        }
        ManagerDto managerDto = ManagerDto.builder()
                .managerId(managerDB.getManagerId())
                .name(managerDB.getName())
                .surname(managerDB.getSurname())
                .email(managerDB.getEmail())
                .status(managerDB.getStatus())
                .build();
        List<TransactionModel> transactionsDB = transactionRepository.findAllByUserReferringFound(false);
        return ResponseHelper.ok("Las transacciones se han podido recuperar",
                Map.of("transactionsDB", transactionsDB, "manager", managerDto));
    }

}
