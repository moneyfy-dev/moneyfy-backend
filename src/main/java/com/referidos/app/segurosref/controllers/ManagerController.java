package com.referidos.app.segurosref.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.services.ManagerService;

import com.referidos.app.segurosref.dtos.manager.DashboardPaginatedResponseDto;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.referidos.app.segurosref.dtos.manager.PayQuotesRequest;
import jakarta.servlet.http.HttpServletRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerController {

    @Value("${moneyfy.api-key}")
    private String moneyfyApiKey;

    private final ManagerService managerService;

    // TODO: Ajuste momentaneo, se debe arreglar a futuro usando
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/quotes")
    public ResponseEntity<DashboardPaginatedResponseDto> getQuotesDashboard(
            @RequestHeader(value = "X-Moneyfy-Api-Key", required = true) String apiKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String quoteStatus) {

        if (apiKey == null || !apiKey.equals(moneyfyApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new DashboardPaginatedResponseDto("No autorizado",
                            HttpStatus.UNAUTHORIZED.value(), null));
        }

        DashboardPaginatedResponseDto response = managerService.getQuotesDashboard(page, size, userId, quoteStatus);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/pay-quotes")
    public ResponseEntity<?> payQuotes(
            @RequestHeader(value = "X-Moneyfy-Api-Key", required = true) String apiKey,
            @RequestBody PayQuotesRequest request) {

        if (apiKey == null || !apiKey.equals(moneyfyApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new DashboardPaginatedResponseDto("No autorizado",
                            HttpStatus.UNAUTHORIZED.value(), null));
        }

        return managerService.payQuotes(request);
    }

    @PutMapping("/finalize/quote")
    public ResponseEntity<?> finalizeQuote(@RequestBody FinalizeQuoteRequest finalizeQuote,
            HttpServletRequest request) {
        return managerService.finalizeQuote(finalizeQuote, request);
    }
}