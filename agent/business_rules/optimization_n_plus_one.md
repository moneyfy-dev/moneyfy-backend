# Optimización N+1 (MongoDB / Spring Data)

Esta regla arquitectónica debe aplicarse siempre que se detecte una posible iteración de consultas a la base de datos dentro de un ciclo (bucle `for`, `while` o `forEach`).

## Definición del Problema
Cuando se recuperan colecciones anidadas (ej. Cotizaciones dentro de Usuarios) y se requiere un dato relacional externo por cada uno de ellos (ej. Transacciones), hacer un `repository.findById()` dentro de un bucle generará cientos o miles de viajes por red hacia la base de datos (N+1 queries). Esto ahoga el pool de conexiones, bloquea otros hilos y colapsa el rendimiento en entornos Cloud.

## Patrón de Solución Obligatorio
El agente debe **siempre** refactorizar la lógica hacia una consulta masiva y un mapeo en memoria `O(1)`:

1. **Recolección**: Iterar los objetos iniciales en memoria (Java) para extraer puramente los `IDs` foráneos y agregarlos a una `List<String> ids`.
2. **Consulta Masiva (Batch Query)**: Usar un método de repositorio en Spring Data MongoDB con el sufijo `In`, por ejemplo: `List<TransactionModel> findByQuoterIdIn(List<String> quoterIds);`. Esto se traduce al operador ultra optimizado `$in` de Mongo.
3. **Mapeo Hash (Memoria)**: Usar la API de Streams de Java para transformar la lista en un `Map` (Diccionario). Ejemplo:
   ```java
   Map<String, TransactionModel> map = transactions.stream()
       .collect(Collectors.toMap(TransactionModel::getQuoterId, t -> t, (t1, t2) -> t1));
   ```
4. **Asignación Rápida**: Ejecutar el bucle principal nuevamente, extrayendo los datos desde el `Map` usando `map.get(id)`, operando 100% en CPU sin latencia de red.
