# Homologacion de vehiculos FDI desde MongoDB

- Solicitud: reemplazar el vehiculo fijo FDI por codigos del Excel homologado,
  manteniendo BCI sin modificaciones; publicar solo este cambio en `eliu`.
- Nueva coleccion `fdi_vehicle_mappings` con _id portalCode; catalogo de 723
  filas, 614 utilizables. Importacion explicita, repetible y sin borrado masivo.
- Resolucion exacta normalizada por marca/modelo; filas incompletas o ambiguas
  impiden llamadas HTTP al proveedor. No se deduce brandId FDI de codigos BCI.
- Pruebas con HTTP local capturan modelo/version/tipo FDI y codigos BCI;
  MongoDB real de Docker verifica lectura e importacion doble sin duplicados.
- Contrato JSON externo de version/tipo provisional: adaptador deshabilitado
  por defecto hasta validacion oficial. Se documentan campos de direccion
  ausentes y limites en `docs/fdi-vehicle-homologation.md`.
- No se publica el prototipo BCI previo ni se cambia autenticacion, emision,
  pagos, comisiones, master o MongoDB del servidor.
- La importacion del servidor debe autorizarse y ejecutarse en un arranque
  controlado de la nueva version; esta tarea no la efectua.
- Validacion del snapshot exclusivo FDI a publicar: WAR generado y 64 pruebas
  correctas, cero fallos, errores u omitidas, con MongoDB real local.
