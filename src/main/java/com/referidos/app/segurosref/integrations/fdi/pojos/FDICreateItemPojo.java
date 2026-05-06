package com.referidos.app.segurosref.integrations.fdi.pojos;

import com.referidos.app.segurosref.integrations.BaseIntegrationResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FDICreateItemPojo extends BaseIntegrationResponse {
    
    private String status;
    private Integer code;
    private Integer itemId;

    public FDICreateItemPojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

}
