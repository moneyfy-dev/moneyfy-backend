# MVP: Cotización de Aseguradoras y Panel Administrativo
**Fecha de Registro:** 2026-06-16 07:35:00

## Estado Base del Proyecto
El proyecto ya contaba con:
- Flujo funcional de autenticación.
- Usuarios principales de la aplicación con rol `USER`.
- Manejo de cuentas bancarias para recibir comisiones.
- Flujo simulado básico de cotización.

## Alcance y Objetivos de este MVP / Incremental
El objetivo de esta fase es implementar el ciclo de vida real y completo de las cotizaciones y establecer la base para la gestión administrativa.

1. **Integración de Aseguradoras Externas**: Conexión con aseguradoras reales (FDI y BCI) (Esta etapa ya se encuentra realizada).
2. **Ciclo de Vida de la Cotización**: El flujo completo debe estar trazado de manera íntegra, desde el estado inicial `"Iniciando"` hasta el estado final `"Pagado"`.
3. **Panel de Administración (`ADMIN`)**: Implementación de servicios para la gestión de usuarios `ADMIN`.
   - Los usuarios administradores se encargarán de realizar ajustes en las etapas finales del flujo de la cotización.
   - Acciones que puede realizar el ADMIN sobre la cotización: pasarla a `"Aprobado"`, `"Rechazado"`, `"Caducado"`, o cuando ya está en `"Aprobado"`, transicionarla a `"Pagado"`.
4. **Gestión de Comisiones**: Las comisiones para el usuario principal (corredor) deben ser gestionadas correctamente y acreditarse/reflejarse con base en estos estados administrados.
5. **Manejo de Vulnerabilidades (Seguridad)**: El incremental incluye mantener coherencia en la arquitectura de seguridad, como la reciente reestructuración de JWT y sesiones, asegurando que los flujos (especialmente los administrativos) sean robustos frente a vulnerabilidades.

## Roadmap Adicional (No es prioridad del MVP - Tareas en caso de contar con tiempo extra)
El usuario ha definido las siguientes iniciativas secundarias que se llevarán a cabo *sólo si no retrasan la entrega del MVP principal*:

1. **Refactorización de Estructura de Carpetas**:
   - Mapeo y reestructuración completa de las carpetas del proyecto para facilitar orden y organización siguiendo un estándar unificado.
   - **Regla Estricta**: Esta refactorización no debe modificar de ninguna manera la lógica interna ni alterar el funcionamiento de los flujos actuales de la API.
2. **Integración con Bot de Telegram (Monitor de APIs Externas)**:
   - Dado que los errores con aseguradoras externas frecuentemente devuelven `200 OK` con un mensaje de error encapsulado de negocio (lo que dificulta ver el código HTTP real o el body sin entrar al servidor), se planea crear un Bot de Telegram.
   - **Funcionalidad del Bot**: Actuará como monitor mapeando los errores de las aseguradoras (y eventualmente cualquier error complejo de la API) y enviando el detalle, estado, cuerpo y origen de la respuesta para acelerar el diagnóstico sin tener que hacer pruebas en el servidor.
