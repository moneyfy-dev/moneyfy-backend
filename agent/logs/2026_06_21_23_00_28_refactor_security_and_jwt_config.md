# Registro de Actualización: Refactorización de Seguridad, Configuración JWT y Zona Horaria

**Fecha:** 21 de Junio de 2026
**Responsable:** Agente / User (Got)

## Resumen de Cambios

1. **Unificación de Cadenas de Seguridad (`SecurityConfig`)**:
   - Se unificaron `publicFilterChain` y `apiFilterChain` en un solo `filterChain` más limpio, utilizando `requestMatchers(FilterHelper.PUBLIC_ROUTES).permitAll()`.
   - Se eliminó el filtro global redundante de CORS (`corsFilter()`), manteniendo únicamente la configuración interna gestionada por Spring Security.

2. **Mejoras en el Filtro JWT (`JwtValidationFilter`)**:
   - Se delegó la validación de las rutas públicas directamente a Spring sobrescribiendo el método nativo `shouldNotFilter(HttpServletRequest request)`.
   - Se corrigió un bug potencial en la renovación de tokens: ahora se utiliza `AuthorityUtils.commaSeparatedStringToAuthorityList()` para parsear correctamente roles múltiples separados por comas.

3. **Fortalecimiento y Simplificación de `JwtConfig`**:
   - Se reemplazó el `LOCAL_DEV_SECRET` duro por una solución basada completamente en `System.getenv("MONEYFY_JWT_SECRET")` con un valor de respaldo nativo para el entorno local, aplicando el principio *Fail-Fast* y mejorando la seguridad.
   - Se simplificó la creación del *Refresh Token* para que utilice siempre el email del usuario como `subject`, eliminando el claim redundante `"user"`.

4. **Ajuste de Zona Horaria Global (`SegurosrefApplication`)**:
   - Se implementó un `@PostConstruct` para establecer la zona horaria por defecto de toda la JVM en `America/Santiago`, asegurando que todas las fechas en MongoDB y procesos internos cuadren con la hora local de Chile.

## Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/SegurosrefApplication.java`
- `src/main/java/com/referidos/app/segurosref/configs/SecurityConfig.java`
- `src/main/java/com/referidos/app/segurosref/configs/JwtConfig.java`
- `src/main/java/com/referidos/app/segurosref/configs/filters/JwtValidationFilter.java`
- `src/main/resources/properties/own-env.example.properties`
