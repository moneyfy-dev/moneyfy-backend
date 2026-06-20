# Registro de Avance: Soporte Multi-Rol (Admin/User) y Endpoints de Moneyfyers

## Resumen
Se implementó el soporte para que un mismo usuario pueda tener tanto el rol de administrador (`ROLE_ADMIN`) como el rol de aplicación (`ROLE_USER`) usando el mismo correo, separando los roles mediante comas en el campo `role` del modelo `AuthModel`. Además, se optimizó el endpoint para consultar el listado de Moneyfyers con paginación, filtros y agregaciones en MongoDB para evitar problemas de sobrecarga de memoria (OOM).

## Tareas Completadas

### Soporte Multi-Rol en `AuthModel` (Comas)
- Se actualizó `ManagerAuthServiceImpl.java` para que al registrar un administrador, si el correo ya existe, concatene `ROLE_ADMIN` a la cadena de roles existente en vez de arrojar error.
- Se actualizó `UserDetailsServiceImpl.java` y `JwtValidationFilter.java` para usar `AuthorityUtils.commaSeparatedStringToAuthorityList` al leer roles.
- Se modificó `RunAdminUserSeeder.java` para evitar que sobreescriba roles de usuarios y soporte de igual manera la concatenación segura del rol `ROLE_ADMIN` para la cuenta principal.
- No hubo necesidad de alterar el modelo de la DB, evitando el dolor de refactorización en el frontend.

### Endpoints y Lógica de Manager
- Se completó la limpieza de servicios moviendo las implementaciones al directorio `impl/`.
- Se creó el endpoint `GET /api/v1/manager/moneyfyers` dentro de `ManagerController.java` y `ManagerServiceImpl.java`.
- Se implementó una consulta MongoDB (`mongoTemplate.aggregate`) para calcular en base de datos las métricas como `cantidad_dinero_recaudado`, `cantidad_referidos_compraron`, etc., por usuario.

### Postman
- Se añadió la petición del endpoint `moneyfyers` a las colecciones de Dev y Prod (con su respectiva documentación).

## Consideraciones Futuras
- Con este cambio, si el día de mañana se agregan nuevos roles, el sistema seguirá funcionando bien leyendo la cadena de roles separada por comas sin romper lo existente.
- Se ha de asegurar que los clientes frontend no dependan de una validación estricta de string (como `role === "ROLE_USER"`) sino `role.includes("ROLE_USER")`.
