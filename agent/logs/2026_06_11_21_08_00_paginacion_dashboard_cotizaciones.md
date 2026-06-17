# Historial de Cambios en la API
**Fecha y Hora:** 11 de Junio de 2026 - 21:08

## Resumen de la Actualización
Implementación exitosa de paginación y filtros desde el Backend (usando MongoDB Aggregations) para optimizar el Dashboard de Cotizaciones del perfil Manager.

### Cambios Específicos
1. **Nuevos DTOs**:
   - Se añadió `DashboardPaginatedResponseDto` como estructura de envoltura para paginación (`content`, `page`, `size`, `totalElements`, `totalPages`).
   - Se enriqueció `DashboardQuoteDto` con campos extraídos de las transacciones: `transactionId`, `transactionStatus`, `commissionStatus` y `approvalDate`.

2. **Controlador (`ManagerController`)**:
   - El endpoint `GET /api/v1/manager/dashboard/quotes` se adaptó para recibir **Query Parameters**: `page` (0), `size` (10), `userId` (opcional) y `quoteStatus` (opcional).
   - Ahora retorna `DashboardPaginatedResponseDto`.

3. **Servicio (`ManagerServiceImpl`)**:
   - Reemplazo del patrón de carga masiva (`userRepository.findAll()`) por un pipeline de **MongoDB Aggregations** usando `MongoTemplate`.
   - El Pipeline realiza un `$unwind` de `quoters`, filtra dinámicamente con `$match` si llegan parámetros, ordena por `$sort`, y finalmente usa `$facet` para bifurcar y retornar tanto el conteo de elementos como la "ventana" paginada.
   - Uso del `MongoConverter` de Spring para un casteo seguro y nativo de documentos BSON a Modelos Java.

### Impacto Arquitectónico
- **Rendimiento:** Reducción drástica del uso de memoria RAM. Ahora solo viajan en la red y se procesan 10 elementos por defecto en vez de miles.
- **Escalabilidad:** Se delegó el conteo, filtrado y ordenamiento al motor de MongoDB en una sola petición a la base de datos (N+1 resuelto en transacciones para la página activa).
