# Limits and Workflow

Este documento define las reglas estrictas de desarrollo y el flujo de trabajo que el agente debe seguir obligatoriamente durante su interacción con el proyecto.

## Reglas y Directrices

> [!NOTE]
> **Prefijo de Comandos:** Todos los comandos mencionados en este flujo deben invocarse con el prefijo `$/` para no confundirlos con rutas de sistema.


1. **Rama de Trabajo Obligatoria (`eliu`)**:
   - Todos los cambios, experimentos y desarrollos de código se deben realizar exclusivamente en la rama `eliu`.
   - **NUNCA** realices commits directos o mezclas automáticas a la rama principal.
   - **Exclusión de Git para `/agent` y `.agent`**: El directorio `/agent` y el archivo/directorio `.agent` están excluidos de Git en `.gitignore`. Por política del proyecto, todo este contexto de documentación y configuración se mantiene de forma estrictamente local y sin seguimiento de Git.

2. **Proceso de Revisión**:
   - Todos los cambios realizados por el agente deben ser presentados y estar listos para ser revisados por el usuario para su posterior confirmación.
   
3. **Flujo del Comando $/target y Pull Requests (PR)**:
   - El desarrollo se organiza en torno al comando [$/target](file:///d:/wk/moneyfy/agent/commands$/target.md), el cual asume de manera obligatoria la **Skill 1: Desarrollador Backend Senior**.
   - Un **Target** consiste en cumplir con un requerimiento de negocio específico a través de la creación, modificación o refactorización de flujos de la API REST.
   - **Cumplimiento del Requerimiento**: Una vez completado y verificado el requerimiento, el agente puede proponer o aconsejar la apertura de una Pull Request (PR) a la rama principal. Esta PR será revisada y aprobada por el usuario.
   - **Requerimiento Incompleto**: Si al finalizar la ejecución del comando aún no se cumple en su totalidad o faltan detalles, se continuará el desarrollo en la rama `eliu` incorporando las indicaciones extras, feedback y detalles que el usuario provea sobre lo que faltó para completarlo.

4. **Flujo de Interacción entre $/review y $/refactor**:
   - Cuando se llame al comando [$/review](file:///d:/wk/moneyfy/agent/commands$/review.md), el agente debe presentar un resumen de cómo se realiza actualmente el proceso y sugerir si es conveniente ajustarlo.
   - Si el usuario decide ajustar el flujo y provee contexto, el agente presentará dos opciones: ajustar de forma compatible con la estructura existente, o aplicar el comando [$/refactor](file:///d:/wk/moneyfy/agent/commands$/refactor.md) para una solución más ordenada.
   - Al proceder con el comando `$/refactor`, antes de efectuar cambios, el agente debe presentar una propuesta sobre la reorganización estándar y el ajuste de ruta de carpetas, iterando con el usuario hasta que esté de acuerdo y la apruebe.

5. **Flujo del Comando $/commit y el Historial ($/log)**:
   - Tras ejecutar comandos de desarrollo como [$/endpoint](file:///d:/wk/moneyfy/agent/commands/endpoint.md), [$/target](file:///d:/wk/moneyfy/agent/commands/target.md), [$/review](file:///d:/wk/moneyfy/agent/commands/review.md) o [$/refactor](file:///d:/wk/moneyfy/agent/commands/refactor.md), se debe revisar si existen cambios pendientes en la rama `eliu`.
   - Si existen cambios pendientes, se debe **sugerir y recomendar** al usuario la ejecución del comando [$/commit](file:///d:/wk/moneyfy/agent/commands/commit.md). **NUNCA debes ejecutar este comando de forma autónoma sin que el usuario lo solicite explícitamente o lo confirme.**
   - Este comando realiza la preparación (`git add`) y confirmación (`git commit`) de los cambios en la rama de desarrollo `eliu`.
   - **Mensaje de Commit**: Obligatoriamente en inglés, descriptivo y siguiendo buenas prácticas.
   - **Historial (NUEVO)**: Inmediatamente después de que el commit sea exitoso, el agente debe recomendar o ejecutar el comando `$/log` para registrar este avance en el historial.
   - **Exclusividad**: Solo en la rama `eliu` usando la **Skill 1: Desarrollador Backend Senior**.

6. **Stack Tecnológico y Contexto de la API**:
   - El desarrollo se realiza utilizando **Java 21** y **Spring Boot** para la API REST.
   - La API REST maneja la recuperación y el acceso a la información mediante **tokens de autenticación del usuario**. Toda lógica de controlador y servicio debe integrarse de acuerdo a este flujo de seguridad y autenticación.

7. **Evaluación Continua y Actualización del Contexto**:
   - Cada vez que el usuario proporcione detalles específicos, reglas de negocio, directrices de arquitectura o flujos (con o sin comando) para realizar una tarea, debes evaluar si este conocimiento ya existe en `agent.md` o en los archivos de la carpeta `agent/`.
   - Si se trata de un contexto valioso, relevante y que no está registrado, debes incorporarlo mentalmente para la tarea actual, y al finalizar, debes **sugerir proactivamente al usuario** su inclusión en la documentación del agente (dentro de un archivo bajo `agent/` o en `agent.md`).
   - El objetivo es mantener el conocimiento del agente actualizado y simplificar futuras interacciones, refactorizaciones y desarrollos dentro de la API.

8. **Políticas de Pruebas y Seguridad**:
   - Queda estrictamente prohibido crear usuarios por defecto (default/test users) con contraseñas hardcodeadas o crear condiciones excepcionales (bypasses) en los filtros de seguridad (como JwtValidationFilter) o en la lógica de negocio para saltarse la autenticación. Todo flujo de pruebas o desarrollo debe apegarse a la validación de seguridad real del negocio usando los correspondientes Tokens JWT.

9. **Historial y Contexto Previo (Últimos 5 Logs)**:
   - **OBLIGATORIO**: Siempre que el usuario solicite un nuevo requerimiento, modificación o inicio de tarea, el agente debe **consultar el directorio `agent/logs/`** y leer los últimos 5 archivos `.md` de historial disponibles. Esto es crucial para que el agente entienda lo último que se hizo y tenga el mayor contexto posible antes de intervenir en el código.
