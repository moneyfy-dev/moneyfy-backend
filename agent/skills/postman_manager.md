# Skill: Postman Collection Manager

**Propósito**: Actuar como un integrador y mantenedor avanzado de colecciones de Postman y sus respectivos archivos de entorno (`postman_environment.json`).

## Responsabilidades Principales
1. **Sincronización Bidireccional**: Cuando la API de Spring Boot muta (se crean o eliminan endpoints en los Controllers), el Postman Collection Manager debe ser capaz de analizar los decoradores `@GetMapping`, `@PostMapping`, etc., y reflejarlos en el JSON de la colección bajo el directorio `agent/postman/collections`.
2. **Gestión Dinámica del Entorno**: Mantener variables de entorno actualizadas en `agent/postman/env`, inyectando configuraciones requeridas como Tokens de Sesión, API Keys maestras (`moneyfy.api-key`), URL Base (`localhost:8080`) y credenciales por defecto.
3. **Scripts de Prueba Automatizados**: Inyectar scripts de JavaScript en la sección `event` de las peticiones críticas de Postman (ej. login) para extraer los tokens de respuesta (`pm.response.json()`) y guardarlos automáticamente en las variables de entorno (`pm.environment.set()`).

## Flujo de Ejecución Recomendado
Cuando se invoque esta skill mediante el comando apropiado:
1. Buscar archivos `.postman_collection.json` y `.postman_environment.json`.
2. Utilizar un script de Python auxiliar (creado temporalmente) para leer el esquema JSON, eliminar rutas obsoletas, actualizar rutas nuevas, y escribir el archivo resultante en disco sin romper su estructura JSON.
3. Asegurar que las variables base estén siempre configuradas y listas para la prueba por parte del desarrollador.
