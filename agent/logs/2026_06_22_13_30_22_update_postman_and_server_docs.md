# Registro de Modificaciones - Colecciones Postman y Documentación del Servidor

## Fecha y Hora
2026-06-22 13:30:22

## Descripción de los Cambios
Este registro consolida la actualización de las colecciones de pruebas (Postman) y la estructuración de la documentación de arquitectura del servidor para producción.

### Modificaciones en Postman
- **`MoneyFy_Dev_API` y `MoneyFy_Prod_API`**:
  - Se eliminó definitivamente de ambas colecciones el endpoint obsoleto de `/weekly/earnings` perteneciente a la ruta del usuario final, garantizando que el equipo de frontend/mobile cuente con la última versión de los contratos de la API.

### Documentación del Proyecto (`agent/docs`)
- **`servidor_referencia.md`**:
  - Se creó un documento "puntero" dentro del contexto del agente (`agent/docs/servidor_referencia.md`).
  - Este documento alerta al agente sobre la sensibilidad de la configuración del servidor de producción (Ubuntu + Tomcat 10) y redirige la lectura hacia un archivo de texto ubicado fuera del repositorio (`D:\wk\useful_data\doc_moneyfy\servidor_entorno\tomcat_env_config.txt`), donde se explican a detalle las diferencias entre el archivo nativo de sistema (`setenv.sh`) y el archivo de persistencia de Spring Boot (`/lib/properties/own-env.properties`).
  - El objetivo es mantener el repositorio limpio de datos sensibles de la infraestructura y proveer una guía segura para futuras consultas del equipo DevOps/Backend.

## Archivos Modificados
- `agent/postman/dev/MoneyFy_Dev_API.postman_collection.json`
- `agent/postman/prod/MoneyFy_Prod_API.postman_collection.json`
- `agent/docs/servidor_referencia.md` (Nuevo)
