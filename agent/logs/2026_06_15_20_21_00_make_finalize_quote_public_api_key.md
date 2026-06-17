# Requerimiento: Hacer público el endpoint finalizeQuote

## Resumen
El usuario solicitó que el endpoint de finalización de cotizaciones (`/quoter/finalize/quote`) temporalmente quede como público, saltándose la seguridad por token de sesión (JWT) y el rol de `ADMIN`. En su lugar, el endpoint ahora usa la clave global de API para autorizar el proceso (`X-Moneyfy-Api-Key`). 

## Cambios Realizados
1. **QuoterController**: Se ajustó la anotación de seguridad a `@PreAuthorize("permitAll()")` y se reconfiguró la especificación Swagger para eliminar `Refresh-Token` y requerir `X-Moneyfy-Api-Key`.
2. **QuoterService / QuoterServiceImpl**: Se actualizó la firma y la lógica del método `finalizeQuote`. Ahora toma `HttpServletRequest` directamente y valida la cabecera haciendo uso de `ValidateInputHelper.checkApiKeyMF` en contra de la propiedad `moneyfy.api-key`.
3. **Filtros de Seguridad**: Se añadió la ruta `/quoter/finalize/quote` al listado de `PUBLIC_ROUTES` en `JwtValidationFilter.java` y al `securityMatcher` en `SecurityConfig.java`.
4. **Colecciones Postman**: Se modificaron las peticiones correspondientes a `finalizeQuote` tanto en la colección `MoneyFy_Dev_API` como en `MoneyFy_Prod_API`, actualizando la llave y el nombre de variable en el encabezado.
