package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;

import com.referidos.app.segurosref.dtos.manager.DashboardPaginatedResponseDto;
import com.referidos.app.segurosref.dtos.manager.PayQuotesRequest;

public interface ManagerService {

    DashboardPaginatedResponseDto getQuotesDashboard(int page, int size, String userId, String quoteStatus);

    ResponseEntity<?> getDashboardSummary();

    ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, HttpServletRequest request);

    ResponseEntity<?> payQuotes(PayQuotesRequest request);

    ResponseEntity<?> generatePayQuotesReport(com.referidos.app.segurosref.dtos.manager.PayQuotesReportRequest request);
}
