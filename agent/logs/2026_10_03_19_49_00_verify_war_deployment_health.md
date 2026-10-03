# Publicacion verificable del WAR de QA

## Problema observado

El despliegue de la PR #40 termino en verde, pero GET /moneyfy/auth/web/csrf devolvia 404 de Tomcat tanto desde el proxy local como directamente. El workflow buscaba cualquier WAR reciente en /tmp, borraba la aplicacion con Tomcat activo y no fallaba cuando la comprobacion HTTP devolvia 404.

## Correccion del proceso

Se publica el artefacto identificado por commit, se comprueba su integridad antes de parar el servicio y se serializan los despliegues. Tomcat se detiene antes de reemplazar la aplicacion para evitar que el despliegue automatico compita con la limpieza. Se conserva una copia del WAR previo. La comprobacion de disponibilidad exige la respuesta JSON de bootstrap CSRF y espera el arranque, sin mostrar tokens ni credenciales. Si falla, el workflow informa estados y clases de excepciones sanitizadas y termina con error.

No se modifican autenticacion, reglas de negocio, datos ni configuracion de secretos. Esta correccion forma parte de recuperar el backend de QA tras la publicacion autorizada por el usuario. Las pruebas de seguridad existentes se siguen ejecutando antes de crear el WAR. La disponibilidad final debe comprobarse contra la API publicada, no inferirse de la compilacion.
