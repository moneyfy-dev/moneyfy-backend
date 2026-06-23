# Registro de Modificaciones - Corrección de Codificación en Validaciones

## Fecha y Hora
2026-06-23 00:03:51

## Descripción de los Cambios
Este registro consolida la corrección de errores de codificación ("mojibake") en el helper de validación de entradas.

### Modificaciones en `ValidateInputHelper.java`
- **Restauración de Caracteres Especiales:** Se reemplazaron todas las cadenas corruptas producto de un fallo de codificación UTF-8 (ej. `Ã¡Ã©ÃÃ³ÃºÃ±Ã§Ã½ÃÃ‰ÃÃ“ÃšÃ‘Ã‡Ã`) por sus equivalentes correctos del español y otros idiomas soportados (`áéíóúñçýÁÉÍÓÚÑÇÝ`).
- **Impacto:** Esto afecta las validaciones de los campos de nombres, apellidos, direcciones, titulares de cuenta, calles y departamentos. Ahora los usuarios podrán registrarse y actualizar sus datos utilizando tildes y caracteres especiales sin ser bloqueados erróneamente por el backend.
- **Validaciones Permisivas:** Se mantuvo la expresión regular `\p{L}` en la validación de `location` (`verifyLocationOptional`), comprobando que es una solución robusta y oficial para permitir cualquier letra Unicode (incluyendo tildes) en las entradas.

## Archivos Modificados
- `src/main/java/com/referidos/app/segurosref/helpers/ValidateInputHelper.java`
