# Log: Estandarización de Cabecera X-Moneyfy-Api-Key y Sincronización de Rutas Públicas
**Fecha y Hora:** 2026-06-15 22:51
**Rama:** eliu
**Commit Hash (referencia):** cc8be73

## 1. Descripción del Avance
Se homologaron todas las cabeceras relacionadas con la API interna de MoneyFy a un estándar único (`X-Moneyfy-Api-Key`), además de actualizar las reglas del filtro JWT para omitir explícitamente estas rutas públicas validadas por API Key y prevenir bloqueos indebidos de Spring Security.

## 2. Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/configs/filters/JwtValidationFilter.java`
- `src/main/java/com/referidos/app/segurosref/controllers/TransactionController.java`
- `src/main/java/com/referidos/app/segurosref/controllers/SeedController.java`
- `src/main/java/com/referidos/app/segurosref/controllers/LogController.java`
- `src/main/java/com/referidos/app/segurosref/services/TransactionServiceImpl.java`
- `src/main/java/com/referidos/app/segurosref/services/SeedServiceImpl.java`
- `src/main/java/com/referidos/app/segurosref/services/LogServiceImpl.java`

## 3. Resumen de Cambios
- **JwtValidationFilter**: Se agregaron las rutas `/seed/**`, `/log/**`, `/transaction/**`, `/quoter/commission/**` y `/api/v1/manager/**` al arreglo `PUBLIC_ROUTES` para mantener sincronía con el `SecurityConfig`.
- **Controllers & Swagger**: Se actualizaron las anotaciones `@Parameter` en la documentación de Swagger para usar la clave de header correcta `X-Moneyfy-Api-Key` en vez de `Api-Key-MoneyFy`.
- **Services**: Se actualizó el método de validación `ValidateInputHelper.checkApiKeyMF` para obtener la variable global leyendo la cabecera correcta usando `request.getHeader("X-Moneyfy-Api-Key")`.

## 4. Estado Actual
Los ajustes están disponibles en la rama y superan todos los procesos de validación y compilación. Todo el sistema consume consistentemente el mismo encabezado para los servicios no autenticados por JWT.
