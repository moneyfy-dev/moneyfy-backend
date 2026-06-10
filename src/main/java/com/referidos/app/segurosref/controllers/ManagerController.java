package com.referidos.app.segurosref.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.dtos.manager.DashboardResponseDto;
import com.referidos.app.segurosref.services.ManagerService;

@RestController
@RequestMapping("/api/v1/manager")
public class ManagerController {

    @Value("${moneyfy.api-key}")
    private String moneyfyApiKey;

    @Autowired
    private ManagerService managerService;

    // TODO: Ajuste momentaneo, se debe arreglar a futuro usando
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/quotes")
    public ResponseEntity<DashboardResponseDto> getQuotesDashboard(
            @RequestHeader(value = "X-Moneyfy-Api-Key", required = false) String apiKey) {

        if (apiKey == null || !apiKey.equals(moneyfyApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new DashboardResponseDto("No autorizado", HttpStatus.UNAUTHORIZED.value(), null));
        }

        DashboardResponseDto response = managerService.getQuotesDashboard();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
