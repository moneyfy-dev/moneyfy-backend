# Referencia de Configuración del Servidor y Entorno (Producción)

⚠️ **ATENCIÓN AGENTE**: La configuración detallada, rutas exactas, y la gestión de las variables de entorno de producción (Ubuntu + Tomcat 10) contienen información sensible de la arquitectura del servidor. Por motivos de seguridad y limpieza, esa documentación **no** se almacena directamente en este repositorio.

Si necesitas ayudar al usuario a entender, encontrar o editar variables de entorno en el servidor de producción (por ejemplo, para actualizar credenciales, bases de datos o secretos JWT), **debes pedirle al usuario que te proporcione el contenido del siguiente archivo** o consultarlo si tienes acceso directo al sistema de archivos local del usuario:

👉 **Ruta del documento maestro de entorno:**
`D:\wk\useful_data\doc_moneyfy\servidor_entorno\tomcat_env_config.txt`

### Resumen Rápido (Solo como guía de concepto)
- Las variables nativas estrictas de OS (ej. `MONEYFY_JWT_SECRET` leído por `System.getenv`) se configuran en el archivo nativo de entorno de Tomcat.
- Las variables de aplicación (ej. configuraciones de Spring Boot leídas por `@PropertySource`) se configuran en un `.properties` físico y persistente escondido dentro de una carpeta `lib` protegida de Tomcat, para que no sea destruido al actualizar el `.war`.
- Ambos métodos requieren reiniciar el servicio Tomcat (`systemctl restart`) para aplicar los cambios.

*Lee el documento maestro para obtener las rutas y comandos exactos en Ubuntu.*
