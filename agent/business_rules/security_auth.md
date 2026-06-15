# Security and Authentication Architecture

Este documento describe la estructura y el comportamiento de la capa de seguridad y autenticación de la API.

## Modelo de Autenticación (`AuthModel`)
La información crítica de seguridad (contraseña, roles, tokens, y códigos de verificación) se maneja de forma completamente aislada de la información personal del usuario (`UserModel`).
Toda validación de acceso interactúa exclusivamente con `AuthRepository` a través de `AuthModel`.

## Flujo de Tokens y Sliding Session
La API utiliza un esquema de tokens dual (Session y Refresh Token):
1. **Session Token (1 Hora)**: Utilizado para autorizar las solicitudes a la API. Sus claims contienen la información básica de autorización (`email`, `authorities`, `iat`).
2. **Refresh Token (8 Horas)**: Utilizado internamente para el proceso de **Silent Refresh**.
3. **Sliding Session**: Si el usuario realiza una petición con un Session Token vencido, pero su Refresh Token es válido, la API emite un nuevo Session Token en las cabeceras (`X-New-Session-Token`).
   Además, si al Refresh Token le resta un 50% o menos de su vida útil (≤ 4 horas), la API emitirá también un nuevo Refresh Token (`X-New-Refresh-Token`).
   **Regla para Frontends**: Los clientes deben interceptar estas cabeceras para actualizar su almacenamiento local de manera transparente para el usuario.

## Validación Stateless y Confirmación de Cuenta
- El sistema es **100% Stateless**. La base de datos no almacena el Refresh Token. Se confía estrictamente en la validación criptográfica del JWT y el chequeo matemático contra el campo `tokenRevocationDate`. Si el claim `iat` (Issued At) es anterior a la revocación, el token se considera comprometido.
- La confirmación de si un usuario terminó su registro de manera exitosa se maneja mediante un campo bandera explícito `isAccountConfirmed` dentro de `AuthModel`.
- Al realizar un "Logout" explícito en `/auth/logout` o deshabilitar una cuenta, se actualiza el campo `tokenRevocationDate` en `AuthModel` con `LocalDateTime.now()`, forzando el rechazo instantáneo en el siguiente request en todos los dispositivos de ese usuario.

## Gestión de Errores (HTTP 417)
Cualquier fallo de validación de token a nivel de filtros (expirado, inválido, revocado, etc.) se estandariza con una respuesta `HTTP 417 Expectation Failed` mapeada al objeto `ErrorResponse<T>` de la aplicación.
- **Código Interno**: `3` (proveniente de `BusinessCodeEnum.APP_TOKEN_INVALID_OR_EXPIRED`).
- **Data**: `null`.
- Este estándar reemplaza el envío de texto plano y garantiza uniformidad en la comunicación frontend-backend.

## Rutas Públicas
Las rutas públicas que no requieren validación JWT se excluyen explícitamente en `JwtValidationFilter` utilizando un `AntPathMatcher` estandarizado (ej. `/auth/**`, `/swagger-ui/**`, etc.). El filtro omite la inspección del token para estos endpoints.
