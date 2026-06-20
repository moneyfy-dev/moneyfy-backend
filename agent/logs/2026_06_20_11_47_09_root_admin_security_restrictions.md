# Registro de Cambios: Restricciones de Seguridad para el Administrador Root Principal

## Resumen
Se implementaron validaciones restrictivas en los flujos de autenticación y gestión de usuarios (`ManagerAuthServiceImpl`) para diferenciar los privilegios del administrador "root" principal de los administradores secundarios.

## Cambios Realizados
1. **Inyección de Identidad**: Se agregó la inyección de la variable de entorno `moneyfy.admin.email` en el servicio `ManagerAuthServiceImpl` y se creó la función utilitaria `isRootAdmin(String email)` para validar contra el correo del administrador principal.
2. **Restricción de Creación (`/create`)**: Se intercepta la identidad de quien solicita la creación de un nuevo gerente (usando el `SecurityContextHolder`). Si el solicitante no es el administrador root, el sistema devuelve un BadRequest con HTTP 400 denegando el acceso. Sólo el root puede expandir el equipo administrativo.
3. **Restricción de Recuperación de Contraseña**:
   - En `/restore/password`, `/confirm/password/reset` y `/resend/code`.
   - Se valida el correo objetivo. Si coincide con el administrador root, el flujo se corta tempranamente retornando un BadRequest. Esto protege las credenciales primarias (manejadas desde el `own-env.properties`) de ser sobrescritas u olvidadas a través de los mecanismos regulares de base de datos.
4. **Respuesta Estandarizada**: Se definieron mensajes claros de error utilizando el método estático `ResponseHelper.badRequest()` para cada caso violado.
