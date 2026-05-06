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
import com.referidos.app.segurosref.integrations.fdi.pojos.FDICreateItemPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuoteDealPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDICreateDealPojo;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpAddressRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpContractorRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIItemCrRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIItemCrVehicleRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpPayerRequest;

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
        FDICreateDealPojo dealCreateResponse = this.createDeal();
        if(dealCreateResponse.hasError()) {
            return new FDIQuotationDto(dealCreateResponse.getInternalErrorCode());
        }
        // Se actualiza deal para ingresar información más relevante
        String dealToken = dealCreateResponse.getToken();
        Integer dealUpdateCodeResponse = this.updateDeal(dealToken);
        if(dealUpdateCodeResponse != null && dealUpdateCodeResponse != -1) {
            return new FDIQuotationDto(dealUpdateCodeResponse);
        }
        // Se crea item/riesgo asegurable en el deal
        FDICreateItemPojo itemCreateResponse = this.createItem(dealToken);
        if(itemCreateResponse.hasError()) {
            return new FDIQuotationDto(itemCreateResponse.getInternalErrorCode());
        }
        // Se realiza la cotización del item asegurable
        FDIQuoteDealPojo quoteDealResponse = this.quoteDeal(dealToken);
        return null;
    }

    // Endpoint para iniciar cotización creando deal
    @SuppressWarnings("null")
    private FDICreateDealPojo createDeal() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Agregamos datos de cabecera
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            headers.set("brokerIdNumber", fdiBrokerId);
            String urlCreateDeal = fdiBaseUrl + "/deals";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<FDICreateDealPojo> response = restTemplate.exchange(urlCreateDeal, HttpMethod.POST, entity, FDICreateDealPojo.class);
            // Revisar si es una respuesta correcta
            if(response.getStatusCode() == HttpStatus.CREATED) {
                FDICreateDealPojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
        } catch(Exception e) {
            return new FDICreateDealPojo(70);
        }
        return new FDICreateDealPojo(71);
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
            FDIDealUpRequest requestUpdateDeal = new FDIDealUpRequest(
                new FDIDealUpContractorRequest("11.111.111-1", fdiContractorEmail, new FDIDealUpAddressRequest("Calle 00", 1111, "15", "13", "13101")),
                new FDIDealUpPayerRequest("22.222.222-2", fdiPayerEmail, 911111111, new FDIDealUpAddressRequest("Calle 01", 1111, "15", "13", "13101")));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIDealUpRequest> entity = new HttpEntity<>(requestUpdateDeal, headers);
            ResponseEntity<Void> response = restTemplate.exchange(urlUpdateDeal, HttpMethod.PUT, entity, Void.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                return -1; // Éxito
            }
        } catch(Exception e) {
            return 72; // Nuevo código: Error de excepción en actualización
        }
        return 73; // Nuevo código: Respuesta inesperada en actualización
    }

    // Crear el item asegurable del deal
    @SuppressWarnings("null")
    private FDICreateItemPojo createItem(String dealToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            String urlCreateItem = fdiBaseUrl + "/deals/" + dealToken + "/items";
            // Construcción cuerpo de solicitud
            FDIItemCrRequest requestCreateItem = new FDIItemCrRequest(
                new FDIItemCrVehicleRequest(132, 2004, 2021, "CXLS79", "132", "2004"),
                new FDIDealUpPayerRequest("22.222.222-2", fdiPayerEmail, 911111111, new FDIDealUpAddressRequest("Calle 01", 1111, "15", "13", "13101")));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIItemCrRequest> entity = new HttpEntity<>(requestCreateItem, headers);
            ResponseEntity<FDICreateItemPojo> response = restTemplate.exchange(urlCreateItem, HttpMethod.POST, entity, FDICreateItemPojo.class);
            if(response.getStatusCode() == HttpStatus.CREATED) {
                FDICreateItemPojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
        } catch (Exception e) {
            return new FDICreateItemPojo(74); // Nuevo código: Error de excepción al crear item asegurable
        }
        return new FDICreateItemPojo(75); // Nuevo código: Respuesta inesperada al crear item asegurable
    }

    private FDIQuoteDealPojo quoteDeal(String dealToken) {
        return null;
    }

}
