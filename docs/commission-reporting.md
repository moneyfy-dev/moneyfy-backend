# Informes y conciliacion de comisiones

## Criterios

Los informes usan el importe historico y el estado de cada elemento de `transactions.commissions`, no el estado global ni `commissionTotal`. Una transaccion puede tener beneficiarios pagados, aprobados pendientes de pago y pagos conflictivos al mismo tiempo.

| Estado de la comision | Indicador |
| --- | --- |
| Pendiente | Por aprobar; aun no es pagable |
| Aprobado | Por pagar |
| Pagado | Pagadas |
| Conflictivo | Pagos conflictivos; separado del importe listo para pagar |
| Rechazado / Caducado | Excluido de los cuatro importes |

El consolidado Moneyfyers aplica el mismo criterio a pendientes de pago y agrega campos `pendingApprovalCommissions` y `conflictCommissions`. Generado propio/referidos/total conserva Aprobado + Pagado + Conflictivo: un pago conflictivo sigue siendo una comision aprobada. Los importes de reportes usan `long` para evitar desbordamientos de sumas; los importes historicos almacenados no cambian.

## Endpoints ADMIN de solo lectura

- `GET /api/v1/manager/dashboard/summary`: agrega `pendingApprovalCommissions`, `conflictCommissions`, `dateFrom`, `dateTo`; `pendingCommissions` pasa a contener exclusivamente Aprobado.
- `GET /api/v1/manager/dashboard/commissions?status=Pendiente&page=0&size=10`: detalle por beneficiario, identificadores de transaccion/cotizacion, estado, importe, fecha y beneficiario. Opcional `userId`; tamanos 1..100, paginas desde cero. `totalAmount` suma todos los resultados, no solo la pagina. Conserva registros cuya cotizacion desaparecio con `quoteMissing=true`.
- `GET /api/v1/manager/dashboard/commission-reconciliation`: compara `users.wallet.outstandingBalance` con la suma historica de las comisiones Pendiente por beneficiario. Devuelve solo diferencias y billeteras/perfiles ausentes. `difference = walletPendingApproval - ledgerPendingApproval`; si la billetera no existe, la diferencia es `null`, nunca cero inventado.

Resumen y detalle aceptan `dateFrom` / `dateTo` ISO YYYY-MM-DD. Limites inclusivos por fecha de creacion de la transaccion; sin filtros incluyen todo el historial. Registros sin fecha se incluyen solo en todo el historial. Los graficos semanales y usuarios activos conservan su alcance anterior y se explican por separado en el admin.

## Datos y publicacion

No hay cambios de esquema, indices, estados persistidos, reglas de pago ni reparaciones automaticas de saldos. Desplegar backend antes del admin porque los campos nuevos son obligatorios en su contrato. El despliegue del WAR debe mantener deshabilitados los procesos automaticos de siembra/limpieza existentes.

Antes de produccion, abrir la conciliacion con una cuenta ADMIN real y contrastar cada discrepancia con las transacciones, sus beneficiarios y las respuestas de aseguradoras. Un saldo desajustado requiere una reparacion independiente, con respaldo y revision del importe; no se borra ni recalcula automaticamente. Los informes usan consultas masivas existentes y paginacion en memoria; evaluar agregacion/proyecciones Mongo si crece el volumen.

Los tests aislados validan importes, estados parciales, fechas, orfandad, paginacion y que la conciliacion no llama a save/saveAll. El test de BSON Long del consolidado no ejecuta el pipeline en Mongo real; la aceptacion autenticada y conciliacion real siguen siendo necesarias.
