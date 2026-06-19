# Registro de Actualización: Plantilla HTML para Pagos Conflictivos

**Fecha:** 19 de Junio de 2026

## Resumen de Cambios

Se ha actualizado el mecanismo de notificación de pagos conflictivos para utilizar una plantilla HTML profesional, alineándose con el estándar de los demás correos del sistema. Anteriormente, los correos de rechazo de pagos se enviaban en formato de texto plano (`StringBuilder`).

## Archivos Modificados/Agregados

1. **`src/main/resources/templates/emails/conflictive-payment.html` (NUEVO)**
   - Plantilla HTML utilizando Thymeleaf.
   - Recibe y renderiza dinámicamente las variables `note` (motivo del rechazo) y `support` (correo de soporte).
   - Incluye advertencias de seguridad corporativas (Moneyfy nunca solicita contraseñas).

2. **`src/main/resources/properties/own-env.properties` & `own-env.example.properties`**
   - Se agregaron las propiedades de configuración:
     - `mail.subject.notify-conflictive-payment=Inconveniente con tu pago de comisiones`
     - `mail.template.notify-conflictive-payment=emails/conflictive-payment`

3. **`src/main/java/com/referidos/app/segurosref/integrations/email/providers/EmailAppProvider.java`**
   - Inyección de las nuevas propiedades `subjectNotifyConflictivePayment` y `templateNotifyConflictivePayment`.
   - Modificación del método `notifyConflictivePayment` para intentar enviar el correo HTML vía `sendEmail` con `templateData`. En caso de fallo (ej. plantilla no encontrada), se preservó el envío en texto plano como mecanismo de seguridad (fallback).
