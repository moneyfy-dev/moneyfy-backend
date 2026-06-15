# Corrección de estructura de PlanModel y permisividad en listReferreds

## Resumen
Se resolvieron dos incidencias que afectaban al proceso de cotización y al listado de referidos: 
1. Incoherencias en cómo se almacenaban y se leían las coberturas en el registro del plan de seguros en la base de datos.
2. Excepciones de tipo 424 originadas cuando un referido se encontraba en la relación pero había sido eliminado o no aparecía en el registro maestro de usuarios.

## Cambios realizados
- **PlanModel**: Se agregó el campo de las coberturas (`Set<QuotationPlanCoverDto> coverages;`) para que el blueprint del plan pueda retener y devolver las coberturas en el endpoint estático de búsqueda (`findPlanById`).
- **QuoterServiceImpl**: Se actualizó el constructor de instanciación del `PlanModel` para enviar `insurerPlan.getCoverages()` y guardarlo correctamente en base de datos.
- **UserServiceImpl (listReferreds)**: Se sustituyó el método limitante `.orElseThrow()` por comprobaciones mediante `Optional` en la búsqueda de usuarios `B` y `C`. Ahora si un usuario falta en el documento de usuarios, se aplica `continue` y la lista vacía resultante se puede entregar sin levantar excepciones en el servidor.
