# Log: Refactorización a JWT Stateless y Correcciones Críticas en Producción
**Fecha y Hora:** 2026-06-15 22:23
**Rama:** eliu
**Commit Hash (referencia):** be4d916

## 1. Descripción del Avance
Se implementó una reestructuración de la lógica de autenticación para que sea completamente "Stateless" (sin estado persistente del Refresh Token en base de datos). A su vez, se corrigieron fallos de bloqueo de seguridad en rutas públicas experimentados en el entorno de producción y se estandarizaron las estructuras de respuesta.

## 2. Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/configs/filters/JwtValidationFilter.java`
- `src/main/java/com/referidos/app/segurosref/controllers/AuthController.java`
- `src/main/java/com/referidos/app/segurosref/helpers/DataHelper.java`
- `src/main/java/com/referidos/app/segurosref/models/AuthModel.java`
- `src/main/java/com/referidos/app/segurosref/services/UserDetailsServiceImpl.java`
- `src/main/java/com/referidos/app/segurosref/services/UserServiceImpl.java`
- `agent/business_rules/security_auth.md`

## 3. Resumen de Cambios
- **Modelo de Autenticación (`AuthModel`)**: Se eliminó `refreshToken` de la BD y se reemplazó por el campo booleano `isAccountConfirmed` para gestionar el estado de los nuevos registros. La revocación ahora depende única y exclusivamente de `tokenRevocationDate`.
- **Rutas Públicas**: Se modificó `request.getRequestURI()` por `request.getServletPath()` en `JwtValidationFilter` para garantizar compatibilidad con despliegues que empleen context paths distintos a la raíz (`/moneyfy`).
- **Errores de Token**: El código de error `401 Unauthorized` lanzado por tokens expirados o reválidos fue cambiado por `417 Expectation Failed` siguiendo las nuevas directrices.
- **Estandarización de Datos**: Se implementaron métodos como `buildUserAuthData` para devolver estructuras estandarizadas al FrontEnd con o sin tokens y se implementó un Auto-Login seguro luego del flujo "Restablecer Contraseña".

## 4. Estado Actual
El código se encuentra subido y establece una base sólida y eficiente para las futuras comunicaciones con la aplicación frontend, permitiendo la generación matemática y segura de nuevos tokens en las cabeceras (Sliding Session).
