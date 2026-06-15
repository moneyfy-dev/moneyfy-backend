# Contexto del Proyecto

Este es un proyecto backend para una API REST construido con Java y Spring Boot (Java 21). El enfoque principal es crear un código limpio, modular, eficiente y escalable, utilizando buenas prácticas de inyección de dependencias, manejo de excepciones y validaciones estandarizadas.

## Propósito del Negocio
La aplicación es una API que se conecta con múltiples aseguradoras externas para cotizar y recuperar planes de seguro asociados a un vehículo.
- **Flujo de Búsqueda de Planes**: El controlador retorna un objeto `QuotationDto` que a su vez contiene una lista de planes del tipo `QuotationPlanDto`.
- **Estructura Multiaseguradora**: El DTO `QuotationPlanDto` consolida todos los campos relevantes de las distintas aseguradoras. Los campos propios de una aseguradora específica (como `quotationIdBCI` para BCI o campos del FDI) se completan únicamente cuando corresponde a dicha aseguradora; para las demás se envían como `null` (o `""` en caso de cadenas de texto).
- **Seguridad**: La API REST maneja la recuperación de información basada en **tokens de autenticación del usuario**.
- **Aseguradoras**:
  - Aseguradoras 1, 2 y 3: Entornos de prueba.
  - Aseguradora 4 (BCI): Integración productiva.
  - Aseguradora 5 (FDI): Integración productiva.

## Modelo de Negocio y Estructura del Usuario
La aplicación gestiona usuarios con dos roles principales: **USER** (corredores que realizan cotizaciones para generar comisiones) y **ADMIN** (administradores). 

La base de datos utiliza MongoDB. La colección de usuarios (`UserModel`) maneja la siguiente estructura y objetos embebidos:
- **`userId`**: Identificador único de Mongo (ObjectId).
- **`codeToRefer`**: Código de referido único del usuario, utilizado para invitar a otros a la plataforma.
- **`disableAccount`**: Fecha que marca cuándo la cuenta ha sido deshabilitada (si aplica).
- **`personalData`**: Información personal y credenciales del usuario.
- **`wallet`**: Monedero embebido para registrar balances (saldo pendiente, saldo disponible y saldo total) y gestionar el pago de comisiones acumuladas.
- **`accounts`**: Arreglo de cuentas bancarias asociadas al usuario para seleccionar en cuál cobrar las comisiones.
- **`quoters` (Cotizaciones)**: Arreglo embebido con las cotizaciones realizadas por el usuario desde su cuenta para asegurar vehículos de sí mismo o de terceros (amigos, conocidos, etc.).
- **`notifPreference`**: Estructura de notificaciones de la aplicación (módulo en fase inicial, solo posee estructura de objeto por ahora).

### Sistema de Referidos y Comisiones
Un usuario (USER) puede ingresar a la aplicación utilizando el código de referido de otro usuario. Cuando un usuario referido concreta con éxito una cotización (es decir, pasa a estado aprobado), se genera una comisión principal para él y comisiones secundarias más pequeñas para sus referidores hacia arriba en la cadena de invitación, con un límite de hasta **3 niveles de referidos** y una comisión total máxima de **50,000**.

**Detalle Técnico (`ReferredModel` y Flujo):** La relación de quién invitó a quién se guarda en la colección `referrals` mediante `ReferredModel`. Este modelo vincula el correo del usuario invitado (campo `referred`) con el correo del usuario que lo invitó (campo `userReferring`). Durante la generación y finalización de una transacción (`generateTransaction` y `finalizeQuote`), el código consulta iterativamente a través de `findByReferred(email)` para encontrar los referidores directos e indirectos (hasta 3 niveles). A cada referidor encontrado se le ajusta su `wallet` sumando la comisión correspondiente en su `outstandingBalance` y luego a `availableBalance` cuando se aprueba, anexando además un `TransactionComissionModel` en el historial de la transacción principal.

Las comisiones son fijas y se distribuyen de la siguiente manera:
- **Nivel 1 (El que cotiza)**: **35,000**
- **Nivel 2 (Referidor directo)**: **10,000**
- **Nivel 3 (Referidor indirecto)**: **5,000**

#### Ejemplos de Flujo de Referidos:
* **Ejemplo de 4 Niveles**: `Usuario A` refiere a `Usuario B`, `Usuario B` refiere a `Usuario C`, `Usuario C` refiere a `Usuario D`.
  - Si el `Usuario D` realiza una cotización exitosa:
    - `Usuario D` (quien cotiza - Nivel 1) recibe **35,000**.
    - `Usuario C` (referidor de D - Nivel 2) recibe **10,000**.
    - `Usuario B` (referidor de C - Nivel 3) recibe **5,000**.
    - `Usuario A` no recibe comisión porque supera el límite máximo de 3 niveles.
    - Comisión total repartida: **50,000**.
* **Ejemplo de 2 Niveles**: `Usuario B` refiere a `Usuario C`. El `Usuario B` no posee referidor (se registró sin código).
  - Si el `Usuario C` realiza una cotización exitosa:
    - `Usuario C` (quien cotiza - Nivel 1) recibe **35,000**.
    - `Usuario B` (referidor de C - Nivel 2) recibe **10,000**.
    - Nivel 3 queda vacío (comisión total = **45,000**).

Los datos y detalles de las comisiones se centralizan en la colección de transacciones (`TransactionModel`).

### Estados de la Cotización y Transacción
El flujo de una cotización y la transacción asociada se coordina a través de estados específicos:

#### Estados de la Cotización (`QuoterModel` embebido):
1. **`Iniciando`**: Estado inicial al arrancar el proceso (ocurre en la búsqueda de vehículo).
2. **`Cotizando`**: Estado activo durante la búsqueda y comparación de planes con aseguradoras.
3. **`Recopilando`**: Se establece cuando el usuario selecciona un plan específico y provee datos de inspección.
4. **`Pendiente`**: Se activa al momento de generar la transacción de la propuesta formal a la aseguradora y procesar las comisiones.
5. **`Aprobado` / `Rechazado` / `Caducado`**: Determinado por la respuesta de la aseguradora (aprobación/pago verificado e inspección completada).
6. **`Liberado`**: Cuando el dinero acumulado de la comisión se transfiere a la cuenta bancaria del usuario.

#### Estados de la Transacción (`TransactionModel`):
Las transacciones se generan en el momento en que se concreta una propuesta formal. Manejan exactamente el mismo ciclo de estados que la cotización, con la salvedad de que la transacción **comienza directamente en el estado `Pendiente`** (cuando se crea el registro de la transacción asociada a la cotización).

### Patrones Técnicos y Flujos Masivos (Batch)
Para operaciones masivas (por ejemplo, el endpoint `finalizeQuote` para uso exclusivo de `ADMIN`), la aplicación implementa patrones específicos para proteger la integridad y el rendimiento:
1. **Actualización por Lotes y Estados en Memoria**: Dado que Spring Data MongoDB no utiliza un "dirty checking" automático al estilo JPA, al modificar una misma entidad (como `UserModel`) múltiples veces en una misma petición, se almacena su referencia más reciente en un `Map` (ej. `usersToSave`). Cualquier iteración posterior sobre ese usuario recupera la instancia del `Map` para evitar sobrescrituras con referencias obsoletas. Al final del flujo, se efectúa un único `saveAll()`.
2. **Resiliencia ante Excepciones y Notificaciones**: Los errores individuales (por ejemplo, `NoSuchElementException` al no hallar un referidor) no detienen el proceso global. Se capturan, se reportan utilizando `LOGGER_MESSAGES.info` con un formato claro (`"Usuario X - Cotización Y: [Error]"`), se marca el estado necesario (ej. `userReferringFound = false`) y el flujo continúa con la siguiente cotización sin actualizar comisiones indebidas. *(Nota Arquitectónica: Actualmente la gestión de avisos y trazabilidad se maneja mediante `LOGGER_MESSAGES`, pero en el futuro se planea reemplazar o complementar con una integración hacia un **Bot de Telegram** para establecer un sistema de notificaciones de procesos más robusto).*
3. **Inyección de Propiedades (`own-env.properties`)**: Valores de negocio como las comisiones (niveles 1, 2 y 3) se externalizan e inyectan mediante `@Value` facilitando su gestión según el entorno.

---

## Agent Roles (Skills)
- [Skill 1: Senior Backend Developer](file:///d:/wk/moneyfy/agent/skills/backend_developer.md)
- [Skill 2: Technical Instructor](file:///d:/wk/moneyfy/agent/skills/technical_instructor.md)
- [Skill 3: Context Manager](file:///d:/wk/moneyfy/agent/skills/context_manager.md)
- [Skill 4: Skill Commander](file:///d:/wk/moneyfy/agent/skills/skill_commander.md)

## Custom Commands

> [!NOTE]
> **Prefijo de Comandos:** Para diferenciar la invocación de comandos de las rutas de archivos, todos los comandos y sus atajos deben ser invocados utilizando el prefijo `$/` (por ejemplo, `$/api` o `$/target`).
- [$/target](file:///d:/wk/moneyfy/agent/commands/target.md) (Atajos: `$/t`, `$/req`): Establece un requerimiento a cumplir modificando, refactorizando o creando flujos. Utiliza la Skill 1.
- [$/endpoint](file:///d:/wk/moneyfy/agent/commands/endpoint.md) (Atajos: `$/ep`, `$/api`): Genera la estructura completa de un nuevo endpoint REST (Controller, Service, Repository, DTOs). Utiliza la Skill 1.
- [$/explain](file:///d:/wk/moneyfy/agent/commands/explain.md) (Atajos: `$/ex`, `$/why`): Detalla el funcionamiento de bloques de código y conceptos del framework. Utiliza la Skill 2.
- [$/review](file:///d:/wk/moneyfy/agent/commands/review.md) (Atajos: `$/rev`): Analiza el **código existente** buscando vulnerabilidades, problemas de rendimiento o mejoras de estructura (Code Review). Utiliza la Skill 1.
- [$/refactor](file:///d:/wk/moneyfy/agent/commands/refactor.md) (Atajos: `$/ref`, `$/clean`): Reestructura el código existente para mejorar validaciones, empaquetado, DTOs, excepciones globales, rendimiento y Clean Code. Utiliza la Skill 1.
- [$/commit](file:///d:/wk/moneyfy/agent/commands/commit.md) (Atajos: `$/c`, `$/save`): Guarda el progreso y avances de desarrollo realizando staging (`git add`) y confirmación (`git commit`) de cambios en la rama `eliu` con un mensaje en inglés. Utiliza la Skill 1.
- [$/analyze](file:///d:/wk/moneyfy/agent/commands/analyze.md) (Atajos: `$/chk`): Evalúa y clasifica **requerimientos de usuario** vagos o nuevos. Deduce si son simples o complejos, sugiere comandos y prepara el terreno.
# Contexto del Proyecto

Este es un proyecto backend para una API REST construido con Java y Spring Boot (Java 21). El enfoque principal es crear un código limpio, modular, eficiente y escalable, utilizando buenas prácticas de inyección de dependencias, manejo de excepciones y validaciones estandarizadas.

## Propósito del Negocio
La aplicación es una API que se conecta con múltiples aseguradoras externas para cotizar y recuperar planes de seguro asociados a un vehículo.
- **Flujo de Búsqueda de Planes**: El controlador retorna un objeto `QuotationDto` que a su vez contiene una lista de planes del tipo `QuotationPlanDto`.
- **Estructura Multiaseguradora**: El DTO `QuotationPlanDto` consolida todos los campos relevantes de las distintas aseguradoras. Los campos propios de una aseguradora específica (como `quotationIdBCI` para BCI o campos del FDI) se completan únicamente cuando corresponde a dicha aseguradora; para las demás se envían como `null` (o `""` en caso de cadenas de texto).
- **Seguridad**: La API REST maneja la recuperación de información basada en **tokens de autenticación del usuario**.
- **Aseguradoras**:
  - Aseguradoras 1, 2 y 3: Entornos de prueba.
  - Aseguradora 4 (BCI): Integración productiva.
  - Aseguradora 5 (FDI): Integración productiva.

## Modelo de Negocio y Estructura del Usuario
La aplicación gestiona usuarios con dos roles principales: **USER** (corredores que realizan cotizaciones para generar comisiones) y **ADMIN** (administradores). 

La base de datos utiliza MongoDB. La colección de usuarios (`UserModel`) maneja la siguiente estructura y objetos embebidos:
- **`userId`**: Identificador único de Mongo (ObjectId).
- **`codeToRefer`**: Código de referido único del usuario, utilizado para invitar a otros a la plataforma.
- **`disableAccount`**: Fecha que marca cuándo la cuenta ha sido deshabilitada (si aplica).
- **`personalData`**: Información personal y credenciales del usuario.
- **`wallet`**: Monedero embebido para registrar balances (saldo pendiente, saldo disponible y saldo total) y gestionar el pago de comisiones acumuladas.
- **`accounts`**: Arreglo de cuentas bancarias asociadas al usuario para seleccionar en cuál cobrar las comisiones.
- **`quoters` (Cotizaciones)**: Arreglo embebido con las cotizaciones realizadas por el usuario desde su cuenta para asegurar vehículos de sí mismo o de terceros (amigos, conocidos, etc.).
- **`notifPreference`**: Estructura de notificaciones de la aplicación (módulo en fase inicial, solo posee estructura de objeto por ahora).

### Sistema de Referidos y Comisiones
Un usuario (USER) puede ingresar a la aplicación utilizando el código de referido de otro usuario. Cuando un usuario referido concreta con éxito una cotización (es decir, pasa a estado aprobado), se genera una comisión principal para él y comisiones secundarias más pequeñas para sus referidores hacia arriba en la cadena de invitación, con un límite de hasta **3 niveles de referidos** y una comisión total máxima de **50,000**.

**Detalle Técnico (`ReferredModel` y Flujo):** La relación de quién invitó a quién se guarda en la colección `referrals` mediante `ReferredModel`. Este modelo vincula el correo del usuario invitado (campo `referred`) con el correo del usuario que lo invitó (campo `userReferring`). Durante la generación y finalización de una transacción (`generateTransaction` y `finalizeQuote`), el código consulta iterativamente a través de `findByReferred(email)` para encontrar los referidores directos e indirectos (hasta 3 niveles). A cada referidor encontrado se le ajusta su `wallet` sumando la comisión correspondiente en su `outstandingBalance` y luego a `availableBalance` cuando se aprueba, anexando además un `TransactionComissionModel` en el historial de la transacción principal.

Las comisiones son fijas y se distribuyen de la siguiente manera:
- **Nivel 1 (El que cotiza)**: **35,000**
- **Nivel 2 (Referidor directo)**: **10,000**
- **Nivel 3 (Referidor indirecto)**: **5,000**

#### Ejemplos de Flujo de Referidos:
* **Ejemplo de 4 Niveles**: `Usuario A` refiere a `Usuario B`, `Usuario B` refiere a `Usuario C`, `Usuario C` refiere a `Usuario D`.
  - Si el `Usuario D` realiza una cotización exitosa:
    - `Usuario D` (quien cotiza - Nivel 1) recibe **35,000**.
    - `Usuario C` (referidor de D - Nivel 2) recibe **10,000**.
    - `Usuario B` (referidor de C - Nivel 3) recibe **5,000**.
    - `Usuario A` no recibe comisión porque supera el límite máximo de 3 niveles.
    - Comisión total repartida: **50,000**.
* **Ejemplo de 2 Niveles**: `Usuario B` refiere a `Usuario C`. El `Usuario B` no posee referidor (se registró sin código).
  - Si el `Usuario C` realiza una cotización exitosa:
    - `Usuario C` (quien cotiza - Nivel 1) recibe **35,000**.
    - `Usuario B` (referidor de C - Nivel 2) recibe **10,000**.
    - Nivel 3 queda vacío (comisión total = **45,000**).

Los datos y detalles de las comisiones se centralizan en la colección de transacciones (`TransactionModel`).

### Estados de la Cotización y Transacción
El flujo de una cotización y la transacción asociada se coordina a través de estados específicos:

#### Estados de la Cotización (`QuoterModel` embebido):
1. **`Iniciando`**: Estado inicial al arrancar el proceso (ocurre en la búsqueda de vehículo).
2. **`Cotizando`**: Estado activo durante la búsqueda y comparación de planes con aseguradoras.
3. **`Recopilando`**: Se establece cuando el usuario selecciona un plan específico y provee datos de inspección.
4. **`Pendiente`**: Se activa al momento de generar la transacción de la propuesta formal a la aseguradora y procesar las comisiones.
5. **`Aprobado` / `Rechazado` / `Caducado`**: Determinado por la respuesta de la aseguradora (aprobación/pago verificado e inspección completada).
6. **`Liberado`**: Cuando el dinero acumulado de la comisión se transfiere a la cuenta bancaria del usuario.

#### Estados de la Transacción (`TransactionModel`):
Las transacciones se generan en el momento en que se concreta una propuesta formal. Manejan exactamente el mismo ciclo de estados que la cotización, con la salvedad de que la transacción **comienza directamente en el estado `Pendiente`** (cuando se crea el registro de la transacción asociada a la cotización).

### Patrones Técnicos y Flujos Masivos (Batch)
Para operaciones masivas (por ejemplo, el endpoint `finalizeQuote` para uso exclusivo de `ADMIN`), la aplicación implementa patrones específicos para proteger la integridad y el rendimiento:
1. **Actualización por Lotes y Estados en Memoria**: Dado que Spring Data MongoDB no utiliza un "dirty checking" automático al estilo JPA, al modificar una misma entidad (como `UserModel`) múltiples veces en una misma petición, se almacena su referencia más reciente en un `Map` (ej. `usersToSave`). Cualquier iteración posterior sobre ese usuario recupera la instancia del `Map` para evitar sobrescrituras con referencias obsoletas. Al final del flujo, se efectúa un único `saveAll()`.
2. **Resiliencia ante Excepciones y Notificaciones**: Los errores individuales (por ejemplo, `NoSuchElementException` al no hallar un referidor) no detienen el proceso global. Se capturan, se reportan utilizando `LOGGER_MESSAGES.info` con un formato claro (`"Usuario X - Cotización Y: [Error]"`), se marca el estado necesario (ej. `userReferringFound = false`) y el flujo continúa con la siguiente cotización sin actualizar comisiones indebidas. *(Nota Arquitectónica: Actualmente la gestión de avisos y trazabilidad se maneja mediante `LOGGER_MESSAGES`, pero en el futuro se planea reemplazar o complementar con una integración hacia un **Bot de Telegram** para establecer un sistema de notificaciones de procesos más robusto).*
3. **Inyección de Propiedades (`own-env.properties`)**: Valores de negocio como las comisiones (niveles 1, 2 y 3) se externalizan e inyectan mediante `@Value` facilitando su gestión según el entorno.

---

## Agent Roles (Skills)
- [Skill 1: Senior Backend Developer](file:///d:/wk/moneyfy/agent/skills/backend_developer.md)
- [Skill 2: Technical Instructor](file:///d:/wk/moneyfy/agent/skills/technical_instructor.md)
- [Skill 3: Context Manager](file:///d:/wk/moneyfy/agent/skills/context_manager.md)
- [Skill 4: Skill Commander](file:///d:/wk/moneyfy/agent/skills/skill_commander.md)
- [Skill 5: Postman Collection Manager](file:///d:/wk/moneyfy/agent/skills/postman_manager.md)

## Custom Commands

> [!NOTE]
> **Prefijo de Comandos:** Para diferenciar la invocación de comandos de las rutas de archivos, todos los comandos y sus atajos deben ser invocados utilizando el prefijo `$/` (por ejemplo, `$/api` o `$/target`).
- [$/target](file:///d:/wk/moneyfy/agent/commands/target.md) (Atajos: `$/t`, `$/req`): Establece un requerimiento a cumplir modificando, refactorizando o creando flujos. Utiliza la Skill 1.
- [$/endpoint](file:///d:/wk/moneyfy/agent/commands/endpoint.md) (Atajos: `$/ep`, `$/api`): Genera la estructura completa de un nuevo endpoint REST (Controller, Service, Repository, DTOs). Utiliza la Skill 1.
- [$/explain](file:///d:/wk/moneyfy/agent/commands/explain.md) (Atajos: `$/ex`, `$/why`): Detalla el funcionamiento de bloques de código y conceptos del framework. Utiliza la Skill 2.
- [$/review](file:///d:/wk/moneyfy/agent/commands/review.md) (Atajos: `$/rev`): Analiza el **código existente** buscando vulnerabilidades, problemas de rendimiento o mejoras de estructura (Code Review). Utiliza la Skill 1.
- [$/refactor](file:///d:/wk/moneyfy/agent/commands/refactor.md) (Atajos: `$/ref`, `$/clean`): Reestructura el código existente para mejorar validaciones, empaquetado, DTOs, excepciones globales, rendimiento y Clean Code. Utiliza la Skill 1.
- [$/commit](file:///d:/wk/moneyfy/agent/commands/commit.md) (Atajos: `$/c`, `$/save`): Guarda el progreso y avances de desarrollo realizando staging (`git add`) y confirmación (`git commit`) de cambios en la rama `eliu` con un mensaje en inglés. Utiliza la Skill 1.
- [$/analyze](file:///d:/wk/moneyfy/agent/commands/analyze.md) (Atajos: `$/chk`): Evalúa y clasifica **requerimientos de usuario** vagos o nuevos. Deduce si son simples o complejos, sugiere comandos y prepara el terreno.
- [$/refine](file:///d:/wk/moneyfy/agent/commands/refine.md) (Atajos: `$/plan`, `$/deep`): Trabaja con `$/analyze` en requerimientos complejos. Refina el prompt y crea un plan de implementación detallado para garantizar claridad total antes de programar.
- [$/agent_sync](file:///d:/wk/moneyfy/agent/commands/agent_sync.md) (Atajos: `$/sync`, `$/org`): Invoca al **Context Manager** y al **Skill Commander** para alinear, organizar y estructurar el conocimiento, reglas de negocio y skills del agente.
- [$/postman](file:///d:/wk/moneyfy/agent/commands/postman.md) (Atajos: `$/col`, `$/post`): Mantiene sincronizada la colección de pruebas de Postman y su entorno, inyectando dependencias, borrando endpoints obsoletos y actualizando rutas para reflejar la API de forma automática utilizando la Skill 5.
- [$/log](file:///d:/wk/moneyfy/agent/commands/log.md): Genera un archivo Markdown en `agent/logs/` con el historial de la última actualización (formato `YYYY_MM_DD_HH_mm_nombre.md`). Este comando debe ejecutarse tras `$/commit` o actualizar el contexto.

## Historial de Contexto (Logs)
**REGLA OBLIGATORIA:** Al iniciar un nuevo requerimiento, el agente debe siempre leer y tener como referencia los últimos 5 registros (logs) ubicados en la carpeta `agent/logs/`. Esto proveerá el contexto de los desarrollos más recientes de la API.

## Limits and Workflow
- [Limits and Workflow](file:///d:/wk/moneyfy/agent/limits/workflow.md): Directrices del proceso de desarrollo en la rama `eliu`, requerimientos de revisión, políticas para proponer Pull Requests y flujos de commits.
- **Exclusión de Git**: Los directorios `/agent` y `.agent` están excluidos de Git en `.gitignore` y se mantienen de forma estrictamente local.

## Business Rules
- [Business Rules Directory](file:///d:/wk/moneyfy/agent/business_rules/): Carpeta con reglas de negocio obligatorias.
- [Security and Authentication](file:///d:/wk/moneyfy/agent/business_rules/security_auth.md): Estándares para el manejo de AuthModel, JWT, Sliding Session y estandarización de errores HTTP 401.
- [Bank Account Requirement](file:///d:/wk/moneyfy/agent/business_rules/bank_account_requirement.md): Validación obligatoria sobre el registro de cuentas bancarias antes de iniciar flujos de cotización.
- [N+1 Optimization](file:///d:/wk/moneyfy/agent/business_rules/optimization_n_plus_one.md): Patrón arquitectónico obligatorio para evitar sobrecarga en la base de datos al recuperar colecciones anidadas.
- [Pending Flows](file:///d:/wk/moneyfy/agent/business_rules/pending_flows.md): Registro del roadmap y flujos de negocio proyectados a futuro (ej. flujo de Liberación de comisiones).

### Política de Evaluación Continua y Actualización del Contexto
Toda modificación sustancial al modelo de negocios, a los flujos principales (ej: generación masiva de transacciones) o al modelo de la API (ej: uso de adaptadores y tipos precisos como `BigDecimal` para representación financiera) exige la invocación inmediata de la **Skill 3: Context Manager**. 
El agente debe revisar si las lógicas implementadas o los nuevos descubrimientos hacen que la documentación actual quede obsoleta o escasa. En tal caso, el agente debe actualizar `agent.md` proactivamente y reorganizar o crear archivos nuevos bajo `agent/` para asegurar que todo el contexto de negocio permanezca robusto, actualizado y jerárquico.
