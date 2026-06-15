# Comando: $/endpoint
**Atajos soportados:** $/ep, $/api

Genera la estructura completa de un nuevo endpoint REST en el proyecto.

## Comportamiento
Al recibir este comando junto con un modelo de datos o requerimiento, debes asumir el rol de la **Skill 1: Senior Backend Developer** y generar:

1. **Controller**: La capa de entrada HTTP, mapeando los métodos REST correspondientes y aplicando validaciones necesarias.
2. **Service / ServiceImpl**: La capa de lógica de negocio e interfaz de servicio.
3. **Repository**: La capa de persistencia de datos (Spring Data JPA, etc.).
4. **DTOs**: Objetos de transferencia de datos de entrada y salida necesarios para el cuerpo de la petición/respuesta.
