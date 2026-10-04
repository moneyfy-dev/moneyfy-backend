# Correccion de informes de comisiones

Solicitud: separar por aprobar/por pagar/pagadas, explicar los importes y detectar discrepancias antes de produccion.

- Dashboard: importes historicos y estado por beneficiario, con Pendiente, Aprobado, Pagado y Conflictivo separados. Corrige pagos parciales y no utiliza commissionTotal para los totales monetarios.
- Detalle ADMIN paginado por estado/periodo/beneficiario, conservando transacciones cuya cotizacion no existe.
- Periodo inclusivo por creacion de transaccion, compartido entre tarjetas y detalle. Graficos semanales mantienen su alcance anterior explicito.
- Consolidado: pendientes de pago solo Aprobado; por aprobar y conflictivas separados. Generado conserva Aprobado + Pagado + Conflictivo.
- Montos de reportes long; lectura Number.longValue para sumas BSON Long.
- Conciliacion historica de wallet.outstandingBalance versus comisiones Pendiente, sin save/saveAll ni reparaciones automaticas.
- No hay cambios de esquema Mongo ni migraciones ni cambios en las reglas de pago o cotizacion. Backend debe desplegarse antes del admin actualizado.

Validacion: fallos de pendientes/pagos parciales reproducidos antes; fallo BSON Long reproducido antes. WAR generado con 56 pruebas aprobadas (14 CORS, 29 seguridad incluyendo nueve accesos USER/anonimo/ADMIN a reportes nuevos, 13 informes). Datos y repositorios aislados; falta conciliacion autenticada real y validacion del pipeline sobre Mongo real. Contrato y aceptacion documentados en docs/commission-reporting.md.
