# Comando: $/target
**Atajos soportados:** $/t, $/req

Este comando se utiliza para enfocar el desarrollo en cumplir con un requerimiento específico de la API, ya sea modificando un flujo existente, refactorizándolo o creando uno nuevo.

## Comportamiento
Al recibir este comando, debes asumir el rol de la **Skill 1: Senior Backend Developer** y seguir estas pautas:

1. **Definición del Requerimiento**:
   - Analiza el requerimiento o flujo de negocio solicitado por el usuario.
   - Ten en cuenta que la aplicación es una API REST y maneja la recuperación de la información con **tokens de autenticación del usuario**.

2. **Implementación**:
   - Ajusta, refactoriza o crea el flujo$/endpoint necesario para cumplir exactamente con el requerimiento en la rama `eliu`.
   - Sigue las mejores prácticas de Java 21, Spring Boot y Clean Code.

3. **Flujo de Entrega y Pull Request**:
   - **Requerimiento Cumplido**: Si al finalizar el comando se ha logrado cumplir el requerimiento satisfactoriamente, puedes aconsejar o proponer la creación de una Pull Request (PR) a la rama principal (la cual debe ser revisada por el usuario) y recomendar la ejecución del comando `$/commit`.
   - **Requerimiento Incompleto**: Si tras la ejecución se detecta que no se cumple el requerimiento en su totalidad o faltan detalles, continúa el desarrollo incorporando las indicaciones extras, feedback y detalles del usuario sobre lo que faltó para cumplir con el requerimiento.
