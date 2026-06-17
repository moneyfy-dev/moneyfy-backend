# Comando: $/postman

**Atajos**: `$/col`, `$/post`
**Skill Asignada**: [Skill 5: Postman Collection Manager](../skills/postman_manager.md)

## Propósito
Sincroniza y alinea la colección de pruebas de Postman y su entorno (ubicados en `agent/postman/`) para que reflejen con precisión el estado actual del código de la API. Elimina flujos obsoletos y agrega las rutas nuevas, inyectando scripts y variables necesarias (como captura automática de JWTs).

## Criterios de Ejecución
1. El agente debe utilizar Python para leer y manipular en bloque los archivos `.json` de la colección de Postman, protegiendo su esquema estructural.
2. Extraerá las rutas base de los `Controllers` (ej. `@RequestMapping("/api/v1/manager")`) y las cotejará contra la estructura de Postman.
3. Se asegurará de inyectar en el entorno (`agent/postman/env/`) los credenciales estándar de prueba (`Testing_123`), el correo (`eliu.practicaltech@gmail.com`), y `moneyfy.api-key`.
4. En los flujos dinámicos que devuelven tokens de sesión (como el endpoint `login`, `confirm registration`, o `confirm password reset`), se validará e inyectará el script en la fase `test` que mapee `sessionToken` y `refreshToken` a las variables de entorno de Postman correspondientes de forma aislada. Asegúrate de usar SIEMPRE el prefijo respectivo (ej. `Dev - Session Token` y `Dev - Refresh Token` para la colección de Dev, y `Prod - ...` para la de Prod).
