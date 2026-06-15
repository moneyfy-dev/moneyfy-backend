# Authentication Refactor Tasks

- [x] `AuthModel.java`: Añadir `boolean isAccountConfirmed` y eliminar `String refreshToken`.
- [x] `JwtValidationFilter.java`:
  - [x] Reemplazar `request.getRequestURI()` por `request.getServletPath()` para ignorar el context path en las rutas públicas.
  - [x] Eliminar `response.addHeader("X-JWT-Filter", "hit");`.
  - [x] Cambiar el código de error `401 UNAUTHORIZED` a `417 EXPECTATION_FAILED` en `sendUnauthorizedError()`.
  - [x] Hacer el filtro "Stateless" eliminando la verificación del Refresh Token contra la BD.
- [x] `ResponseHelper.java` & `DataHelper.java`: Estandarizar estructuras de respuesta de usuario con o sin tokens (`buildUserAuthData`, etc.).
- [x] `UserDetailsServiceImpl.java`:
  - [x] Usar `isAccountConfirmed` en vez de chequear si `refreshToken` está vacío.
  - [x] Establecer `isAccountConfirmed = false` y `tokenRevocationDate = now()` en `createUnconfirmedUser`.
  - [x] Establecer `isAccountConfirmed = true` al confirmar la cuenta.
  - [x] Evitar intentos de vaciar `refreshToken` en Logout o DisableAccount.
  - [x] Auto-login con generación de tokens tras éxito en `confirmPasswordReset`.
- [x] `agent/business_rules/security_auth.md`: Actualizar contexto reflejando las nuevas reglas de negocio (Stateless, 417, isAccountConfirmed).
