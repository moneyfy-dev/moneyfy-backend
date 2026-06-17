# Fix: Actualización de Postman para renombrado de directorio y extracción de tokens

**Fecha:** 17 de Junio de 2026
**Autor:** Antigravity (Agent)
**Skill:** Senior Backend Developer / Postman Collection Manager

## Resumen de Cambios

1. **Renombrado del Directorio en Colecciones**:
   - En las colecciones de Postman (`MoneyFy_Dev_API` y `MoneyFy_Prod_API`) se detectó que el directorio "Cities" todavía conservaba su antiguo nombre.
   - Se renombró el directorio "Cities" por "Regions" para mantener congruencia total con la refactorización reciente del modelo de regiones.

2. **Ajuste de Script de Extracción de Tokens**:
   - La estructura de respuesta del login y flujos de confirmación cambió, anidando los tokens en `jsonData.data.sessionToken` y `jsonData.data.refreshToken`.
   - Se actualizó el script javascript bajo el evento `test` en Postman para capturar e inyectar correctamente los tokens bajo la nueva estructura en el entorno dinámico.

3. **Inyección en Nuevos Flujos**:
   - El mismo script de auto-inyección de tokens se acopló en las peticiones de:
     - `login`
     - `confirm registration` (confirmación de registro)
     - `confirm password reset` (confirmación de reseteo de contraseña)
   - Esto agiliza las pruebas, pues al ejecutar cualquiera de estos flujos con éxito, la colección de Postman actualizará de inmediato sus variables de sesión, evitando hacerlo de forma manual.

## Siguientes Pasos
Continuar probando y validando las integraciones con el cliente y mantener este estándar de inyección automatizada de tokens para futuros flujos de autenticación.
