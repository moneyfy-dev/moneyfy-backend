package com.referidos.app.segurosref.services.impl;
import com.referidos.app.segurosref.services.LogService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.LogModel;
import com.referidos.app.segurosref.repositories.LogRepository;

import org.springframework.security.core.context.SecurityContextHolder;
import com.referidos.app.segurosref.models.ManagerModel;
import com.referidos.app.segurosref.dtos.manager.ManagerDto;
import com.referidos.app.segurosref.repositories.ManagerRepository;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogRepository logRepository;
    private final ManagerRepository managerRepository;

    @Override
    public ResponseEntity<?> findAllLogs() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<ManagerModel> managerOpt = managerRepository.findByEmail(email);

        if (managerOpt.isEmpty()) {
            return ResponseHelper.unauthorized("no autorizado");
        }

        ManagerModel managerDB = managerOpt.get();
        ManagerDto managerDto = ManagerDto.builder()
                .managerId(managerDB.getManagerId())
                .name(managerDB.getName())
                .surname(managerDB.getSurname())
                .email(managerDB.getEmail())
                .status(managerDB.getStatus())
                .build();

        List<LogModel> logsDB = logRepository.findAll();
        return ResponseHelper.ok("Se han recuperados los logs de la API",
                Map.of("logs", logsDB, "manager", managerDto));
    }

}
