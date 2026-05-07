package com.referidos.app.segurosref.integrations.fdi.pojos;

import java.util.List;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // Compara usando la clase padre (internalErrorCode)
public class FDIQuoteDealPojo extends BaseIntegrationResponse {

    @EqualsAndHashCode.Exclude // Ignora la colección para el equals/hashCode
    private List<FDIQuoteItemPojo> items;

    public FDIQuoteDealPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
