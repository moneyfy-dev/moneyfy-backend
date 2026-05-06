package com.referidos.app.segurosref.integrations.fdi.pojos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class FDICreateDealPojo extends BaseIntegrationResponse {

    private String token;

    public FDICreateDealPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
