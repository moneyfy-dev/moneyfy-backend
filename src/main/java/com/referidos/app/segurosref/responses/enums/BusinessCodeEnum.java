package com.referidos.app.segurosref.responses.enums;

public enum BusinessCodeEnum {

    EXTERNAL_SERVICE_ERROR(50, "Error de conexión con el proveedor"),
    INSUFFICIENT_FUNDS(51, "Saldo insuficiente para la operación"),
    QUOTA_LIMIT_REACHED(60, "Has superado el límite de cotizaciones");

    private final int errorCode;
    private final String errorDescription;

    BusinessCodeEnum(int errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public int getErrorCode() { return this.errorCode; }
    public String getErrorDescription() { return this.errorDescription; }

}
