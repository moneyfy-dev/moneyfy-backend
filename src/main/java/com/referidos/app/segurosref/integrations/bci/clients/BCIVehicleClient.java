package com.referidos.app.segurosref.integrations.bci.clients;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;
import lombok.RequiredArgsConstructor;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.integrations.bci.pojos.BCITokenCreatePojo;
import com.referidos.app.segurosref.integrations.bci.pojos.BCIVehicleResponsePojo;

@Component
@RequiredArgsConstructor
public class BCIVehicleClient {

    private final BCIAuthClient bciAuthClient;

    @Value(value = "${bci.qa.base-url}")
    private String bciBaseUrl;

    @SuppressWarnings("null")
    public BCIVehicleResponsePojo searchVehicle(String ppu) {
        // 1. Obtener Token
        BCITokenCreatePojo tokenCreateResponse = bciAuthClient.createToken();
        if (tokenCreateResponse.hasError()) {
            return new BCIVehicleResponsePojo(tokenCreateResponse.getInternalErrorCode(),
                    tokenCreateResponse.getResponseBodyStr(),
                    tokenCreateResponse.getStatusOrErrorStr(),
                    tokenCreateResponse.getStatusResponse());
        }

        String token = tokenCreateResponse.getToken();

        // 2. Realizar petición de búsqueda de vehículo
        BCIVehicleResponsePojo bciPojoResult = new BCIVehicleResponsePojo();
        ObjectMapper objectMapper = new ObjectMapper();
        String responseJsonRaw = "";
        HttpStatusCode statusResponse = null;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            headers.set(JwtConfig.HEADER_AUTHORIZATION, token);

            String urlSearch = bciBaseUrl + "/DatosVehiculo?str_Patente=" + ppu;
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(urlSearch, HttpMethod.POST, entity, String.class);
            responseJsonRaw = response.getBody();
            statusResponse = response.getStatusCode();

            bciPojoResult.setResponseBodyStr(responseJsonRaw);
            bciPojoResult.setStatusOrErrorStr(statusResponse.toString());
            bciPojoResult.setStatusResponse(statusResponse);

            if (response.getStatusCode() == HttpStatus.OK) {
                if (responseJsonRaw != null) {
                    objectMapper.readerForUpdating(bciPojoResult).readValue(responseJsonRaw);
                }

                // Si boolEstado es false o resultado es null, se considera respuesta no
                // esperada / fallida
                if (bciPojoResult.getBoolEstado() == null || !bciPojoResult.getBoolEstado()
                        || bciPojoResult.getResultado() == null) {
                    bciPojoResult.setInternalErrorCode(47); // BCI_VEHICLE_LOOKUP_UNEXPECTED_RESPONSE
                } else {
                    bciPojoResult.setInternalErrorCode(-1);
                }
                return bciPojoResult;
            }

            LOGGER_MESSAGES.info(
                    "Respuesta no esperada al buscar vehículo en servicio externo (BCI): " + statusResponse.value());
            return new BCIVehicleResponsePojo(47, responseJsonRaw, statusResponse.toString(), statusResponse);

        } catch (HttpStatusCodeException e) {
            statusResponse = e.getStatusCode();
            bciPojoResult.setResponseBodyStr(e.getResponseBodyAsString());
            bciPojoResult.setStatusOrErrorStr("HTTP Error: " + statusResponse.value() + " - " + e.getStatusText());
            bciPojoResult.setStatusResponse(statusResponse);
            bciPojoResult.setInternalErrorCode(46); // BCI_VEHICLE_LOOKUP_EXCEPTION
            LOGGER_MESSAGES
                    .info("Error HTTP de BCI buscar vehículo | JSON de error: " + bciPojoResult.getResponseBodyStr());
            return bciPojoResult;
        } catch (Exception e) {
            bciPojoResult.setStatusOrErrorStr("Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            bciPojoResult.setInternalErrorCode(46); // BCI_VEHICLE_LOOKUP_EXCEPTION
            if (DataHelper.isNull(bciPojoResult.getResponseBodyStr())) {
                bciPojoResult.setResponseBodyStr("No se alcanzó a obtener respuesta del servidor externo.");
            }
            LOGGER_MESSAGES.info(
                    "Excepción al buscar vehículo en servicio externo (BCI): " + bciPojoResult.getStatusOrErrorStr());
            return bciPojoResult;
        }
    }
}
