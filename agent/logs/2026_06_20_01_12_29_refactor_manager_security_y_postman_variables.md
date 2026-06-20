# Registro de cambios: Refactorización de Seguridad para Administradores y Ajuste en Postman

## Resumen
Se implementó un refactor completo a la seguridad en los módulos administrativos de la API, deprecando el uso de una API key estática en favor de tokens dinámicos con validación de roles de sistema (ADMIN).

## Cambios Realizados
1. **Supresión de API Key**: Se eliminó la validación estática de `X-Moneyfy-Api-Key` en `SecurityConfig`, `FilterHelper` y en todas las declaraciones de endpoints públicos relacionados.
2. **Restricciones de Rol y Autorización**: Se añadió `@PreAuthorize("hasRole('ADMIN')")` en todos los endpoints de `LogController`, `SeedController`, `TransactionController` y `ManagerController`.
3. **Manejo de Contexto**: Se intercepta al administrador en los servicios mediante el `SecurityContextHolder` usando el JWT proveniente de la petición.
4. **Respuesta Estándar**: Se integró el modelo de `ManagerDto` en las respuestas 200 de los flujos de administradores. Se generó además el método `ResponseHelper.unauthorized()` para rechazar tokens de administradores desactivados o no encontrados en Base de Datos.
5. **Limpieza de Código**: Se removieron por completo las dependencias de `HttpServletRequest` en los controladores administrativos que ya no requerían de él.
6. **Ajuste en Postman**: Se actualizaron y limpiaron las colecciones Postman de `dev` y `prod` inyectando automáticamente la seguridad Bearer (`manager_session_token`) y las cabeceras de `X-New-Refresh-Token` (`manager_refresh_token`).
