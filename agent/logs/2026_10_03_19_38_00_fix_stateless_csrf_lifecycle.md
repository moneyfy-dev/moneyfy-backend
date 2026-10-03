# Correccion de CSRF eliminado despues de autenticar JWT

## Evidencia

QA reporto 403 WEB_CSRF_INVALID en hidratacion, ingresos, referidos y busqueda de vehiculo. Una reproduccion aislada con Spring Security, Tomcat y navegador confirmo la secuencia: login emite tres cookies; el primer POST autenticado responde 200 pero elimina CSRF con Max-Age=0; el siguiente POST recibe 403. La causa es CsrfAuthenticationStrategy ejecutada durante cada autenticacion JWT sin estado, no una integracion de aseguradoras.

## Cambio

SecurityConfig configura NullAuthenticatedSessionStrategy exclusivamente para CsrfConfigurer. El filtro, comparacion cookie/encabezado, lista de origenes y atributos Secure/HttpOnly/SameSite siguen activos. La rotacion de CSRF permanece explicita al emitir credenciales en login, confirmacion y recuperacion; logout sigue borrando cookies. No se alteran reglas de negocio, cuentas, MongoDB ni el contrato movil/admin.

## Validacion

Nueva regresion construye un cookie jar desde Set-Cookie, conserva la cookie en tres POST autenticados consecutivos y comprueba restauracion de sesion y otro POST. Fallo antes del cambio y pasa despues. Suite de 34 pruebas aisladas (20 sesion + 14 CORS) y WAR aprobados, sin MongoDB ni cuentas reales.

Frontend local: fondos sin recortes de seccion, foco sobre contorno redondeado completo, selects nativos con flecha SVG y avion Lottie verde. Build, 87 pruebas web, 40 E2E y paridad de 32 rutas/18 assets aprobados. Estos archivos viven fuera del repositorio backend.

## Publicacion

Esta correccion completa la entrega de seguridad QA anterior. Se mantiene la autorizacion expresa del usuario para subir e integrar hasta master; se conserva el flujo de trabajo en eliu y se integra por PR. No se guardaron ni utilizaron las credenciales pegadas por el usuario. El resultado del despliegue se verificara en GitHub Actions y con comprobaciones anonimas de HTTP.
