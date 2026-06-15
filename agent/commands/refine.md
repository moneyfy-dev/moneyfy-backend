# Comando: $/refine
**Atajos soportados:** `$/plan`, `$/deep`

Este comando trabaja en conjunto y de la mano con el comando `$/analyze` (o `$/check`), actuando exclusivamente cuando un requerimiento ha sido clasificado como **Complejo** y requiere entrar en Modo de Planificación (Planning Mode).

## Comportamiento
Al invocar este comando, asumes un rol analítico profundo y metódico para desglosar un requerimiento vago o amplio en una ruta de trabajo ejecutable:

1. **Recepción del Contexto**:
   - Toma el resultado de la comprobación previa (de `$/analyze`) y el prompt inicial del usuario.
   
2. **Refinamiento de Requerimiento**:
   - Transforma el prompt vago original en un requerimiento altamente detallado, preciso y claro. 
   - Identifica y lista los archivos específicos que deberán ser modificados, los DTOs, las validaciones y los controladores involucrados.

3. **Creación del Plan de Implementación**:
   - Basándose en el requerimiento refinado, crea o actualiza el artefacto `implementation_plan.md`.
   - El plan debe contener los pasos exactos para lograr el objetivo, garantizando que el agente tendrá total claridad de lo que se debe hacer paso a paso sin suposiciones.

4. **Alineación Táctica**:
   - Confirma y estructura el plan para cumplir con el comando sugerido (ej. `$/target` o `$/refactor`) y hace uso del modelo LLM óptimo sugerido previamente.
   - Si existen ambigüedades, debe documentar explícitamente "Open Questions" en el plan para que el usuario las aclare antes de iniciar la escritura de código.
