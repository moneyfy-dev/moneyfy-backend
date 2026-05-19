package com.referidos.app.segurosref.integrations.bci.pojos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BCITokenCreatePojo extends BaseIntegrationResponse {

    @EqualsAndHashCode.Include
    private String token;

    public BCITokenCreatePojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
