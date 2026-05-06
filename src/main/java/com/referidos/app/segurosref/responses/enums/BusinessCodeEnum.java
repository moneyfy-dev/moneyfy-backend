package com.referidos.app.segurosref.responses.enums;

public enum BusinessCodeEnum {

    EXTERNAL_SERVICE_ERROR(50, "Error de conexión con el proveedor"),
    INSUFFICIENT_FUNDS(51, "Saldo insuficiente para la operación"),
    QUOTA_LIMIT_REACHED(60, "Has superado el límite de cotizaciones"),
    FDI_DEAL_EXCEPTION(70, "Error de excepción al intentar crear el deal en el servicio externo (FDI)"),
    FDI_DEAL_UNEXPECTED_RESPONSE(71, "Respuesta no esperada del servicio externo al procesar el deal"),
    FDI_DEAL_UPDATE_EXCEPTION(72, "Error de excepción al intentar actualizar el deal en el servicio externo (FDI)"),
    FDI_DEAL_UPDATE_UNEXPECTED_RESPONSE(73, "Respuesta no esperada del servicio externo al actualizar la información del deal");

    private final int errorCode;
    private final String errorDescription;

    BusinessCodeEnum(int errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public int getErrorCode() { return this.errorCode; }
    public String getErrorDescription() { return this.errorDescription; }

    // Buscar error específico por código mapeado
    public static BusinessCodeEnum fromCode(int code) {
        for (BusinessCodeEnum b : BusinessCodeEnum.values()) {
            if (b.getErrorCode() == code) {
                return b;
            }
        }
        return EXTERNAL_SERVICE_ERROR; // Un default por si el código no existe
    }

}
