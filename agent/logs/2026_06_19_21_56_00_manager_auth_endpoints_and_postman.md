# Registro de Actualización: Creación de Endpoints para Manager Authentication y Ajuste de Colecciones Postman

## Resumen de Cambios
Se implementó el flujo completo de autenticación y gestión de usuarios administradores en la plataforma Moneyfy, incluyendo los servicios, controladores, seguridad y actualización de herramientas de consumo (Postman).

## Flujos y Archivos Alterados
1. **Controladores y Servicios**:
   - Creación de `ManagerAuthController` para exponer endpoints: `/log-in`, `/logout`, `/create`, `/restore/password`, `/confirm/password/reset`, `/resend/code`.
   - Implementación de la lógica en `ManagerAuthServiceImpl` manejando integraciones con MongoDB (`AuthRepository`, `ManagerRepository`), generación de tokens (JWT), codificación de contraseñas y notificación por correo.
   - Refactorización de duplicidad de código moviendo `generateRandomCode()` a `DataHelper.java`.

2. **Seguridad y Enrutamiento**:
   - Centralización de rutas públicas en `FilterHelper.PUBLIC_ROUTES` y actualización de `SecurityConfig` para usar dicho array de forma dinámica usando `AntPathMatcher` dentro de `JwtValidationFilter`.
   - Corrección de la coincidencia de patrones para evitar exclusión inadvertida de autorización en los métodos de administración; garantizando que los roles `ROLE_ADMIN` sean evaluados correctamente en métodos protegidos como `/create` y `/logout`.
   - Implementación de `ResponseHelper.badRequest()` y `ResponseHelper.notFound()`.

3. **Colecciones y Postman**:
   - Centralización de variables de entorno (Dev y Prod) directamente hacia las Colecciones respectivas de Postman, y eliminación de los archivos `.postman_environment.json`.
   - Actualización de los scripts de `Tests` (Tests Scripts) en la colección para recuperar `sessionToken` y asignarlos de manera dinámica al entorno interno de la colección vía `pm.collectionVariables.set`.
   - Inserción de cabeceras de `X-New-Refresh-Token` para autenticación de peticiones administrativas y recarga automática de sesión.
