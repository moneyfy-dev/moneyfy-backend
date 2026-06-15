# Flujos Pendientes (Business Rules)

Este documento registra los flujos de negocio que están proyectados para desarrollo futuro, con el fin de mantener el contexto del agente alineado con el roadmap del producto.

## 1. Flujo de Liberación de Comisiones
- **Estado Actual**: Las cotizaciones llegan hasta el estado final `"Aprobado"`, `"Rechazado"` o `"Caducado"`.
- **Flujo Pendiente**: Existe una fase posterior donde una cotización con estado `"Aprobado"` transita al estado `"Liberado"`.
- **Lógica de Negocio Proyectada**: El estado `"Liberado"` representa el momento en que las comisiones retenidas (que se encuentran en el "saldo disponible") son efectivamente **pagadas/liberadas** a los usuarios (referidores).
- **Impacto**: Esto probablemente involucrará la lógica de la billetera virtual del usuario (Wallet/WalletBalance), el registro histórico de comisiones (Transactions) y posibles comprobantes de pago. No pertenece al flujo inicial del Dashboard, pero debe contemplarse arquitectónicamente a futuro para no bloquear su implementación.

## 2. Ajuste de Seguridad: Rol de Administrador
- **Estado Actual**: El `ManagerController` se encuentra configurado para validar una clave API maestra `X-Moneyfy-Api-Key` interceptada a nivel de cabecera contra `moneyfy.api-key` del `own-env.properties` (al igual que los seeders).
- **Flujo Pendiente**: Refactorizar la validación para utilizar estrictamente Spring Security. El JWT emitido en el inicio de sesión del Administrador deberá poseer los `Claims` apropiados (ej. `role: ADMIN`) para que el controlador sea asegurado usando la anotación `@PreAuthorize("hasRole('ADMIN')")`.
- **Impacto**: Se requerirá un ajuste en el modelo de usuario, la emisión en el Login y la intercepción en `JwtValidationFilter`.
