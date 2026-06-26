# Registro de Avances: Endpoint Pending Quotes & Fixes

**Fecha:** 25 de Junio de 2026

## Resumen de Cambios

1. **Nuevo Endpoint de Manager (`/pending-quotes`)**:
   - Se implementó el endpoint `GET /api/v1/manager/pending-quotes` en `ManagerController` y `ManagerServiceImpl` (restringido para administradores).
   - Se agregó agregación en MongoDB para recuperar usuarios con cotizadores en estado "Pendiente".
   - Se agruparon las cotizaciones por `insurerAlias` incluyendo los datos correspondientes de las aseguradoras principales, aseguradora4 (BCI) y aseguradora5 (FDI).

2. **Refactorización de DTOs y Respuestas**:
   - Se resolvió un error de compilación cambiando `PendingQuotesResponseDto` (que no podía extender el record `GeneralResponse`) a una clase regular.
   - Se estandarizó el uso de `ResponseHelper.unauthorized()` para manejar fallos de autorización en el servicio de Manager.

3. **Ajustes en el Flujo de Cotizaciones**:
   - Se corrigió `QuotationPlanDto` removiendo el campo redundante `insurer` ya que la consulta se hace por aseguradora.
   - Se ajustó el constructor de `QuotationPlanDto` y `QuoterHelper` para asignar valores por defecto en los campos de aseguradoras BCI y FDI en aseguradoras de prueba.
   - En el método `selectPlan` (`QuoterServiceImpl`), se agregó la lógica para remover los prefijos `"BCI_"` y `"FDI_"` del campo `planId` antes de persistirlo en base de datos.

4. **Actualización de Colecciones de Postman**:
   - Se incluyó el nuevo endpoint en las colecciones `MoneyFy_Dev_API` y `MoneyFy_Prod_API`.
   - Se ajustaron las variables de entorno para usar `{{Dev - URL}}` y `{{Prod - URL}}` junto con las variables correctas de tokens.
   - Se introdujo y corrigió la cabecera `X-New-Refresh-Token` tanto en el endpoint de pending-quotes como en el de moneyfyers dashboard.
