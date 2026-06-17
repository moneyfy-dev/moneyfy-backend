# Fix: Correcciones adicionales en flujos y colecciones de Postman

**Fecha:** 17 de Junio de 2026
**Autor:** Antigravity (Agent)
**Skill:** Senior Backend Developer / Postman Collection Manager

## Resumen de Cambios

1. **Limpieza de Directorios Duplicados**:
   - En las colecciones de Postman (`MoneyFy_Dev_API` y `MoneyFy_Prod_API`) se eliminó la carpeta duplicada `Region Controller` generada accidentalmente, conservando únicamente la que se renombró desde `Cities` hacia `Regions`.

2. **Eliminación de Flujos Obsoletos**:
   - Se removió permanentemente de la colección de Postman el endpoint `/auth/confirm/device/change`, dado que ya no forma parte del diseño actual de la API.

3. **Inyección Limpia de Tokens por Entorno y Flujo**:
   - Se ajustó el script javascript de Postman para garantizar que la inyección de los tokens se aplique en todos los flujos dinámicos pertinentes: `login`, `/auth/confirm/registration` (register step 2), y `/auth/confirm/password/reset`.
   - El script asegura aislar las variables usando correctamente el prefijo del entorno, asignando exclusivamente `Dev - Session Token` y `Dev - Refresh Token` en desarrollo, y lo mismo con el prefijo `Prod` en producción.
   - El contexto del agente en `agent/commands/postman.md` fue actualizado para asimilar esta regla permanentemente en futuros comandos de postman.

## Siguientes Pasos
Validar que la segregación de tokens funcione perfectamente en ambos entornos dentro de la aplicación de Postman y generar el commit consolidado.
