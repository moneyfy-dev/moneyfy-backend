# Actualización de ruta raíz pública a /home

## Resumen
Se modificó el endpoint de validación y estado del servidor de la ruta raíz `/` a `/home` para evitar la exposición directa del proyecto en la ruta principal. Este cambio involucró actualizaciones en controladores, filtros de seguridad y colecciones de Postman para garantizar que las peticiones se dirijan correctamente.

## Cambios realizados
- **HomeController**: El mapeo de `@GetMapping(value = "/")` se actualizó a `@GetMapping(value = "/home")`.
- **SecurityConfig**: Se reemplazó el `securityMatcher` de `/` a `/home` en la lista de rutas públicas, con el fin de evitar que el filtro exija tokens para acceder.
- **JwtValidationFilter**: Se agregó `/home` al listado de `PUBLIC_ROUTES` asegurando sincronización con `SecurityConfig` para omitir la inspección del encabezado.
- **Postman Collections (DEV y PROD)**: Se actualizó el endpoint base utilizado en la petición "API available", reemplazando la ruta a `{{Dev - URL}}/home` y `{{Prod - URL}}/home`.
