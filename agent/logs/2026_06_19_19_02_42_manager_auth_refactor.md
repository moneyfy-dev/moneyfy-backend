# Refactorización de Autenticación de Administradores

## Resumen
Se extrajo la lógica de identidad del usuario administrador (root) separándola del modelo convencional de los corredores/clientes. Se estableció un flujo de autenticación exclusivo para gerentes/administradores de la plataforma.

## Cambios Realizados
- **Modelos**: Se crearon `ManagerModel` y `ManagerDto` para representar de forma ligera la identidad del usuario administrador.
- **Repositorio**: Se añadió el `ManagerRepository` (`ObjectId`).
- **Autenticación**: Se introdujeron `ManagerAuthService` y `ManagerAuthController` para proveer el endpoint de inicio de sesión (`POST /manager/auth/log-in`), el cual valida credenciales a través de `AuthModel` y devuelve los tokens junto con la estructura de datos empaquetada en el `ManagerDto`.
- **Seeder**: Se actualizó `RunAdminUserSeeder` para almacenar a `ManagerModel` de acuerdo a las credenciales configuradas en `own-env.properties`, inyectándolas con `@Value`.
- **Variables de Entorno**: Adición de `moneyfy.admin.email`, `moneyfy.admin.password`, `moneyfy.admin.name`, `moneyfy.admin.surname` en el `own-env.properties`.
