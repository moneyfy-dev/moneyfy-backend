package com.referidos.app.segurosref.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.services.ManagerService;

import com.referidos.app.segurosref.dtos.manager.DashboardPaginatedResponseDto;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.referidos.app.segurosref.dtos.manager.PayQuotesRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping("/dashboard/quotes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardPaginatedResponseDto> getQuotesDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String quoteStatus) {

        DashboardPaginatedResponseDto response = managerService.getQuotesDashboard(page, size, userId, quoteStatus);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardSummary() {

        return managerService.getDashboardSummary();
    }

    @PutMapping("/finalize/quote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> finalizeQuote(@RequestBody FinalizeQuoteRequest finalizeQuote) {
        return managerService.finalizeQuote(finalizeQuote);
    }

    @PostMapping("/pay-quotes/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generatePayQuotesReport(
            @RequestBody com.referidos.app.segurosref.dtos.manager.PayQuotesReportRequest request) {

        return managerService.generatePayQuotesReport(request);
    }

    @PostMapping("/pay-quotes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> payQuotes(
            @RequestBody PayQuotesRequest request) {

        return managerService.payQuotes(request);
    }

}
