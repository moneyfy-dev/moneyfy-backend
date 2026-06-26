package com.referidos.app.segurosref.dtos.manager;

import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingQuotesResponseDto {
    private String message;
    private int status;
    private PendingQuotesData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingQuotesData {
        private ManagerDto manager;
        private List<InsurerGroupDto> quotations;
        private List<PendingQuoteErrorDto> errors;
    }
}
