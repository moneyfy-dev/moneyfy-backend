# Registro de Actualización: Estandarización BCI/FDI y Optimización N+1

**Fecha:** 18 de Junio de 2026
**Responsable:** Agente Antigravity (Senior Backend Developer / Context Manager)

## Resumen de Cambios

1. **Optimización N+1 en `QuoterServiceImpl`**:
   - Se refactorizó el método `searchPlan` que consultaba los planes guardados históricamente en MongoDB.
   - Ahora agrupa los `planId` únicos y utiliza `planRepository.findAllById()` para realizar una sola llamada a la base de datos, en lugar de consultar iterativamente uno a uno.
   - Se implementó la lógica para acumular coberturas (`coverages`) usando `Set.addAll`, asegurándose de no sobreescribir coberturas existentes ni generar duplicados.
   - Se centralizó la persistencia con un `planRepository.saveAll()`.
   - Se solucionó un defecto que no asignaba dinámicamente el nombre de la aseguradora desde la base de datos al DTO.

2. **Estandarización de Coberturas BCI (`BCIDocsHelper`)**:
   - Se creó el directorio `integrations/bci/docs` y la clase de apoyo `BCIDocsHelper`.
   - La lógica para las coberturas del producto "Solución Móvil 2.0" se refactorizó para que dinámicamente ajuste los textos de límites de deducible según el deducible principal del plan, acorde a la documentación oficial.
   - Se emplearon los códigos oficiales de registro S.V.S. (`POL` y `CAD`) indicados en la documentación.
   - Se establecieron métodos estáticos para unificar atributos base como `workshopType`, `stolenVehicle`, entre otros.

3. **Estandarización FDI (`FDIQuotationClient`)**:
   - Se estandarizó la captura del deducible y la descripción amigable (`deductibleDesc`) para utilizar los textos `"Sin Deducible"`, `"Deducible <x> UF"`, o `"No definido"`.
   - Se homologaron los atributos base con BCI (`"Valor comercial"`, etc).

4. **Remoción de Atributo Obsoleto (`details`)**:
   - Se eliminó el atributo `details` tanto de `QuotationPlanDto` como del modelo persistido `PlanModel`.
   - Se limpiaron las dependencias y constructores en `BCIQuotationClient`, `FDIQuotationClient`, `QuoterServiceImpl` y se actualizó la data de prueba en `QuoterHelper` para no invocar más este atributo.
   
5. **Ajuste Menor en Controlador (`ManagerController`)**:
   - Se detectó una reorganización del método de `payQuotes` realizado en el código.
