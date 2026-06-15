# Regla de Negocio: Requisito de Cuenta Bancaria para Cotizar

## Descripción
En la aplicación, los usuarios con rol `USER` actúan como corredores que cotizan planes de seguros para vehículos con el fin de generar comisiones por cada venta concretada.

## Regla
* **Condición Obligatoria**: Un usuario no puede comenzar una cotización de seguro si no tiene al menos una cuenta bancaria asociada y registrada en su perfil.
* **Propósito**: Asegurar que la aplicación tenga una cuenta bancaria destino válida para transferir y pagar las comisiones obtenidas cuando estas sean liberadas.
* **Comportamiento del Sistema**:
  - En los endpoints iniciales del ciclo de cotización (por ejemplo, búsqueda de marcas `/search/vehicle/brands` y búsqueda de vehículos `/search/vehicle`), se debe validar que el usuario tenga cuentas registradas en el arreglo `accounts`.
  - Si no posee ninguna cuenta, el sistema debe bloquear el flujo y retornar un código HTTP `423 Locked` con un mensaje indicando que debe asociar una cuenta bancaria antes de continuar.
