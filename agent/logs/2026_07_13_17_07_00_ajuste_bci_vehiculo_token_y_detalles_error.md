# Registro de Modificaciones - Ajuste de Token BCI y Trazabilidad de Errores en Búsqueda de Vehículo

## Fecha y Hora
2026-07-13 17:07:00

## Descripción de los Cambios
Este registro documenta el ajuste realizado al cliente de integración de BCI para la búsqueda de vehículos por patente y la mejora en la devolución de detalles de error en el servicio de cotización.

### Modificaciones en `BCIVehicleClient.java`
- **Eliminación del prefijo "Bearer ":** Se eliminó la concatenación de `JwtConfig.PREFIX_TOKEN` del header de autorización al invocar el endpoint externo de BCI (`/DatosVehiculo`). El servicio de BCI espera recibir el token directamente sin el prefijo "Bearer " en la cabecera `Authorization`.
- **Formateo y Limpieza:** Se corrigieron algunas líneas con indentación y saltos de línea para mantener el estándar de formato Java del proyecto.

### Modificaciones en `QuoterServiceImpl.java`
- **Trazabilidad de errores en respuesta (Código 46):** En el método `searchVehicle` (o en la sección de generación de respuesta de cotización cuando falla la búsqueda de vehículo), se agregaron campos adicionales en el mapa `dataResponse` ante respuestas fallidas (`vehicleResponse.hasError()`):
  - `errorStatus`: Contiene el código de estado HTTP recibido de la integración.
  - `errorStatusInfo`: Información descriptiva del estado o tipo de error.
  - `errorResponseBodyStr`: El cuerpo completo del JSON de error retornado por el servicio externo (BCI).
  - Esto facilita la depuración rápida de fallas de integración en lugar de solo visualizar el código genérico `46` (BCI_VEHICLE_LOOKUP_EXCEPTION).

## Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/integrations/bci/clients/BCIVehicleClient.java`
- `src/main/java/com/referidos/app/segurosref/services/impl/QuoterServiceImpl.java`
