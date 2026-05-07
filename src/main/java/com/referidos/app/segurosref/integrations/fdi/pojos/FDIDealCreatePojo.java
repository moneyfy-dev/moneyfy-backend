package com.referidos.app.segurosref.integrations.fdi.pojos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FDIDealCreatePojo extends BaseIntegrationResponse {

    @EqualsAndHashCode.Include // Solo este campo se usará para equals y hashcode
    private String token;

    public FDIDealCreatePojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
