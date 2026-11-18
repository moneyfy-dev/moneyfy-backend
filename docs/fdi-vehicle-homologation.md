# Homologacion local de vehiculos FDI

## Estado y limites

Implementado en el worktree local `moneyfy-backend-bci-prototype`, rama `eliu`.
La primera version se publico y se cargo la coleccion el 2026-11-18 mediante
autorizacion expresa del usuario. El prototipo BCI de transaccion se conserva
localmente, pero no forma parte del commit de homologacion FDI publicado.

**Correccion comprobada en FDI QA, 2026-11-18:** PUT de negocio con RUT sin
puntos, telefono entero y direcciones completas devuelve 204. POST de item
con `modelId=2058` devuelve 201. FDI rechaza `versionId` y `typeId` como
propiedades adicionales: se conservan en Mongo, pero no se envian al item.
No se deduce ni envia un `brandId` desde codigos BCI. El GET de tarifas de la
prueba directa alcanzo FDI y obtuvo 504 (timeout upstream), no tarifas.
Esto confirma aceptacion del request de negocio/item, no contratacion ni
correspondencia comercial completa de todos los modelos del Excel.

La integracion queda deshabilitada por defecto mediante
`fdi.homologation.enabled=false`. Sin habilitarla explicitamente devuelve
codigo de negocio 79 y no inicia un deal externo. La activacion del servidor
se autoriza para diagnostico contra QA. No hay fallback al vehiculo fijo.

## Catalogo y origen

- Fuente: archivo Excel `Catalogo homologado Enviar MoneyFY_Fid.xlsx`, hoja
  `Validados`, proporcionado en `D:\MoneyFy` (el nombre original contiene
  acentuacion Unicode).
- SHA256 del XLSX utilizado:
  `9AC9DA5824618815C8B3F2E0B31887D99A798C53164963CEFF6D05DCA950D216`.
- Copia normalizada versionable: `src/main/resources/catalogs/fdi-vehicle-mappings.json`.
- 723 filas, 614 utilizables y 109 sin los tres codigos FDI, deshabilitadas.
- Coleccion Mongo: `fdi_vehicle_mappings`. `_id=portalCode` garantiza una clave
  unica por pareja de codigos del portal; los codigos BCI no se sobrescriben.

| Excel | Persistencia |
|---|---|
| MarcaID_Portal | portalBrandId |
| ModeloID_Portal | portalModelId |
| CODIGO | portalCode / _id |
| MARCA, MODELO, TIPO | brand, model, vehicleType |
| ModeloID (FID) | fdiModelId |
| VersionID (FID) | fdiVersionId |
| TipoID (FID) | fdiTypeId |

La importacion solo se ejecuta con `fdi.homologation.import-enabled=true`
(`FDI_HOMOLOGATION_IMPORT_ENABLED=true`). Valida el catalogo completo antes
de escribir y usa `saveAll` por clave: repetirla actualiza sin duplicar.
No borra registros fuera del archivo ni modifica `brands`.

## Flujo

`POST /quoter/search/plan` conserva autenticacion. Agrega `fdiAddress`, opcional
para BCI pero obligatorio para FDI, junto con un telefono valido del comprador.

1. `aseguradora4`: rama BCI original, resuelve los codigos desde `brands`.
2. `aseguradora5`: recibe `SearchPlanRequest` y el vehiculo de la cotizacion;
   busca en Mongo por marca/modelo normalizados (mayusculas, espacios y tildes).
3. Solo acepta una coincidencia exacta y utilizable; sin ella retorna codigo
   de negocio 78 antes de cualquier llamada HTTP FDI. No usa similitud difusa.
4. Conserva la secuencia POST deal, PUT deal, POST item, GET quotations,
   pero construye el vehiculo con los valores de Mongo y el ano/patente/motor/
   chasis solicitados, no con las constantes 132/2004/2021/CXLS79.
5. Valida contacto antes de crear un deal (error 82 si faltan datos). Normaliza
   RUT a digitos-DV y telefono +569XXXXXXXX a entero 9XXXXXXXX. La direccion
   procede de `fdiAddress`; no se inventan datos de contacto en el software.
   Este contrato simple requiere `ownerRelationOption=0` (comprador propietario)
   y utiliza sus datos para contratante, pagador y asegurado. Otros casos
   requieren un contrato ampliado de personas; se rechazan en vez de confundirlas.

Campos adicionales para FDI (fragmento, no request completo):

```json
{
  "purchaserPhone": "+56911111111",
  "ownerRelationOption": "0",
  "fdiAddress": {
    "streetName": "Direccion Prueba QA",
    "building": 100,
    "countryCode": "15",
    "regionCode": "13",
    "communeCode": "13101"
  }
}
```

Los valores anteriores son ejemplos de certificacion, no defaults de la app.
El frontend debe aportar estos campos para cotizar FDI; sin ellos recibe
error de negocio 82. BCI sigue aceptando el request anterior sin fdiAddress.

El error de negocio se conserva dentro de `QuotationDto`; HTTP 200 por si
solo no indica exito. No se cambian generacion de comisiones ni transacciones.

## Ejemplo capturado en HTTP local: Chevrolet Spark GT, 2018

```json
{
  "vehicle": {
    "modelId": 2058,
    "modelYear": 2018,
    "plateNumber": "TEST12",
    "chasisNumber": "CHASSIS-TEST",
    "engineNumber": "ENGINE-TEST"
  }
}
```

No hay codigo FDI de marca deducido. En la prueba BCI, se capturan
`Int_Marca=4`, `Int_Modelo=1570`, `Int_Ano=2018`, `Int_TipoVehiculo=1` y
`Int_UsoVehiculo=2`. Los dos ultimos son las constantes anteriores del cliente
BCI; **no** se interpreta el `TipoID=101` de FDI como tipo BCI ni se cambia
el uso con este trabajo.

## Pruebas reproducibles

```powershell
docker compose -f local/fdi-mapping/compose.yaml up -d --wait
$env:MONEYFY_LOCAL_MAPPING_MONGO='true'
# Configurar MONEYFY_JWT_SECRET con una clave efimera si se ejecuta la suite de seguridad.
.\mvnw.cmd -B -ntp '-Dtest=VehicleHomologationTest' test
docker compose -f local/fdi-mapping/compose.yaml stop
```

Mongo de pruebas fijo: `127.0.0.1:27019`, base
`moneyfy_fdi_mapping_local_test`, volumen Docker independiente. La prueba
no admite una URI externa y no crea usuarios de negocio ni credenciales.
Los servidores HTTP de test solo enlazan `127.0.0.1` en puertos efimeros.

Verificaciones: importacion doble mantiene 723 documentos, lectura real de
Mongo con 2058/1/101, JSON externo solo con modelId=2058, otro vehiculo con su propio codigo,
homologacion ausente/incompleta/ambigua sin llamadas externas, adaptador
deshabilitado sin llamadas, y BCI sin consultar la homologacion FDI.
El GET de tarifas se simula con 503 para no presentar tarifas inventadas.
Tambien se verifica RUT sin puntos, telefono entero, direcciones completas,
ausencia de versionId/typeId, datos obligatorios ausentes sin llamadas HTTP
y compatibilidad de deserializacion del request BCI anterior.

Resultado 2026-11-11: `package` con las suites aisladas de CORS, seguridad,
comisiones, prototipo BCI y homologacion: **70 pruebas correctas, 0 fallos,
0 errores, 0 omitidas**. No se ejecuto el test de contexto general de la app,
ni una llamada real a las aseguradoras. No se prueba disponibilidad comercial
del ano, modelo o plan.

Validacion adicional de publicacion: snapshot limpio con solo los cambios FDI,
sin el prototipo BCI local: WAR generado y **64 pruebas correctas, 0 fallos,
0 errores, 0 omitidas**, incluyendo importacion y lectura de Mongo real.

## Preparacion del servidor

No hace falta editar `brands`, `users`, `transactions` ni sus identificadores.
La coleccion nueva se crea al ejecutar el importador sobre la base configurada
en el backend; requiere permisos de creacion de coleccion y escritura. `_id`
es la clave `portalCode` y tiene el indice unico automatico de MongoDB.

Tras revisar contrato FDI, respaldo y ambiente de Mongo, el operador puede
habilitar `FDI_HOMOLOGATION_IMPORT_ENABLED=true` en el entorno del proceso
Java para un arranque controlado de la version nueva. Verificar 723 documentos,
614 habilitados y SPARK GT = 2058/1/101; despues quitar/desactivar la opcion de
importacion para evitar sobrescribir ajustes posteriores al reiniciar.
`FDI_HOMOLOGATION_ENABLED=true` controla por separado el consumo del adaptador:
no se debe habilitar en un entorno real sin confirmar el contrato externo.
Si queda en false, las cotizaciones FDI devuelven codigo de negocio 79.

## Diagnostico autorizado en servidor (2026-11-18)

El usuario autoriza activar el adaptador para diagnosticar la integracion FDI
previamente inoperativa; esto no confirma el contrato JSON externo. Se mantiene
la advertencia de compatibilidad pendiente y no se emiten propuestas ni pagos.
Con `FDI_HOMOLOGATION_TRACE_CODES=true`, las trazas `FDI_MAPPING_RESOLVED` y
`FDI_ITEM_REQUEST` contienen exclusivamente codigos: la resolucion muestra
modelo/version/tipo de Mongo y el request muestra solo el modelId enviado.
No incluyen patente, RUT, correo, token ni payload completo.
La primera traza confirma resolucion local; la segunda, preparacion del POST
del item antes de HTTP. Ninguna por si sola prueba aceptacion del proveedor.

Antes de activar se respaldaron base, WAR y configuracion; se cargo solo
`fdi_vehicle_mappings` y se verificaron 723 filas, 614 habilitadas y 109
deshabilitadas. El importador automatico debe permanecer desactivado en este
despliegue porque la carga ya se efectuo de forma controlada.

No se incluye ningun cambio al workflow de despliegue. El push a `eliu` no
integra `master` ni habilita el despliegue configurado solo para esa rama.

## Errores y timeout

- 78: homologacion no utilizable. 79: adaptador deshabilitado.
- 80: respuesta sin planes. 81: respuesta de tarifas no interpretable.
- 82: contacto/direccion ausentes, invalidos o comprador no propietario.
- 72/74/76: fallo al actualizar negocio/crear item/obtener tarifas.
- GET de tarifas incluye `deductible=-1`, como la coleccion FDI proporcionada.
  Timeout de lectura configurable `fdi.qa.quotation-read-timeout-ms` (45 s por
  defecto, limitado a 1-90 s); no hay reintentos automaticos.
- Las excepciones FDI se registran solo con etapa, HTTP y clase: sin URL con
  deal token, cuerpo del proveedor, RUT o correo.
