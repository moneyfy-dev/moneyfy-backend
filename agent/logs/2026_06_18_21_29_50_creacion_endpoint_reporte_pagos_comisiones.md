# Registro de Actualización: Endpoint Reporte de Pagos de Comisiones

**Fecha:** 18 de Junio de 2026
**Hora:** 21:29:50

## Resumen de Cambios
Se creó exitosamente el endpoint `/api/v1/manager/pay-quotes/report`, el cual antecede al endpoint final de pagos masivos (`/pay-quotes`), y que tiene como propósito pre-generar un reporte de comisiones pendientes con la nómina para el banco y el payload final para el backend.

## Flujos y Configuraciones Alteradas:
- **TransactionRepository**: Se añadió la consulta `findAllByApprovalDateBetweenAndStatus` para buscar transacciones en estado `Aprobado` durante un rango de fechas.
- **DTOs (`com/referidos/app/segurosref/dtos/manager/`)**:
  - Creados: `PayQuotesReportRequest`, `ConflictDto`, `BankPayrollDto`, `PayQuotesReportResponse`.
- **ManagerService / ManagerServiceImpl**:
  - Añadida la lógica en `generatePayQuotesReport`. Se agrupan transacciones por `userId`.
  - Se valida obligatoriamente que el usuario tenga una cuenta bancaria (`AccountModel`) seleccionada.
  - Genera arreglos independientes: `bankPayroll` (información plana con el banco), `backendPayload` (payload listo para enviar a `/pay-quotes`) y un listado de `conflicts` para advertir sobre matemáticos de montos <= 0 o falta de cuentas bancarias.
- **ManagerController**:
  - Implementación del nuevo `POST /api/v1/manager/pay-quotes/report`, resguardado por el encabezado obligatorio `X-Moneyfy-Api-Key`.
- **Postman Collections**:
  - Se corrigió la estructura del endpoint original `/pay-quotes` dentro de las colecciones Dev y Prod (en la carpeta `agent/postman/`) para emparejarla con las especificaciones de envío actuales (`usersQuotes`).
