package com.referidos.app.segurosref.integrations.bci.clients;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.integrations.bci.pojos.BCITokenCreatePojo;
import com.referidos.app.segurosref.integrations.bci.requests.BCITokenCreateRequest;

@Component
public class BCIAuthClient {

    @Value(value = "${bci.qa.base-url}")
    private String bciBaseUrl;

    @Value(value = "${bci.qa.user}")
    private String bciUser;

    @Value(value = "${bci.qa.password}")
    private String bciPassword;

    @SuppressWarnings("null")
    public BCITokenCreatePojo createToken() {
        // Instanciamos el objeto que vamos a retornar al final del flujo
        BCITokenCreatePojo bciPojoResult = new BCITokenCreatePojo();
        // Variables de respaldo por si el exchange falla
        String tokenInBody = "";
        HttpStatusCode statusResponse = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            String urlCreateToken = bciBaseUrl + "/GenerarToken";
            // Construcción de cuerpo de solicitud, entidad y realización de petición
            BCITokenCreateRequest requestCreateToken = new BCITokenCreateRequest(bciUser, bciPassword);
            HttpEntity<BCITokenCreateRequest> entity = new HttpEntity<>(requestCreateToken, headers);
            // Aseguramos la captura de la solicitud convirtiendo la solicitud a String
            ResponseEntity<String> response = restTemplate.exchange(urlCreateToken, HttpMethod.POST, entity,
                    String.class);
            // Captura inmediata de datos reales del servidor
            tokenInBody = response.getBody();
            statusResponse = response.getStatusCode();
            // Seteo inicial en el POJO de control
            bciPojoResult.setResponseBodyStr(tokenInBody);
            bciPojoResult.setStatusResponse(statusResponse);
            bciPojoResult.setStatusOrErrorStr(statusResponse.toString());
            if (statusResponse == HttpStatus.OK) {
                bciPojoResult.setToken(tokenInBody);
                bciPojoResult.setInternalErrorCode(-1);
                return bciPojoResult;
            }
            LOGGER_MESSAGES.info(
                    "Respuesta no esperada al realizar petición para generar token (BCI): " + statusResponse.value());
            return new BCITokenCreatePojo(41, tokenInBody, statusResponse.toString(), statusResponse);
        } catch (HttpStatusCodeException e) {
            // Error HTTP (4xx o 5xx)
            statusResponse = e.getStatusCode();
            bciPojoResult.setResponseBodyStr(e.getResponseBodyAsString());
            bciPojoResult.setStatusOrErrorStr("HTTP Error: " + statusResponse.value() + " - " + e.getStatusText());
            bciPojoResult.setStatusResponse(statusResponse);
            bciPojoResult.setInternalErrorCode(40);
            LOGGER_MESSAGES.info("Error HTTP de BCI | JSON de error: " + bciPojoResult.getResponseBodyStr());
            return bciPojoResult;
        } catch (Exception e) {
            // Error de Parseo o Red, es muy probable que se hayan recuperado datos de respuesta
            bciPojoResult.setStatusOrErrorStr("Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            bciPojoResult.setInternalErrorCode(40);
            // Si la excepción ocurrió antes de obtener respuesta (ej. Timeout o Red), evitamos que quede en null
            if (DataHelper.isNull(bciPojoResult.getResponseBodyStr())) {
                bciPojoResult.setResponseBodyStr("No se alcanzó a obtener respuesta del servidor externo.");
            }
            LOGGER_MESSAGES.info("Excepción en flujo para generar token en servicio externo (BCI): "
                    + bciPojoResult.getStatusOrErrorStr());
            return bciPojoResult;
        }
    }
}
