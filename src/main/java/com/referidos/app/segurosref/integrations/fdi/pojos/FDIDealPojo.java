package com.referidos.app.segurosref.integrations.fdi.pojos;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class FDIDealPojo {

    private String token;
    private Integer internalErrorCode;

    public FDIDealPojo(Integer internalErrorCode) {
        this.internalErrorCode = internalErrorCode;
    }

}
