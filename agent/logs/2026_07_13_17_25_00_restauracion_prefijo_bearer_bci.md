# Registro de Modificaciones - Restauración de Prefijo Bearer en BCI Vehicle Client

## Fecha y Hora
2026-07-13 17:25:00

## Descripción de los Cambios
- **Restauración del prefijo "Bearer ":** Se volvió a agregar `JwtConfig.PREFIX_TOKEN` en la cabecera `Authorization` para las consultas del cliente de BCI (`BCIVehicleClient.java`).
- **Motivo:** Al usar el método HTTP `POST` correcto, el servidor de BCI arrojaba error `401 Unauthorized` si el token no incluía el prefijo `"Bearer "` (el cual Postman agrega automáticamente tras bambalinas al configurar Bearer Token).

## Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/integrations/bci/clients/BCIVehicleClient.java`
