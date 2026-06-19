# Comando: $/commit
**Atajos soportados:** $/c, $/save

Este comando se utiliza para guardar en un commit de git los cambios y avances realizados en la rama de desarrollo `eliu`.

## Comportamiento
Al recibir este comando o cuando se recomiende su ejecución, debes asumir el rol de la **Skill 1: Senior Backend Developer** y seguir las siguientes directrices:

1. **Restricción de Rama**:
   - Este comando **solo** se puede utilizar para realizar commits dentro de la rama de desarrollo `eliu`. No se debe ejecutar en otras ramas.

2. **Registro en el Historial Automático ($/log)**:
   - Antes de proceder con el `git add` y el `commit`, debes generar automáticamente el archivo de log correspondiente a los avances realizados en el historial del agente (`agent/logs/`). Esto garantiza que el registro se incluya en el mismo commit junto con las modificaciones. No esperes a que el usuario lo solicite.

3. **Preparación de Cambios (Staging)**:
   - Realizar `git add` de los archivos modificados, nuevos o eliminados que formen parte del avance a consolidar (incluyendo el nuevo archivo de log generado).

4. **Mensaje del Commit**:
   - Redactar un mensaje de commit claro, conciso y profesional **obligatoriamente en inglés**.
   - El formato del mensaje debe seguir las convenciones de Git (por ejemplo: `feat: add BCI integration client`, `docs: rename commands to English`).

5. **Sincronización (Push)**:
   - Inmediatamente después de crear el commit, debes ejecutar obligatoriamente un empuje de los cambios al repositorio remoto en la misma rama. Por ejemplo: `git push origin eliu`.

6. **Recomendación y Activación**:
   - Se debe revisar si hay cambios pendientes en la rama `eliu` tras la aplicación de comandos como `$/endpoint`, `$/target`, `$/review` o `$/refactor`.
   - Si se detecta que se realizaron cambios de código, se debe **sugerir y recomendar** al usuario la ejecución del comando `$/commit`.
   - **REGLA ESTRICTA**: Está TOTALMENTE PROHIBIDO ejecutar el comando `$/commit` de forma proactiva. Solo se debe ejecutar cuando el usuario lo pida explícitamente o luego de que confirme tu sugerencia tras finalizar una tarea.

7. **Sugerencia de Descripción para Pull Request (PR)**:
   - Independientemente de si el usuario decide hacer una PR inmediatamente o no, después de ejecutar exitosamente el flujo de este comando (`$/c`), debes generar y **sugerirle al usuario una descripción estructurada y profesional para una Pull Request**.
   - Esta descripción debe abarcar los cambios más relevantes que se encuentran actualmente en la rama `eliu` y que aún no han sido integrados a `master`.
   - **Propósito**: Facilitar la vida del usuario entregándole un borrador listo de PR (título, qué cambió, por qué cambió, y consideraciones especiales) para que él mismo lo utilice cuando inicie su proceso de PR.
