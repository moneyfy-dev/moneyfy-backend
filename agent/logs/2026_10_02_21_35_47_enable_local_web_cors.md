# Acceso web local y rotación de sesión por CORS

Solicitud: habilitar pruebas de la migración React local contra la API existente y subir el ajuste del backend.

## Diagnóstico y cambio

- La API real rechazaba con 403 `Invalid CORS request` los orígenes `http://localhost:5173` y `http://127.0.0.1:5173` antes de validar credenciales.
- `SecurityConfig` incorpora ambos orígenes exactos. Se preservan los patrones existentes de producción, LAN y Expo.
- Se exponen `X-New-Session-Token` y `X-New-Refresh-Token` para que los clientes web, incluido admin, puedan persistir la rotación.
- No se alteran roles, endpoints, JWT, contraseñas, base de datos ni reglas de negocio.

## Validación

- Prueba inicial: 13 casos, 5 fallos que reproducían el rechazo local y la ausencia de exposición de encabezados.
- Resultado final: `mvnw.cmd -B -ntp -Dtest=CorsConfigurationTest package`, Java 21, BUILD SUCCESS, 14 pruebas aprobadas y WAR generado.
- Casos: preflight local/producción, rechazo de orígenes ajenos y puertos no configurados, POST permitido/rechazado, encabezados de rotación y rechazo JWT 417 sin sesión en una ruta privada.
- Pruebas aisladas sin iniciar Spring ni conectar MongoDB; no se crean usuarios ni se ejecutan transacciones reales.
- Revisión independiente de agente completada. Se conserva CRLF del archivo original; diff verificado con `core.whitespace=cr-at-eol`.

## Publicación y límites

El workflow despliega únicamente al actualizar `master`. El commit en `eliu` queda para revisión; no implica despliegue. El proxy Vite del proyecto web también fue corregido para alinear Origin y Host solo para los dos orígenes locales, permitiendo pruebas locales mientras se publica el backend.

La revisión confirmó que el backend original no implementa 2FA ni `/users/notification-settings`; son trabajos distintos. PIN/WebAuthn del cliente no constituyen segundo factor del servidor. No se considera validado el acceso con una cuenta real por estas pruebas.
