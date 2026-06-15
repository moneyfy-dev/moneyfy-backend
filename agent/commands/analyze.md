# Comando: $/analyze
**Atajos soportados:** `$/chk`

Este comando actúa como la primera línea de evaluación para cualquier requerimiento nuevo. Su objetivo es tomar un prompt del usuario (incluso si es vago o carece de detalles) e identificar su alcance, complejidad y la estrategia técnica para abordarlo, antes de escribir una sola línea de código.

## Comportamiento
Al recibir este comando seguido de un requerimiento, asumes el rol de arquitecto y evaluador, siguiendo este flujo:

1. **Clasificación de Complejidad**:
   - **Requerimiento Normal**: Tareas puntuales, modificaciones simples o ajustes menores.
   - **Requerimiento Complejo**: Tareas que implican crear nuevos flujos, refactorizaciones arquitectónicas o lógicas de negocio entrelazadas que requieren entrar en **Modo de Planificación** (`Planning Mode`).
   - *Nota*: Si el usuario especifica explícitamente el tipo de requerimiento, debes respetar su clasificación. Puedes apoyarte mentalmente (o invocar) el comando `$/review` para inspeccionar el código actual y determinar la complejidad.

2. **Refinamiento Base**:
   - Si es **Normal**: Redactas un prompt o plan de acción sencillo, preciso y detallado que el agente podrá seguir directamente.
   - Si es **Complejo**: Indicas claramente que el requerimiento necesita un análisis profundo y recomiendas ejecutar inmediatamente el comando `$/refine` para crear el plan maestro.

3. **Recomendación Táctica**:
   - **Comando Sugerido**: Indicas qué comando será el ideal para ejecutar la tarea (ej. `$/target` para flujos, `$/endpoint` para nuevas APIs, o `$/refactor` para deuda técnica).
   - **Modelo LLM Sugerido**: Sugieres qué modelo de inteligencia artificial sería el más capaz para esa tarea (ej. modelos rápidos para tareas Normales, o modelos de razonamiento avanzado para tareas Complejas).

4. **Claridad Total**: El output debe dejar completamente claro al agente y al usuario qué se va a hacer, cómo se clasifica la tarea y cuáles son los siguientes pasos exactos.
