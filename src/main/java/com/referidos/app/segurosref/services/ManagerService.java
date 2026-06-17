package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.dtos.manager.DashboardPaginatedResponseDto;
import com.referidos.app.segurosref.dtos.manager.PayQuotesRequest;

public interface ManagerService {

    DashboardPaginatedResponseDto getQuotesDashboard(int page, int size, String userId, String quoteStatus);

    ResponseEntity<?> payQuotes(PayQuotesRequest request);
}
