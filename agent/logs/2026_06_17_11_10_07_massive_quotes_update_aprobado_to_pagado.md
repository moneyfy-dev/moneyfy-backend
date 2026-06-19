# Massive Quotes Update from "Aprobado" to "Pagado"

## Resumen de Cambios

Se implementó el flujo masivo para procesar pagos y actualizar el estado de cotizaciones y transacciones de `Aprobado` a `Pagado` para usuarios específicos.

### Flujos Alterados y Nuevos:
- **`ManagerController`**: Se añadió el nuevo endpoint `POST /pay-quotes` para manejar las peticiones de actualización de pagos masivos de usuarios.
- **`ManagerServiceImpl`**:
  - Implementación de la lógica para el método `payQuotes` que maneja el registro por lotes (Batch) en memoria para evitar el problema de N+1 (consultas a DB excesivas).
  - Implementación de un modelo estricto transaccional lógico de "Todo o Nada" por cada usuario, evitando la actualización parcial.
  - Modificación del flujo de `getQuotesDashboard` para capturar la fecha de pago (`paymentDate`) de manera individual desde la comisión `TransactionComissionModel` del usuario.

### Modificaciones de Modelos:
- **`PaymentModel`**: Se eliminó la propiedad redundante `paymentDate`.
- **`TransactionModel`**: Se agregó la propiedad general `paymentDate` para capturar cuándo todas las comisiones fueron pagadas.
- **`TransactionComissionModel`**: Se agregó `paymentDate` para llevar control de la fecha de pago a nivel individual por usuario.
- **DTOs**: Creación de los nuevos DTOs (`PayQuotesRequest`, `UserQuotePaymentDto`, `FailedPaymentDto`) y modificación de `DashboardQuoteDto` para añadir `paymentDate`.

Los errores de inicialización de los modelos originados por la refactorización fueron solucionados modificando `QuoterServiceImpl` y `QuoterHelper`. El código fue compilado exitosamente.
