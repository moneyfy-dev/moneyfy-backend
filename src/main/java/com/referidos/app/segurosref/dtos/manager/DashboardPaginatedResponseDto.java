package com.referidos.app.segurosref.dtos.manager;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPaginatedResponseDto {
    private String message;
    private int status;
    private PaginatedData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginatedData {
        private List<DashboardQuoteDto> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
