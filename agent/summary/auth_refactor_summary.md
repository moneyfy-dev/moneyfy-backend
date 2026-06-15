# Resumen de Refactorización de Autenticación y Seguridad

Este documento resume todos los cambios realizados durante la refactorización profunda del módulo de autenticación, la transición al nuevo esquema `AuthModel` y las incidencias de compilación superadas durante la integración.

## 1. Desacoplamiento y Centralización (`AuthModel`)
- **Creación de `AuthModel`**: Se extrajo toda la información sensible y el estado de la sesión (contraseña, roles, tokens, fechas de caducidad) desde el antiguo `UserDataModel` hacia la nueva colección `auth_credentials` representada por `AuthModel`.
- **Limpieza en Modelos**: `UserDataModel` ahora únicamente alberga información personal del usuario, haciendo el diseño mucho más limpio y reduciendo el riesgo de exposición de credenciales en respuestas de la API.

## 2. Eliminación de Dispositivos (`DeviceModel`)
- Se erradicó por completo el `DeviceModel`, su respectivo repositorio `DeviceRepository` y toda lógica acoplada a la cabecera `User-Agent`.
- Se eliminó el `DeviceValidationFilter` del flujo de seguridad (`SecurityConfig.java`), lo cual aligeró la carga por petición y redujo código obsoleto sin aportar riesgos de seguridad.

## 3. Reestructuración del `JwtValidationFilter`
- **Gestión Centralizada**: El filtro ahora inyecta `AuthRepository` e implementa la validación directa usando `AuthModel`.
- **Rutas Públicas**: Se incorporó un `AntPathMatcher` nativo de Spring para evadir limpiamente el chequeo del token en endpoints que no lo requieren (ej. `/auth/register`, `/auth/log-in`, etc.).
- **Silent Refresh y Sliding Session**:
  - Si el `Session Token` caduca (1 hora de vida), pero el `Refresh Token` sigue vigente (8 horas de vida), el filtro regenera e inyecta un nuevo `Session Token` (cabecera `X-New-Session-Token`).
  - Si el `Refresh Token` se encuentra dentro del `REFRESH_THRESHOLD` (50% de caducidad, es decir 4 horas), también se emite uno nuevo (cabecera `X-New-Refresh-Token`).
- **Revocación Global**: Implementación de la validación contra `tokenRevocationDate`. Ahora, hacer *logout* invalida todos los tokens emitidos previamente para ese usuario en todos los dispositivos simultáneamente.

## 4. Ajustes Críticos y Solución de Errores de Compilación
Durante la refactorización a nivel de servicios, la compilación de Maven levantó alertas y fallos al estar múltiples lógicas interconectadas al modelo viejo. Los cambios imprevistos necesarios fueron:

- **Ajustes en `UserServiceImpl.java`**: 
  - La función de cambio de contraseñas (`changePassword`) antes accedía a `userData.getPwd()`. Ahora inyecta y accede a `AuthRepository` para manejar y guardar la nueva contraseña directamente en `AuthModel`.
  - Lo mismo ocurrió al listar referidos (`listReferreds`); ahora verifica el estado del registro a través de `authModel.getRefreshToken()`.
- **Ambigüedad en sobrecargas (`ResponseHelper`)**:
  - El compilador de Java 21 falló con las invocaciones `ResponseHelper.ok(..., null)` y `ResponseHelper.failedDependency(..., null)` en el servicio. Al haber diferentes métodos sobrecargados recibiendo `Map<String, Object>` o clases personalizadas como `QuotationDto`, pasar `null` directo arrojaba "referencia ambigua". Se solucionó realizando *castings* explícitos como `(Map<String, Object>) null`.
- **Correcciones en Constructores y Métodos Huérfanos**:
  - Actualizamos el constructor temporal de `UserDataModel` tras quitar las columnas del token.
  - El ayudante `DataHelper.java` debió prescindir del retorno de `TokensDto` debido a que ahora la emisión de tokens recae de forma independiente y segura sobre la entidad `AuthModel`.
- **Inyecciones Desactualizadas (`SecurityConfig.java`)**:
  - Se modificó la inyección de repositorios en el Security Config para proveer `AuthRepository` al `JwtValidationFilter` en lugar del ya innecesario `UserRepository`.
- **Imports Obsoletos**: Un script de limpieza retiró eficientemente los `imports` innecesarios de archivos modificados que arrojaban advertencias (como `io.jsonwebtoken.security.SignatureException`, imports de `Swagger`, entre otros).

## 5. Estandarización de Errores
Todos los errores devueltos por `JwtValidationFilter` que suponen credenciales inválidas, tokens revocados o no provistos se canalizan mediante un `ErrorResponse` usando el enum estándar `APP_TOKEN_INVALID_OR_EXPIRED`, devolviendo al cliente un código identificador limpio (`code: 3`) en vez de una estructura inconsistente.

---
*Este documento consolida y registra las decisiones arquitectónicas para la refactorización del flujo de Autenticación de Moneyfy, asegurando el cumplimiento de los estándares y sirviendo como guía para el manejo del nuevo `AuthModel`.*
