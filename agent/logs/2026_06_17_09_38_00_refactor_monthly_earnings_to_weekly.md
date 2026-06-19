# Registro de Actualización: Refactorización de Ganancias Mensuales a Semanales (Últimos 7 días)

**Fecha**: 17 de Junio de 2026
**Responsable**: Agente Antigravity (Skill 1 & Skill 3)

## Resumen
Se modificó el flujo completo del método `monthlyEarnings` del `UserController` para que, en lugar de retornar las ganancias de los últimos 5 meses, ahora retorne las ganancias de los **últimos 7 días**. Este cambio está orientado a alimentar un gráfico interactivo diario en el frontend.

## Detalles de los Cambios

### 1. Reestructuración de DTOs
- **Eliminados**: `MonthlyCommissionDto`, `MonthlyDataDto`, y `MonthlyEarningDto`.
- **Creados**:
  - `DailyCommissionDto`: Representa una comisión individual procesada en un día específico.
  - `DailyDataDto`: Agrupa los totales (cantidad de comisiones y montos) para una fecha específica (`yyyy-MM-dd`).
  - `LastDaysEarningDto`: DTO principal que empaqueta los 7 días y los contadores globales (totales de la semana).

### 2. Capa de Servicios y Controladores
- **UserService / UserServiceImpl**:
  - Se renombró el método `monthlyEarnings` a `weeklyEarnings`.
  - La lógica de filtrado de fecha se ajustó a `currentDate.minusDays(6)` para contemplar el día actual más los 6 días anteriores.
  - Se adaptaron los métodos helpers `addDaysToEarnings` y `addCommissionToDailyEarnings` para iterar y asignar ganancias a los últimos 7 días.
- **UserController**:
  - Se cambió el mapeo de ruta de `/users/monthly/earnings` a `/users/weekly/earnings`.

### 3. Colecciones de Postman
- Se ejecutó el alineamiento (`$/postman`) sobre los entornos `dev` y `prod`.
- Se actualizó la ruta del endpoint a `/users/weekly/earnings` y el nombre descriptivo de la petición a `weekly earnings`.

## Impacto
- El frontend ahora deberá cambiar la URL de su petición y la interfaz que procesa la respuesta para adaptarse al formato diario.
- El build y la compilación de la API pasaron sin errores (`BUILD SUCCESS`), confirmando la eliminación limpia del código obsoleto.
