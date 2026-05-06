package com.referidos.app.segurosref.integrations;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class BaseIntegrationResponse {

    protected Integer internalErrorCode;

    // Constructor para inicializar con el código de error (como tus 70, 71, etc.)
    public BaseIntegrationResponse(Integer internalErrorCode) {
        this.internalErrorCode = internalErrorCode;
    }
    
    // Método de ayuda para saber si hubo error sin comparar con -1 manualmente
    public boolean hasError() {
        return internalErrorCode != null && internalErrorCode != -1;
    }

}
