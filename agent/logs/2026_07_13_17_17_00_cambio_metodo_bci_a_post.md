# Registro de Modificaciones - Cambio de Método HTTP a POST en BCI Vehicle Client

## Fecha y Hora
2026-07-13 17:17:00

## Descripción de los Cambios
- **Ajuste del método HTTP:** Se cambió el método HTTP de `GET` a `POST` en la petición realizada al servicio externo de BCI para consultar los datos del vehículo por patente.
- **Motivo:** El servicio externo reportaba un error `405 Method Not Allowed` al utilizar `GET`.

## Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/integrations/bci/clients/BCIVehicleClient.java`
