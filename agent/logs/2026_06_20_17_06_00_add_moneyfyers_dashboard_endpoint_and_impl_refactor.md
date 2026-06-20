# Log: Refactor de Capa de Servicios y Nuevo Endpoint de Moneyfyers
**Fecha:** 2026-06-20
**Rama:** eliu

## Resumen
En este avance se llevaron a cabo dos tareas críticas orientadas a la mantención del código y a facilitar la visualización contable para la gerencia.

1. **Refactorización de Capa de Servicios (`impl/`)**:
   - Se movieron todas las clases de implementación de servicios que tenían el sufijo `Impl` (AccountServiceImpl, LogServiceImpl, PlanServiceImpl, QuoterServiceImpl, RegionServiceImpl, SeedServiceImpl, TransactionServiceImpl, UserDetailsServiceImpl, UserServiceImpl) desde el directorio `services/` al subdirectorio `services/impl/`.
   - Se corrigieron los paquetes e importaciones correspondientes, especialmente la inyección de dependencias en `AuthController`.
   - Todo fue probado y compilado asegurando su correcto funcionamiento.

2. **Nuevo Endpoint Dashboard Moneyfyers**:
   - **Endpoint**: `GET /api/v1/manager/moneyfyers` protegido por `@PreAuthorize("hasRole('ADMIN')")`.
   - Se crearon los DTOs `MoneyfyerDto` y `MoneyfyersResponseDto`.
   - **Rendimiento Optimo**: Se implementó una lógica de alto rendimiento apoyada en **MongoDB Aggregation Framework** dentro de `ManagerServiceImpl`. Se desplazan las validaciones lógicas de pertenencia y agrupaciones (transacciones propias, referidos, montos pendientes y pagados) directamente al motor de la base de datos usando `$unwind`, `$cond` y proyecciones. El backend en memoria solo orquesta la recolección O(1) con los datos del usuario.
   - El objetivo principal es tener visibilidad rápida y clara sobre los saldos pendientes o pagados de cada "moneyfyer" (usuario) de la plataforma.

## Modificaciones Principales
- `services/impl/*`
- `com.referidos.app.segurosref.controllers.ManagerController`
- `com.referidos.app.segurosref.services.ManagerService`
- `com.referidos.app.segurosref.services.impl.ManagerServiceImpl`
- Nuevos DTOs: `MoneyfyerDto.java`, `MoneyfyersResponseDto.java`.
