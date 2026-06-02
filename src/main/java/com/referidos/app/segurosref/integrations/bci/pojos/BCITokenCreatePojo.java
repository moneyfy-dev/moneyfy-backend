package com.referidos.app.segurosref.integrations.bci.pojos;

import org.springframework.http.HttpStatusCode;

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
    private String responseBodyStr;
    private String statusOrErrorStr;
    private HttpStatusCode statusResponse;

    public BCITokenCreatePojo(Integer internalErrorCode) {
        super(internalErrorCode);
    }

    public BCITokenCreatePojo(Integer internalErrorCode, String responseBodyStr, String statusOrErrorStr, HttpStatusCode statusResponse) {
        super(internalErrorCode);
        this.responseBodyStr = responseBodyStr;
        this.statusOrErrorStr = statusOrErrorStr;
        this.statusResponse = statusResponse;
    }

}
