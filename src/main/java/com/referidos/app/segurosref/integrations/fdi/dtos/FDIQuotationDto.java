package com.referidos.app.segurosref.integrations.fdi.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class FDIQuotationDto {

    private int internalErrorCode;

    public FDIQuotationDto(int internalErrorCode) {
        this.internalErrorCode = internalErrorCode;
    }

}
