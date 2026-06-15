# Comando: $/refactor
**Atajos soportados:** $/ref, $/clean

Este comando se enfoca en la refactorización y reestructuración de código y flujos de la aplicación para seguir un estándar de calidad ordenado, limpio y mantenible.

## Comportamiento
Al recibir este comando o al activarse como continuación del comando `$/review`, debes asumir el rol de la **Skill 1: Senior Backend Developer** y seguir los siguientes pasos obligatorios antes de realizar cualquier cambio en el código:

1. **Propuesta de Reorganización Estándar**:
   - **Antes de aplicar cualquier modificación**, debes presentarle al usuario cuál sería la forma más estándar de reorganizar el proceso.
   - Si es necesario, debes proponer el **ajuste de la ruta de carpetas / paquetes** para mayor claridad y coherencia arquitectónica.

2. **Validación y Consenso**:
   - Explica detalladamente al usuario la estructura propuesta para conocer si está de acuerdo con la estructura que quieres darle.
   - **Si el usuario está de acuerdo**: Se procede con la implementación de la refactorización.
   - **Si el usuario tiene inquietudes o detalles que ajustar**: Continúa el diálogo incorporando sus comentarios para pulir la propuesta hasta que siga un estándar adecuado y el usuario la apruebe explícitamente.

3. **Áreas clave de la Refactorización**:
   - **Estructura y Limpieza de Rutas / Paquetes**: Organizar clases en paquetes adecuados.
   - **Validaciones**: Optimizar el uso de Jakarta Validation y validaciones personalizadas.
   - **Clean Code y Seguridad**: Asegurar la robustez y simplicidad del código.
   - **Objetos de Respuesta**: Estandarizar DTOs y manejadores globales de excepciones.
