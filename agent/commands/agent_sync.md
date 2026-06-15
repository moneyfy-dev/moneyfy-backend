---
name: Comando $/agent_sync
description: Comando para invocar la alineación y actualización del contexto y las skills del agente.
atajos: $/sync, $/org
---

# Comando `$/agent_sync`
**Atajos soportados:** `$/sync, $/org`

## Propósito
Sincronizar, evaluar y organizar todo el conocimiento y comportamiento del agente. Invoca al **Context Manager** y al **Skill Commander** en simultáneo para asegurar que la documentación base, las estructuras de las carpetas, las reglas de negocio y los roles del agente estén siempre en perfecta armonía con los últimos cambios y descubrimientos del proyecto.

## Cuándo usar
- Después de haber implementado refactorizaciones grandes o cambios de arquitectura.
- Cuando el contexto, las reglas de negocio o el comportamiento deseado parezcan desalineados o incompletos.
- Cuando se desea evaluar si es necesario crear nuevas skills o estructurar mejor la documentación actual.

## Skills Invocadas
- [Context Manager](file:///d:/wk/moneyfy/agent/skills/context_manager.md)
- [Skill Commander](file:///d:/wk/moneyfy/agent/skills/skill_commander.md)

## Flujo de Trabajo
1. **Evaluación de Cambios**: El agente evalúa los últimos cambios y decisiones hechas durante la interacción o el desarrollo.
2. **Context Manager (Actualización de Documentación)**: Revisa el `agent.md` y las carpetas del contexto. Si es necesario, reestructura, limpia, crea nuevos archivos de contexto y asegura que los conceptos estén claros y las políticas (como la limpieza de archivos temporales) se documenten adecuadamente.
3. **Skill Commander (Actualización de Skills/Comandos)**: Analiza si los nuevos requerimientos ameritan la creación de un nuevo rol (skill) o la afinación de comandos existentes (`/commands`), generándolos y vinculándolos al flujo del agente.
4. **Reporte Final**: El agente finaliza indicando un resumen de qué archivos de contexto, comandos o skills fueron actualizados y cómo esto mejora el ecosistema.

## Restricciones
- No debe realizar modificaciones de código fuente (no refactorizar el código de la API).
- Su uso es estrictamente para la gestión de los archivos ubicados bajo `agent/` y el archivo central `agent.md`.
