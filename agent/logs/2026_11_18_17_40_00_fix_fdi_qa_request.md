# Correccion del request FDI validada contra QA

- FDI QA rechazo RUT con puntos, telefono null y direcciones null en PUT deal.
- Probar RUT normalizado, telefono entero y direcciones de certificacion dio 204.
- QA rechazo versionId/typeId como propiedades adicionales en POST item.
  Con solo modelId homologado, ano/patente/motor/chasis y policyHolder valido,
  POST item dio 201; GET quotations dio 504 upstream. No se emitio ni contrato.
- Request MoneyFy agrega fdiAddress para FDI; valida telefono y comprador
  propietario antes de llamadas externas. BCI no requiere campos nuevos.
- Version/tipo permanecen en Mongo como referencia, no se envian a la API
  que los rechaza. Sin datos obligatorios se devuelve error de negocio 82.
- Errores de parseo/planes FDI usan 80/81, sin colision con homologacion 78/79.
- Logs FDI sin payloads ni datos personales; timeout de tarifas acotado y sin
  reintentos. El frontend necesita aportar la direccion para poder cotizar FDI.
- El prototipo BCI pausado permanece fuera del commit y del despliegue.
- Snapshot limpio de la correccion: WAR generado y 67 pruebas correctas,
  incluyendo Mongo real y compatibilidad BCI; cero fallos, errores u omitidas.
