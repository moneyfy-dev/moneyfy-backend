package com.referidos.app.segurosref.dtos.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingQuoteErrorDto {
    private String userId;
    private String quotationId;
    private String insurerAlias;
    private String errorMessage;
}
