package com.referidos.app.segurosref.services;

import com.referidos.app.segurosref.dtos.manager.DashboardPaginatedResponseDto;

public interface ManagerService {

    DashboardPaginatedResponseDto getQuotesDashboard(int page, int size, String userId, String quoteStatus);

}
