package com.referidos.app.segurosref.integrations.fdi.clients;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.referidos.app.segurosref.dtos.quotation.QuotationPlanCoverDto;
import com.referidos.app.segurosref.dtos.quotation.QuotationPlanDto;
import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationDto;
import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationPlanCoverDto;
import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationPlanDto;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIItemCreatePojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuoteDealPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuoteDetailPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuoteItemPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuotePlanCoverPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuotePlanParamPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuotePlanParamRangeValuePojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuotePlanPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIDealCreatePojo;
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

    public Object[] quoteVehicle() {
        // Se inicia y se revisa creación de deal
        FDIDealCreatePojo dealCreateResponse = this.createDeal();
        if(dealCreateResponse.hasError()) {
            return new Object[] {dealCreateResponse.getInternalErrorCode(), null};
        }
        // Se actualiza deal para ingresar información más relevante
        String dealToken = dealCreateResponse.getToken();
        Integer dealUpdateCodeResponse = this.updateDeal(dealToken);
        if(dealUpdateCodeResponse != null && dealUpdateCodeResponse != -1) {
            return new Object[] {dealUpdateCodeResponse, null};
        }
        // Se crea item/riesgo asegurable en el deal
        FDIItemCreatePojo itemCreateResponse = this.createItem(dealToken);
        if(itemCreateResponse.hasError()) {
            return new Object[] {itemCreateResponse.getInternalErrorCode(), null};
        }
        // Se realiza la cotización del item asegurable
        FDIQuoteDealPojo quoteDealResponse = this.quoteDeal(dealToken);
        if(quoteDealResponse.hasError()) {
            return new Object[] {quoteDealResponse.getInternalErrorCode(), null};
        }
        // Se realizaron todas las peticiones necesarias para construir el dto de cotización de FDI
        FDIQuotationDto fdiQuotationDto = this.buildFDIQuotationDto(dealToken, quoteDealResponse);
        if(fdiQuotationDto.hasError()) {
            return new Object[] {fdiQuotationDto.getInternalErrorCode(), null};
        }
        // Se creó correctamente el objeto dto de cotización de FDI, ahora se retorna construye objeto que entiende la app
        return this.buildResponseQuotationFDI(fdiQuotationDto);
    }

    // Endpoint para iniciar cotización creando deal
    @SuppressWarnings("null")
    private FDIDealCreatePojo createDeal() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Agregamos datos de cabecera
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            headers.set("brokerIdNumber", fdiBrokerId);
            String urlCreateDeal = fdiBaseUrl + "/deals";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<FDIDealCreatePojo> response = restTemplate.exchange(urlCreateDeal, HttpMethod.POST, entity, FDIDealCreatePojo.class);
            // Revisar si es una respuesta correcta
            if(response.getStatusCode() == HttpStatus.CREATED) {
                FDIDealCreatePojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
            LOGGER_MESSAGES.info("Respuesta no esperada del servicio externo al procesar el deal: " + response.getStatusCode().value());
            return new FDIDealCreatePojo(71);
        } catch(Exception e) {
            LOGGER_MESSAGES.info("Error de excepción al intentar crear el deal en el servicio externo (FDI): " + e.getMessage());
            return new FDIDealCreatePojo(70);
        }
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
                new FDIDealUpContractorRequest("11111111-1", fdiContractorEmail, new FDIDealUpAddressRequest("Calle 00", 1111, "15", "13", "13101")),
                new FDIDealUpPayerRequest("22222222-2", fdiPayerEmail, 911111111, new FDIDealUpAddressRequest("Calle 01", 1111, "15", "13", "13101")));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIDealUpRequest> entity = new HttpEntity<>(requestUpdateDeal, headers);
            ResponseEntity<Void> response = restTemplate.exchange(urlUpdateDeal, HttpMethod.PUT, entity, Void.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                return -1; // Éxito
            }
            LOGGER_MESSAGES.info("Respuesta inesperada en actualización: " + response.getStatusCode().value());
            return 73;
        } catch(Exception e) {
            LOGGER_MESSAGES.info("Error de excepción en actualización del deal en el servicio externo (FDI): " + e.getMessage());
            return 72;
        }
    }

    // Crear el item asegurable del deal
    @SuppressWarnings("null")
    private FDIItemCreatePojo createItem(String dealToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            String urlCreateItem = fdiBaseUrl + "/deals/" + dealToken + "/items";
            // Construcción cuerpo de solicitud
            FDIItemCrRequest requestCreateItem = new FDIItemCrRequest(
                new FDIItemCrVehicleRequest(132, 2004, 2021, "CXLS79", "132", "2004"),
                new FDIDealUpPayerRequest("22222222-2", fdiPayerEmail, 911111111, new FDIDealUpAddressRequest("Calle 01", 1111, "15", "13", "13101")));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIItemCrRequest> entity = new HttpEntity<>(requestCreateItem, headers);
            ResponseEntity<FDIItemCreatePojo> response = restTemplate.exchange(urlCreateItem, HttpMethod.POST, entity, FDIItemCreatePojo.class);
            if(response.getStatusCode() == HttpStatus.CREATED) {
                FDIItemCreatePojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
            LOGGER_MESSAGES.info("Respuesta inesperada al crear item asegurable: " + response.getStatusCode().value());
            return new FDIItemCreatePojo(75);
        } catch (Exception e) {
            LOGGER_MESSAGES.info("Error de excepción al crear item asegurable: " + e.getMessage());
            return new FDIItemCreatePojo(74);
        }
    }

    @SuppressWarnings("null")
    private FDIQuoteDealPojo quoteDeal(String dealToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            String urlGetQuotations = fdiBaseUrl + "/deals/" + dealToken + "/items/quotations";
            // Creamos entidad http y realizamos petición (No hay cuerpo de solicitud)
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<FDIQuoteDealPojo> response = restTemplate.exchange(urlGetQuotations, HttpMethod.GET, entity, FDIQuoteDealPojo.class);
            // Hacer LOG, porque se deshabilitó el endpoint de cotización
            if(response.getStatusCode() == HttpStatus.OK) {
                FDIQuoteDealPojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
            LOGGER_MESSAGES.info("Respuesta no esperada del servicio externo al procesar la cotización del deal: " + response.getStatusCode().value());
            return new FDIQuoteDealPojo(77);
        } catch (Exception e) {
            LOGGER_MESSAGES.info("Error de excepción al intentar solicitar la cotización final del deal: " + e.getMessage());
            return new FDIQuoteDealPojo(76);
        }
    }

    private FDIQuotationDto buildFDIQuotationDto(String dealToken, FDIQuoteDealPojo quoteDealResponse) {
        FDIQuotationDto fdiQuotationDto = new FDIQuotationDto(quoteDealResponse.getInternalErrorCode());
        fdiQuotationDto.setDealToken(dealToken);
        try {
            // Cotizaciones del plan puede tener más de un deducible, si es así, por deducible se crea un nuevo plan
            Set<FDIQuotationPlanDto> plansDto = new HashSet<>();
            FDIQuoteItemPojo itemPojo = quoteDealResponse.getItems().get(0);
            fdiQuotationDto.setItemId(itemPojo.getItemId());
            for(FDIQuoteDetailPojo quotationPojo : itemPojo.getQuotations()) {
                // Como el plan es el mismo y solo cambia dependiendo el deducible, se busca los gastos que cubre que será lo mismo independiente del deducible
                Set<FDIQuotationPlanCoverDto> coversDto = new HashSet<>();
                FDIQuotePlanPojo planDetailPojo = quotationPojo.getPlan();
                for(FDIQuotePlanCoverPojo coverPojo : planDetailPojo.getCoverages()) {
                    coversDto.add(new FDIQuotationPlanCoverDto(coverPojo.getId(), coverPojo.getName(), coverPojo.getMainDescription(), coverPojo.getGeneralDescription(), coverPojo.getIsMain(), coverPojo.getIsParam(), coverPojo.getValueDescription(), coverPojo.getPolCad(), coverPojo.getValue()));
                }
                // Ahora buscamos los deducibles. Y por deducibles creamos un plan de la cotización específica
                for(FDIQuotePlanParamPojo parameterPojo : planDetailPojo.getParameters()) {
                    if(parameterPojo.getType().equals("Deducible")) {
                        // Se encuentra el parámetro del deducible, se buscan los deducibles disponibles
                        for(FDIQuotePlanParamRangeValuePojo valueDeducPojo : parameterPojo.getRanges().get(0).getValues()) {
                            Integer deductibleUF = (Integer) valueDeducPojo.getValue();
                            String deductibleDesc = "Deducible " + String.valueOf(deductibleUF) + " UF";
                            String sourcePlanId = resolveFDIPlanId(quotationPojo);
                            String uniquePlan = sourcePlanId + "_" + String.valueOf(deductibleUF);
                            Integer totalMonths = 11;
                            Double monthlyPriceUF = (quotationPojo.getGrossWrittenPremium() + quotationPojo.getBrokerage()) / totalMonths;
                            Double monthlyPrice = monthlyPriceUF * quotationPojo.getValueUf();
                            plansDto.add(new FDIQuotationPlanDto(uniquePlan, planDetailPojo.getName(), uniquePlan, quotationPojo.getId(), quotationPojo.getFIDId(), quotationPojo.getExpiryDate(), quotationPojo.getPolicyInceptionDate(), quotationPojo.getPolicyExpiryDate(), quotationPojo.getPolicyPeriodVigency(), quotationPojo.getNetPremium(), quotationPojo.getGrossWrittenPremium(), quotationPojo.getBrokerage(), quotationPojo.getLiabilityAmount(), quotationPojo.getGarageType(), quotationPojo.getVehicleReplacement(), quotationPojo.getInspectionRequired(), quotationPojo.getMonthlyPremium(), monthlyPriceUF, monthlyPrice, quotationPojo.getValueUf(), totalMonths, deductibleUF, deductibleDesc, 0.0, planDetailPojo.getPaymentPlan(), planDetailPojo.getPaymentPipeline(), planDetailPojo.getQuotationPeriod(), planDetailPojo.getPaymentWay(), coversDto));
                        }
                        break; // Ya se crearon todos los planes por deducible de la cotización, se sigue con la otra
                    }
                }
            }
            // Entregar error si no se asignaron planes correctamente
            fdiQuotationDto.setPlans(plansDto);
            if(plansDto.size() <= 0) {
                LOGGER_MESSAGES.info("No se han encontrado planes al construir el dto de la cotización de FDI");
                return new FDIQuotationDto(78);
            }
            return fdiQuotationDto;
        } catch(Exception e) {
            LOGGER_MESSAGES.info("Error de excepción al construir el dto de la cotización de FDI: " + e.getMessage());
            return new FDIQuotationDto(79);
        }
    }

    private String resolveFDIPlanId(FDIQuoteDetailPojo quotationPojo) {
        String planId = quotationPojo.getPlanId();
        if(planId != null && !planId.isBlank() && planId.length() >= 4) {
            return planId;
        }
        if(quotationPojo.getFIDId() != null && !quotationPojo.getFIDId().isBlank()) {
            return "FDI-" + quotationPojo.getFIDId();
        }
        return "FDI-" + String.valueOf(quotationPojo.getId());
    }

    private Object[] buildResponseQuotationFDI(FDIQuotationDto fdiQuotationDto) {
        List<QuotationPlanDto> plansDto = new ArrayList<>();
        for(FDIQuotationPlanDto planDtoFDI : fdiQuotationDto.getPlans()) {
            // Crear la lista de cobertura para agregarle al plan, antes de crearlo
            Set<QuotationPlanCoverDto> coveragesDto = new HashSet<>();
            for(FDIQuotationPlanCoverDto coverageDtoFDI : planDtoFDI.getCoverages()) {
                coveragesDto.add(new QuotationPlanCoverDto(coverageDtoFDI.getId(), coverageDtoFDI.getName(), coverageDtoFDI.getGeneralDescription(), coverageDtoFDI.getPolCad(), coverageDtoFDI.getValue()));
            }
            plansDto.add(new QuotationPlanDto(planDtoFDI.getUniquePlan(), planDtoFDI.getPlanId(), "FDI", planDtoFDI.getPlanName(), planDtoFDI.getValueUF(), planDtoFDI.getGrossWrittenPremiumUF(), planDtoFDI.getTotalMonths(), planDtoFDI.getMonthlyPriceUF(), planDtoFDI.getMonthlyPrice(), planDtoFDI.getDeductibleUF(), planDtoFDI.getDeductibleDesc(), planDtoFDI.getDiscount(), "", "", String.valueOf(planDtoFDI.getLiabilityAmount()), planDtoFDI.getGarageType(), null, "", "", null, fdiQuotationDto.getDealToken(), fdiQuotationDto.getItemId(), planDtoFDI.getQuotationId(), planDtoFDI.getFIDId(), planDtoFDI.getExpiryDate(), planDtoFDI.getBrokerageUF(), planDtoFDI.getVehicleReplacement(), planDtoFDI.getInspectionRequired(), planDtoFDI.getMonthlyPremium(), planDtoFDI.getPaymentPlan(), planDtoFDI.getQuotationPeriod(), planDtoFDI.getPaymentWay(), coveragesDto, new ArrayList<>()));
        }
        return new Object[] {-1, plansDto};
    }

}
