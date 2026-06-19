# Registro de Cambios: Refactorización de FinalizeQuote y Sincronización de Postman

## Fecha y Hora
2026-06-17 18:07:37

## Resumen de Cambios
- **Refactorización del flujo `finalizeQuote`**: Se ha trasladado el endpoint `finalizeQuote` desde el controlador `QuoterController` hacia el `ManagerController` (`PUT /api/v1/manager/finalize/quote`). Este cambio permite consolidar la lógica administrativa y de comisiones bajo un mismo controlador y servicio (`ManagerServiceImpl`), manteniendo la cohesión y responsabilidad del rol Manager.
- **Migración de Dependencias**: La lógica en `QuoterServiceImpl` se trasladó a `ManagerServiceImpl`, inyectando exitosamente el `ReferredRepository`, la clave `apiKeyMF` y las variables de comisión `commissionUserA`, `commissionUserB` y `commissionUserC`.
- **Actualización de Postman Collections**: Mediante un script en Python se analizaron y actualizaron las colecciones Postman (tanto `Dev` como `Prod`) de manera automatizada. Se movió la petición `Finalize Quote` a la carpeta `Manager` modificando su URL. Adicionalmente, se incluyó el endpoint `POST /api/v1/manager/pay-quotes` creado en los requerimientos previos y se verificó la existencia de `Weekly Earnings` dentro de `User`.
- **Validación Adicional**: Se validó la corrección reportada por el IDE respecto a la comparación de fechas (`getPaymentDate()`), confirmando la necesidad de cambiar de `DataHelper.deprecatedDate()` a `DataHelper.deprecatedDateTime()` para evitar fallos lógicos debido a discrepancias en el tipado (`LocalDateTime` vs `LocalDate`).
