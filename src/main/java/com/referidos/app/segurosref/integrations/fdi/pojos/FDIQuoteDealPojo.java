package com.referidos.app.segurosref.integrations.fdi.pojos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FDIQuoteDealPojo extends BaseIntegrationResponse {

    // Datos propios

    public FDIQuoteDealPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
