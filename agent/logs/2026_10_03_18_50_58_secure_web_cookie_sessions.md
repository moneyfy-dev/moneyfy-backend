# Sesión web con cookies HttpOnly y protección CSRF

El usuario autorizó implementar la seguridad de sesión web y, posteriormente, crear la rama separada e integrar/publicar hasta master para probar el frontend local en QA. Esta autorización sustituye para esta entrega las restricciones de rama eliu y de no integración automática; no se modifica permanentemente el flujo del repositorio.

## Cambios

- Modo web explícito por encabezado o cookies presentes, sin alternativa Bearer para ese modo.
- Cookies __Host-MoneyfySession, __Host-MoneyfyRefresh y __Host-MoneyfyCsrf con Secure, HttpOnly, SameSite=Strict, Path=/, sin Domain.
- CSRF Spring Security para operaciones web, incluidos login, registro, confirmación, recuperación y logout. Rechazo antes del controlador, con código WEB_CSRF_INVALID.
- GET /auth/web/csrf y GET /auth/web/session; la sesión se comprueba mediante JWT y revocación antes de hidratar usuario.
- Respuestas web sin JWT en cuerpo ni encabezados legibles; rotación por Set-Cookie y borrado de cookies al cerrar o invalidar sesión. Acceso y refresh deben corresponder al mismo usuario.
- Lista exacta de orígenes web mediante moneyfy.web.allowed-origins / MONEYFY_WEB_ALLOWED_ORIGINS; no hereda la confianza en todos los subdominios del CORS legado.
- Contrato Bearer móvil/admin conservado. No se implementan 2FA ni notificaciones, ni se modifican reglas de negocio, usuarios o base de datos.
- El workflow existente ejecuta las pruebas aisladas de seguridad con una clave efímera antes de publicar el WAR.

## Validación y coordinación

- Backend: 33 pruebas aprobadas (14 CORS + 19 WebSessionSecurityTest), WAR generado. Filtros/CSRF/JWT reales; servicios/repositorios aislados, sin MongoDB ni cuentas creadas.
- Web local: 86 pruebas unitarias/integración, 40 E2E Edge escritorio/móvil y build aprobados. Incluye Set-Cookie real en loopback, logout offline, dos pestañas, ausencia de tokens JavaScript y PIN derivado con sal.
- Revisión independiente e integración corrigieron cierre pendiente, PIN entre cuentas y exclusión de login hasta completar el estado local.
- El frontend se mantiene en v5/webapp, fuera de este repositorio backend. Requiere proxy del mismo origen y publicar este backend antes de usar cuentas reales.
- Incluye el commit CORS previo 3087938 por ascendencia; una única integración en master puede publicar ambos cambios.

La compilación y los fixtures no prueban despliegue ni credenciales reales. La publicación debe verificarse con Actions y comprobaciones HTTP sin mutaciones de negocio.
