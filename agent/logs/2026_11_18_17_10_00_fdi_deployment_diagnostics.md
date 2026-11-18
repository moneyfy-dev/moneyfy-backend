# Activacion controlada FDI autorizada por el usuario

- El usuario autoriza carga de la coleccion y despliegue para diagnosticar FDI
  actualmente inoperativa, con contrato externo aun pendiente de confirmar.
- Coleccion fdi_vehicle_mappings cargada: 723 documentos, 614 habilitados,
  109 deshabilitados; catalogo BCI sin modificar.
- Backups protegidos en el servidor: base refseguros y WAR/configuracion.
  El tag rollback/pre-fdi-homologation-2026-11-11 referencia el master anterior.
- Trazas opt-in solo de codigos, sin datos personales ni credenciales, para
  separar resolucion en Mongo de preparacion del POST de item FDI.
- No se incluye el prototipo BCI pausado. No se autorizan aprobaciones,
  transacciones de prueba ni pagos como parte de la validacion de cotizacion.
- Despliegue y comprobaciones operativas se verifican aparte; estas trazas
  no garantizan aceptacion del request por el proveedor.
