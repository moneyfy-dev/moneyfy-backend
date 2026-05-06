package com.referidos.app.segurosref.integrations.fdi.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationDto;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIDealPojo;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIUpdateAddressRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIUpdateContractorRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIUpdateDealRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIUpdatePayerRequest;

@Component
public class FDIQuotationClient {

    @Value(value = "${fdi.qa.base-url}")
    private String fdiBaseUrl;

    @Value(value = "${fdi.qa.version}")
    private String fdiVersion;

    @Value(value = "${fdi.qa.api-key}")
    private String fdiApiKey;

    @Value(value = "${fdi.qa.broker-id}")
    private String fdiBrokerId;

    @Value(value = "${fdi.qa.update-endpoint.contractor-email}")
    private String fdiContractorEmail;

    @Value(value = "${fdi.qa.update-endpoint.payer-email}")
    private String fdiPayerEmail;

    public FDIQuotationDto quoteVehicle() {
        // Se inicia y se revisa creación de deal
        FDIDealPojo dealResponse = this.createDeal();
        Integer dealErrorCode = dealResponse.getInternalErrorCode();
        if(dealErrorCode != null && dealErrorCode != -1) {
            return new FDIQuotationDto(dealErrorCode);
        }
        // Se actualiza deal para ingresar información más relevante
        String dealToken = dealResponse.getToken();
        Integer dealUpdateErrorCode = this.updateDeal(dealToken);
        if(dealUpdateErrorCode != null && dealUpdateErrorCode != -1) {
            return new FDIQuotationDto(dealUpdateErrorCode);
        }
        // Se crea item/riesgo asegurable en el deal
        
        return null;
    }

    // Endpoint para iniciar cotización creando deal
    @SuppressWarnings("null")
    private FDIDealPojo createDeal() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Agregamos datos de cabecera
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            headers.set("brokerIdNumber", fdiBrokerId);
            String urlCreateDeal = fdiBaseUrl + "/deals";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<FDIDealPojo> response = restTemplate.exchange(urlCreateDeal, HttpMethod.POST, entity, FDIDealPojo.class);
            // Revisar si es una respuesta correcta
            if(response.getStatusCode() == HttpStatus.CREATED) {
                FDIDealPojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
        } catch(Exception e) {
            return new FDIDealPojo(70);
        }
        return new FDIDealPojo(71);
    }

    // Endpoint para actualizar datos relevantes del deal
    @SuppressWarnings("null")
    private Integer updateDeal(String dealToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Agregamos datos de cabecera y construimos url
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            String urlUpdateDeal = fdiBaseUrl + "/deals/" + dealToken;
            // Construcción cuerpo de solicitud
            FDIUpdateDealRequest requestUpdateDeal = new FDIUpdateDealRequest(
                new FDIUpdateContractorRequest("11.111.111-1", fdiContractorEmail, new FDIUpdateAddressRequest("Calle 00", 1111, "15", "13", "13101")),
                new FDIUpdatePayerRequest("22.222.222-2", fdiPayerEmail, 911111111, new FDIUpdateAddressRequest("Calle 01", 1111, "15", "13", "13101")));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIUpdateDealRequest> entity = new HttpEntity<>(requestUpdateDeal, headers);
            ResponseEntity<Void> response = restTemplate.exchange(urlUpdateDeal, HttpMethod.PUT, entity, Void.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                return -1; // Éxito
            }
        } catch(Exception e) {
            return 72; // Nuevo código: Error de excepción en actualización
        }
        return 73; // Nuevo código: Respuesta inesperada en actualización
    }

}
