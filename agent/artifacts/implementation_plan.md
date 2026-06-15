# Refinamiento del Flujo de Autenticación y Estandarización de Respuestas

Este plan aborda las optimizaciones y correcciones señaladas en la revisión de la refactorización previa de seguridad. El objetivo principal es hacer el sistema verdaderamente "stateless" (eliminando el almacenamiento en BD del Refresh Token), introducir un campo explícito para confirmar la cuenta, y unificar el formato de respuesta de la API.

## Cambios Propuestos

### 1. `AuthModel` (Colección `auth_credentials`)
- **[MODIFICAR]** `src/main/java/com/referidos/app/segurosref/models/AuthModel.java`
  - Añadir el campo `boolean isAccountConfirmed;`.
  - Eliminar el campo `String refreshToken;`.

### 2. Filtro de Validación JWT
- **[MODIFICAR]** `src/main/java/com/referidos/app/segurosref/configs/filters/JwtValidationFilter.java`
  - Eliminar la verificación contra la base de datos de que el refresh token ingresado sea igual al almacenado en `AuthModel`.
  - Validar matemáticamente el Refresh Token y confiar exclusivamente en la validación contra `tokenRevocationDate` usando su claim `iat` (Issued At). Si fue emitido antes de la revocación, el token es inválido. Esto hace el sistema verdaderamente sin estado.
  - Al realizar el *Sliding Session* (renovación de Refresh Token), ya no se guardará el nuevo token en la BD, simplemente se entregará en la cabecera `X-New-Refresh-Token`.

### 3. Servicios de Autenticación (`UserDetailsServiceImpl`)
- **[MODIFICAR]** `src/main/java/com/referidos/app/segurosref/services/UserDetailsServiceImpl.java`
  - Reemplazar todas las verificaciones confusas (`getRefreshToken() != null && !isEmpty()`) por el nuevo chequeo semántico `authDB.isAccountConfirmed()`.
  - **`createUnconfirmedUser`:** Iniciar `isAccountConfirmed = false` y setear el `tokenRevocationDate = LocalDateTime.now()` desde el principio (ya que no afectará negativamente).
  - **`confirmRegistration`:** Cambiar estado a `isAccountConfirmed = true`.
  - **`logout` / `disableAccount`:** Al cerrar sesión o inhabilitar, ya no intentaremos "vaciar" el refresh token. Con simplemente actualizar el `tokenRevocationDate` a `LocalDateTime.now()`, cualquier Refresh Token en posesión del cliente queda instantáneamente revocado.
  - **`confirmPasswordReset`:** Una vez que se verifique el código y se actualice la contraseña exitosamente, **se realizará un auto-login**. Se emitirán el Session Token y Refresh Token nuevos, y se retornarán en la respuesta estándar para que el usuario ingrese directamente sin tener que volver a hacer Login explícito.

### 4. Estandarización de Respuestas (`ResponseHelper` y `DataHelper`)
- **[MODIFICAR]** `src/main/java/com/referidos/app/segurosref/helpers/DataHelper.java`
- **[MODIFICAR]** `src/main/java/com/referidos/app/segurosref/helpers/ResponseHelper.java`
  - Se estandarizará un formato único en `DataHelper` para las respuestas que involucren al usuario. Por ejemplo, `buildUserAuthData(user, session, refresh)` para flujos de Login/Registro, y `buildUserData(user)` para retornos simples.
  - Ambos formatos garantizarán que el frontend reciba exactamente la misma estructura base para mapear el estado, sin importar si el código HTTP es 200 (OK) o 201 (CREATED).

### 5. Contexto del Agente
- **[MODIFICAR]** `agent/business_rules/security_auth.md`
  - Actualizar el documento de reglas de negocio para plasmar el nuevo modelo "Stateless" sin Refresh Tokens en la base de datos y la inclusión de `isAccountConfirmed`.

## > [!IMPORTANT]
## Open Questions / Feedback Required

1. **Auto-Login tras `confirmPasswordReset`**: Se propone regresar la data del usuario junto con los Tokens generados para que el frontend lo loguee automáticamente. ¿Estás de acuerdo con este enfoque o prefieres forzar al usuario a ir a la pantalla de Login e ingresar la nueva contraseña manualmente?
2. **Validación JWT sin almacenamiento**: Al no almacenar el Refresh Token, si el usuario hace "logout", su `tokenRevocationDate` se actualiza. Esto invalida su token en **todos sus dispositivos simultáneamente**. ¿Es aceptable este comportamiento de "Cierre de sesión global" en todos los dispositivos por seguridad, o existía un motivo para intentar rastrear el refresh token individual? (Actualmente `AuthModel` tampoco soportaba múltiples dispositivos a la vez por tener solo un campo `refreshToken`).
