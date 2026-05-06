package com.referidos.app.segurosref.integrations.fdi.dtos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)  // Importante para Lombok con herencia
public class FDIQuotationDto extends BaseIntegrationResponse {
    
    // Incluir campos necesarios

    public FDIQuotationDto(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
