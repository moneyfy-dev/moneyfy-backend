# Ciclo de Vida de Pago de Comisiones (MoneyFy)

Este documento detalla el ciclo de vida y la máquina de estados de las comisiones generadas por los usuarios (agentes referidores), estableciendo las reglas de negocio críticas implementadas en el backend.

## 1. Relación de Modelos Involucrados

El flujo de comisiones abarca múltiples entidades interconectadas:
- **`QuoterModel`**: Representa la cotización original. Contiene un estado global (`quoterStatus`).
- **`TransactionModel`**: La transacción general creada al aprobarse un Quoter. Contiene un arreglo de comisiones desglosadas por usuario (dueño de la cotización y/o referidores en su jerarquía). Tiene su propio `status`.
- **`TransactionComissionModel`**: Sub-documento anidado en `TransactionModel`. Refleja la porción de dinero exacta para un usuario específico y su estado individual (`commissionStatus`).
- **`PaymentModel`**: Documento de trazabilidad financiera. Se crea cada vez que el sistema intenta procesar el pago de un usuario agrupando varias comisiones, **independientemente de si el pago fue exitoso o fallido**.
- **`WalletModel`**: La billetera del usuario. Mantiene los saldos (Disponible, Pendiente, Pagado, Total).

## 2. Estados de las Comisiones (`commissionStatus`)

Una comisión individual pasa por los siguientes estados:
1. **`Pendiente`**: Creada al finalizar la transacción. Todavía no ha sido aprobada por la aseguradora.
2. **`Aprobado`**: La aseguradora validó la cotización y la comisión entra al pozo de dinero `Disponible` en la Wallet del usuario. Solo en este estado la comisión es elegible para ser pagada (entrar a nómina).
3. **`Pagado`**: El administrador o el banco confirmó la transferencia exitosa hacia la cuenta bancaria del usuario. El dinero sale del balance `Disponible` de su Wallet y suma al balance de `Pagado`.
4. **`Conflictivo`**: El intento de transferencia bancaria falló (cuenta cerrada, rut incorrecto, etc.). El dinero **NO** se descuenta de la Wallet, pero se marca para seguimiento y se notifica al usuario para corregir sus datos.
5. **`Rechazado` / `Vencido` / etc.**: Otros estados terminales definidos por negocio en las fases iniciales.

## 3. Reglas de Negocio Críticas

### 3.1. Validación "Todo o Nada"
Cuando el sistema procesa una lista de transacciones a pagar para un único usuario, **todas** las comisiones correspondientes a ese usuario en esas transacciones deben estar en estado `Aprobado`. 
Si incluso una de las transacciones enviadas no está en estado `Aprobado` o no pertenece al usuario, la solicitud completa para ese usuario es rechazada, omitiendo la creación del registro de `PaymentModel`. Esto previene inconsistencias matemáticas y previene cobros dobles.

### 3.2. Independencia de Estados en `QuoterModel`
Una cotización (`QuoterModel`) puede generar múltiples comisiones (dueño, referido nivel 1, nivel 2). Si la comisión del dueño de la cotización es pagada exitosamente (`Pagado`), el `quoterStatus` cambia a `Pagado`, incluso si las comisiones de los otros referidos fallaron (`Conflictivo`). Esto refleja que la "venta" principal ya fue liquidada.

### 3.3. Estado Global de la Transacción (`TransactionModel`)
A diferencia de `QuoterModel`, el `status` general de un `TransactionModel` es estricto:
- Si **todas** las comisiones en el arreglo `commissions` tienen el estado `Pagado`, el estado de la transacción será `Pagado`.
- Si **al menos una** comisión en el arreglo está en `Conflictivo`, el estado de la transacción completa será `Conflictivo`, sirviendo de bandera roja para el administrador de que el ciclo de vida de este negocio aún no ha terminado.

### 3.4 Trazabilidad Constante (`PaymentModel`)
Cualquier respuesta de un banco o administrador sobre una nómina (ya sea `Pagado` o `Conflictivo`) generará la inserción de un `PaymentModel`. Si el pago fue `Conflictivo`, el modelo incluirá un campo `note` explicando el porqué y se gatillará un envío de correo al usuario para que subsane sus datos.
