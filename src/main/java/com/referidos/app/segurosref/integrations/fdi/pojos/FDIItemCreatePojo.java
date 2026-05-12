package com.referidos.app.segurosref.integrations.fdi.pojos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FDIItemCreatePojo extends BaseIntegrationResponse {
    
    private String status;
    private Integer code;
    @EqualsAndHashCode.Include
    private Integer itemId;

    public FDIItemCreatePojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
