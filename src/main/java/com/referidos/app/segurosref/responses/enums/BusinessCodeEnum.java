package com.referidos.app.segurosref.responses.enums;

public enum BusinessCodeEnum {

    // Errores Generales de la Aplicación
    APP_GENERAL_BAD_REQUEST(1, "Error al procesar la solicitud"),
    APP_INCORRECT_FORMAT(2, "El formato de los datos enviados no es válido o es incorrecto para procesar la solicitud"),
    
    // Integración BCI (Códigos 40+)
    BCI_TOKEN_CREATION_EXCEPTION(40, "Error de excepción al realizar petición para generar token en servicio externo (BCI)"),
    BCI_TOKEN_CREATION_UNEXPECTED_RESPONSE(41, "Respuesta no esperada al realizar petición para generar token (BCI)"),

    // Integración Proveedores Generales
    EXTERNAL_SERVICE_ERROR(50, "Error de conexión con el proveedor"),
    
    // Integración FDI (Códigos 70+)
    FDI_DEAL_EXCEPTION(70, "Error de excepción al intentar crear el deal en el servicio externo (FDI)"),
    FDI_DEAL_UNEXPECTED_RESPONSE(71, "Respuesta no esperada del servicio externo al procesar el deal"),
    FDI_DEAL_UPDATE_EXCEPTION(72, "Error de excepción al intentar actualizar el deal en el servicio externo (FDI)"),
    FDI_DEAL_UPDATE_UNEXPECTED_RESPONSE(73, "Respuesta no esperada del servicio externo al actualizar la información del deal"),
    FDI_ITEM_CREATION_EXCEPTION(74, "Error de excepción al intentar crear el ítem asegurable en el servicio externo (FDI)"),
    FDI_ITEM_CREATION_UNEXPECTED_RESPONSE(75, "Respuesta no esperada del servicio externo al crear el ítem asegurable"),
    FDI_QUOTE_DEAL_EXCEPTION(76, "Error de excepción al intentar solicitar la cotización final del deal (FDI)"),
    FDI_QUOTE_DEAL_UNEXPECTED_RESPONSE(77, "Respuesta no esperada del servicio externo al procesar la cotización del deal");

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
        return APP_GENERAL_BAD_REQUEST; 
    }

}
