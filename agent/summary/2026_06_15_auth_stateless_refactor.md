# Resumen de Cambios: Refactorización a Stateless y Solución de Bugs Críticos
**Fecha:** 2026-06-15

## 1. Migración a JWT Completamente Stateless
- **Modelos**: Se eliminó la persistencia del `refreshToken` en la base de datos (colección `auth_credentials`). La validación del Refresh Token ahora es matemática y asíncrona, comparando su claim de emisión (`iat`) con la fecha global de invalidación (`tokenRevocationDate`).
- **Estados de Cuenta**: Se incorporó el campo booleano explícito `isAccountConfirmed` en `AuthModel` (colección `auth_credentials`) que actúa como bandera para saber si un usuario culminó el proceso de registro, reemplazando la dependencia ambigua que se tenía de comprobar si el "refreshToken estaba vacío o era nulo".

## 2. Correcciones en el Filtro de Seguridad (`JwtValidationFilter.java`)
- **Bloqueo en Producción**: Se modificó `request.getRequestURI()` por `request.getServletPath()` durante el chequeo de las rutas públicas. Esto corrige el error crítico en producción donde endpoints como `/moneyfy/auth/log-in` eran bloqueados debido a la falta de coincidencia con el context-path de despliegue.
- **Códigos de Error**: Se estandarizó el código HTTP de respuesta para errores de validación de tokens o tokens expirados/modificados. Se pasó de `HTTP 401 (Unauthorized)` a `HTTP 417 (Expectation Failed)`, cumpliendo con la regla de negocio para evitar confusiones con otras protecciones generales de Cloudflare/Nginx y estandarizar la captura de este evento en el frontend.
- **Limpieza**: Se eliminó la cabecera intrusiva en respuestas HTTP: `X-JWT-Filter: hit`.

## 3. Experiencia de Usuario y Respuestas (`UserDetailsServiceImpl.java` & `DataHelper.java`)
- **Estandarización**: Se crearon métodos base en `DataHelper` (`buildUserData` y `buildUserAuthData`) para generar una estructura estándar y consistente en los JSON de respuestas (`{"user": {...}, "sessionToken": "...", "refreshToken": "..."}`) sin generar objetos anidados por error.
- **Auto-Login**: Se modificó el flujo de "Confirmación de Restablecimiento de Contraseña". Ahora, después de cambiar la contraseña exitosamente, se devuelve instantáneamente un set de tokens válidos (`sessionToken` y `refreshToken`) aplicando Auto-Login, mejorando notablemente la experiencia de usuario.
- **Unificación de Validaciones**: Todos los métodos que dependen del estado del usuario (Listar referidos, Restablecer clave, Deshabilitar, Re-envío de códigos) ahora utilizan la propiedad atómica `authDB.isAccountConfirmed()` para sus comprobaciones de flujo en reemplazo de lógicas arcaicas sobre el RefreshToken.

## 4. Contexto del Agente
- Se actualizaron las reglas de negocio en `agent/business_rules/security_auth.md` para asentar el nuevo comportamiento del sistema (Modelo Stateless y Código HTTP 417).
