# Homologacion local de vehiculos FDI

## Estado y limites

Implementado en el worktree local `moneyfy-backend-bci-prototype`, rama `eliu`.
No se modificaron ni publicaron `master`, produccion, Mongo de produccion ni
credenciales. El prototipo BCI de confirmacion de transaccion se conserva
localmente, pero no forma parte del commit de homologacion FDI publicado.

**El contrato HTTP FDI sigue pendiente de confirmar.** El Excel acredita los
valores `ModeloID`, `VersionID`, `TipoID`, pero no los nombres de campos JSON
ni un `brandId` FDI. El adaptador local envia `modelId`, `versionId`, `typeId`
como nombres provisionales. No se envia `MarcaID_Portal` como `brandId` FDI.
Los tests verifican los bytes JSON recibidos por un servidor local, no que
FDI acepte el contrato ni que haya cotizaciones comerciales disponibles.

La integracion queda deshabilitada por defecto mediante
`fdi.homologation.enabled=false`. Sin habilitarla explicitamente devuelve
codigo de negocio 79 y no inicia un deal externo. No publicar esta rama ni
habilitarla en QA real hasta confirmar el esquema y los campos personales
que FDI exige. No hay fallback al vehiculo fijo anterior.

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

`POST /quoter/search/plan` conserva su request y autenticacion actuales.

1. `aseguradora4`: rama BCI original, resuelve los codigos desde `brands`.
2. `aseguradora5`: recibe `SearchPlanRequest` y el vehiculo de la cotizacion;
   busca en Mongo por marca/modelo normalizados (mayusculas, espacios y tildes).
3. Solo acepta una coincidencia exacta y utilizable; sin ella retorna codigo
   de negocio 78 antes de cualquier llamada HTTP FDI. No usa similitud difusa.
4. Conserva la secuencia POST deal, PUT deal, POST item, GET quotations,
   pero construye el vehiculo con los valores de Mongo y el ano/patente/motor/
   chasis solicitados, no con las constantes 132/2004/2021/CXLS79.
5. El proveedor puede rechazar datos obligatorios ausentes. Ya no se inventan
   RUT, correo ni direccion fija para completar el request externo: el correo
   y RUT proceden del comprador, el telefono ausente/incompatible es null y
   la direccion es null porque el request actual de cotizacion no la recibe.
   El mapeo definitivo de asegurado, contratante y pagador requiere el contrato
   FDI; este cambio no acredita un flujo personal/comercial completo.

El error de negocio se conserva dentro de `QuotationDto`; HTTP 200 por si
solo no indica exito. No se cambian generacion de comisiones ni transacciones.

## Ejemplo capturado en HTTP local: Chevrolet Spark GT, 2018

```json
{
  "vehicle": {
    "modelId": 2058,
    "versionId": 1,
    "typeId": 101,
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
Mongo, JSON externo con 2058/1/101, otro vehiculo con su propio codigo,
homologacion ausente/incompleta/ambigua sin llamadas externas, adaptador
deshabilitado sin llamadas, y BCI sin consultar la homologacion FDI.
El GET de tarifas se simula con 503 para no presentar tarifas inventadas.

Resultado 2026-11-11: `package` con las suites aisladas de CORS, seguridad,
comisiones, prototipo BCI y homologacion: **70 pruebas correctas, 0 fallos,
0 errores, 0 omitidas**. No se ejecuto el test de contexto general de la app,
ni una llamada real a las aseguradoras. No se prueba disponibilidad comercial
del ano, modelo o plan.

Validacion adicional de publicacion: snapshot limpio con solo los cambios FDI,
sin el prototipo BCI local: WAR generado y **64 pruebas correctas, 0 fallos,
0 errores, 0 omitidas**, incluyendo importacion y lectura de Mongo real.

## Preparacion del servidor (no ejecutada por esta tarea)

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
`FDI_ITEM_REQUEST` contienen exclusivamente portalCode y codigos de modelo,
version y tipo: no incluyen patente, RUT, correo, token ni payload completo.
La primera traza confirma resolucion local; la segunda, preparacion del POST
del item antes de HTTP. Ninguna por si sola prueba aceptacion del proveedor.

Antes de activar se respaldaron base, WAR y configuracion; se cargo solo
`fdi_vehicle_mappings` y se verificaron 723 filas, 614 habilitadas y 109
deshabilitadas. El importador automatico debe permanecer desactivado en este
despliegue porque la carga ya se efectuo de forma controlada.

No se incluye ningun cambio al workflow de despliegue. El push a `eliu` no
integra `master` ni habilita el despliegue configurado solo para esa rama.
