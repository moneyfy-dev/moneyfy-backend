package com.referidos.app.segurosref.integrations.fdi.clients;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;

import com.referidos.app.segurosref.dtos.quotation.QuotationPlanCoverDto;
import com.referidos.app.segurosref.dtos.quotation.QuotationPlanDto;
import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationDto;
import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationPlanCoverDto;
import com.referidos.app.segurosref.integrations.fdi.dtos.FDIQuotationPlanDto;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIItemCreatePojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIQuoteDealPojo;
import com.referidos.app.segurosref.integrations.fdi.pojos.FDIDealCreatePojo;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpContractorRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIItemCrRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIItemCrVehicleRequest;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpPayerRequest;
import com.referidos.app.segurosref.integrations.fdi.FdiVehicleMappingService;
import com.referidos.app.segurosref.models.FdiVehicleMapping;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.requests.SearchPlanRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FDIQuotationClient {

    private final FdiVehicleMappingService mappings;

    @Value("${fdi.homologation.enabled:false}")
    private boolean homologationEnabled;

    @Value("${fdi.homologation.trace-codes:false}")
    private boolean traceCodes;

    @Value("${fdi.qa.quotation-read-timeout-ms:45000}")
    private int quotationReadTimeoutMs = 45000;

    private RestTemplate restTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    @Value(value = "${fdi.qa.base-url}")
    private String fdiBaseUrl;

    @Value(value = "${fdi.qa.version}")
    private String fdiVersion;

    @Value(value = "${fdi.qa.api-key}")
    private String fdiApiKey;

    @Value(value = "${fdi.qa.broker-id}")
    private String fdiBrokerId;

    public Object[] quoteVehicle(SearchPlanRequest request, QuoterModel quote) {
        if (!homologationEnabled) {
            return new Object[] { 79, null };
        }
        FdiVehicleMapping mapping;
        try {
            // Validar antes de crear un deal externo. No usar coincidencias aproximadas.
            mapping = mappings.resolve(request.brand(), request.model());
            if (traceCodes) {
                LOGGER_MESSAGES.info("FDI_MAPPING_RESOLVED portal=" + mapping.portalCode()
                        + " modelId=" + mapping.fdiModelId() + " versionId=" + mapping.fdiVersionId()
                        + " typeId=" + mapping.fdiTypeId());
            }
        } catch (IllegalArgumentException e) {
            return new Object[] { 78, null };
        }
        if (!validContact(request)) {
            return new Object[] { 82, null };
        }
        // Se inicia y se revisa creación de deal
        FDIDealCreatePojo dealCreateResponse = this.createDeal();
        if (dealCreateResponse.hasError()) {
            return new Object[] { dealCreateResponse.getInternalErrorCode(), null };
        }
        // Se actualiza deal para ingresar información más relevante
        String dealToken = dealCreateResponse.getToken();
        Integer dealUpdateCodeResponse = this.updateDeal(dealToken, request);
        if (dealUpdateCodeResponse != null && dealUpdateCodeResponse != -1) {
            return new Object[] { dealUpdateCodeResponse, null };
        }
        // Se crea item/riesgo asegurable en el deal
        FDIItemCreatePojo itemCreateResponse = this.createItem(dealToken, mapping, request, quote);
        if (itemCreateResponse.hasError()) {
            return new Object[] { itemCreateResponse.getInternalErrorCode(), null };
        }
        // Se realiza la cotización del item asegurable
        FDIQuoteDealPojo quoteDealResponse = this.quoteDeal(dealToken);
        if (quoteDealResponse.hasError()) {
            return new Object[] { quoteDealResponse.getInternalErrorCode(), null };
        }
        // Se realizaron todas las peticiones necesarias para construir el dto de
        // cotización de FDI
        FDIQuotationDto fdiQuotationDto = this.buildFDIQuotationDto(dealToken, quoteDealResponse);
        if (fdiQuotationDto.hasError()) {
            return new Object[] { fdiQuotationDto.getInternalErrorCode(), null };
        }
        // Se creó correctamente el objeto dto de cotización de FDI, ahora se retorna
        // construye objeto que entiende la app
        return this.buildResponseQuotationFDI(fdiQuotationDto);
    }

    // Endpoint para iniciar cotización creando deal
    @SuppressWarnings("null")
    private FDIDealCreatePojo createDeal() {
        try {
            RestTemplate restTemplate = restTemplate();
            // Agregamos datos de cabecera
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            headers.set("brokerIdNumber", fdiBrokerId);
            String urlCreateDeal = fdiBaseUrl + "/deals";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<FDIDealCreatePojo> response = restTemplate.exchange(urlCreateDeal, HttpMethod.POST, entity,
                    FDIDealCreatePojo.class);
            // Revisar si es una respuesta correcta
            if (response.getStatusCode() == HttpStatus.CREATED) {
                FDIDealCreatePojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
            LOGGER_MESSAGES.info("Respuesta no esperada del servicio externo al procesar el deal: "
                    + response.getStatusCode().value());
            return new FDIDealCreatePojo(71);
        } catch (Exception e) {
            logFailure("create-deal", e);
            return new FDIDealCreatePojo(70);
        }
    }

    // Endpoint para actualizar datos relevantes del deal
    @SuppressWarnings("null")
    private Integer updateDeal(String dealToken, SearchPlanRequest request) {
        try {
            RestTemplate restTemplate = restTemplate();
            // Agregamos datos de cabecera y construimos url
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String urlUpdateDeal = fdiBaseUrl + "/deals/" + dealToken;
            // Construcción cuerpo de solicitud
            FDIDealUpRequest requestUpdateDeal = new FDIDealUpRequest(
                    new FDIDealUpContractorRequest(fdiRut(request.purchaserId()), request.purchaserEmail(), request.fdiAddress()),
                    new FDIDealUpPayerRequest(fdiRut(request.purchaserId()), request.purchaserEmail(), phone(request), request.fdiAddress()));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIDealUpRequest> entity = new HttpEntity<>(requestUpdateDeal, headers);
            ResponseEntity<Void> response = restTemplate.exchange(urlUpdateDeal, HttpMethod.PUT, entity, Void.class);
            if (traceCodes) LOGGER_MESSAGES.info("FDI_DEAL_UPDATED status=" + response.getStatusCode().value());
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                return -1; // Éxito
            }
            LOGGER_MESSAGES.info("Respuesta inesperada en actualización: " + response.getStatusCode().value());
            return 73;
        } catch (Exception e) {
            logFailure("update-deal", e);
            return 72;
        }
    }

    // Crear el item asegurable del deal
    @SuppressWarnings("null")
    private FDIItemCreatePojo createItem(String dealToken, FdiVehicleMapping mapping,
            SearchPlanRequest request, QuoterModel quote) {
        try {
            RestTemplate restTemplate = restTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String urlCreateItem = fdiBaseUrl + "/deals/" + dealToken + "/items";
            // Construcción cuerpo de solicitud
            FDIItemCrRequest requestCreateItem = new FDIItemCrRequest(
                    new FDIItemCrVehicleRequest(mapping.fdiModelId(),
                            Integer.valueOf(request.year()), request.ppu().toUpperCase(java.util.Locale.ROOT),
                            quote.getQuoterCarData().getChassisNum(), quote.getQuoterCarData().getEngineNum()),
                    new FDIDealUpPayerRequest(fdiRut(request.purchaserId()), request.purchaserEmail(), phone(request), request.fdiAddress()));
            // Creamos entidad http y realizamos petición
            HttpEntity<FDIItemCrRequest> entity = new HttpEntity<>(requestCreateItem, headers);
            if (traceCodes) {
                LOGGER_MESSAGES.info("FDI_ITEM_REQUEST portal=" + mapping.portalCode()
                        + " modelId=" + mapping.fdiModelId());
            }
            ResponseEntity<FDIItemCreatePojo> response = restTemplate.exchange(urlCreateItem, HttpMethod.POST, entity,
                    FDIItemCreatePojo.class);
            if (traceCodes) LOGGER_MESSAGES.info("FDI_ITEM_RESPONSE portal=" + mapping.portalCode()
                    + " status=" + response.getStatusCode().value());
            if (response.getStatusCode() == HttpStatus.CREATED) {
                FDIItemCreatePojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
            LOGGER_MESSAGES.info("Respuesta inesperada al crear item asegurable: " + response.getStatusCode().value());
            return new FDIItemCreatePojo(75);
        } catch (Exception e) {
            logFailure("create-item", e);
            return new FDIItemCreatePojo(74);
        }
    }

    @SuppressWarnings("null")
    private FDIQuoteDealPojo quoteDeal(String dealToken) {
        try {
            var factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(Math.max(1000, Math.min(90000, quotationReadTimeoutMs)));
            RestTemplate restTemplate = new RestTemplate(factory);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", fdiApiKey);
            headers.set("x-api-version", fdiVersion);
            String urlGetQuotations = fdiBaseUrl + "/deals/" + dealToken + "/items/quotations?deductible=-1";
            // Creamos entidad http y realizamos petición (No hay cuerpo de solicitud)
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<FDIQuoteDealPojo> response = restTemplate.exchange(urlGetQuotations, HttpMethod.GET, entity,
                    FDIQuoteDealPojo.class);
            // Hacer LOG, porque se deshabilitó el endpoint de cotización
            if (response.getStatusCode() == HttpStatus.OK) {
                FDIQuoteDealPojo body = response.getBody();
                body.setInternalErrorCode(-1);
                return body;
            }
            LOGGER_MESSAGES.info("Respuesta no esperada del servicio externo al procesar la cotización del deal: "
                    + response.getStatusCode().value());
            return new FDIQuoteDealPojo(77);
        } catch (Exception e) {
            logFailure("quotations", e);
            return new FDIQuoteDealPojo(76);
        }
    }

    private void logFailure(String stage, Exception error) {
        String status = error instanceof RestClientResponseException response
                ? String.valueOf(response.getStatusCode().value()) : "unavailable";
        LOGGER_MESSAGES.info("FDI_HTTP_FAILURE stage=" + stage + " status=" + status
                + " exception=" + error.getClass().getSimpleName());
    }

    private Integer phone(SearchPlanRequest request) {
        if (request.purchaserPhone() == null || request.purchaserPhone().isBlank()) {
            return null;
        }
        String value = request.purchaserPhone().strip();
        if (value.startsWith("+56")) value = value.substring(3);
        // FDI pide entero; MoneyFy recibe +569XXXXXXXX. No inventar ni truncar telefonos.
        return value.matches("9[0-9]{8}") ? Integer.valueOf(value) : null;
    }

    private String fdiRut(String value) {
        if (value == null) return "";
        String normalized = value.replace(".", "").replace(" ", "").toUpperCase(java.util.Locale.ROOT);
        if (!normalized.contains("-") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1) + "-" + normalized.substring(normalized.length() - 1);
        }
        return normalized;
    }

    private boolean validContact(SearchPlanRequest request) {
        var address = request.fdiAddress();
        return "0".equals(request.ownerRelationOption()) && phone(request) != null
                && fdiRut(request.purchaserId()).matches("[0-9]{7,8}-[0-9K]")
                && request.purchaserEmail() != null && !request.purchaserEmail().isBlank()
                && address != null && address.streetName() != null && !address.streetName().isBlank()
                && address.building() != null && address.building() > 0
                && address.countryCode() != null && address.countryCode().matches("[0-9]+")
                && address.regionCode() != null && address.regionCode().matches("[0-9]+")
                && address.communeCode() != null && address.communeCode().matches("[0-9]+");
    }

    private FDIQuotationDto buildFDIQuotationDto(String dealToken, FDIQuoteDealPojo quoteDealResponse) {
        FDIQuotationDto fdiQuotationDto = new FDIQuotationDto(quoteDealResponse.getInternalErrorCode());
        fdiQuotationDto.setDealToken(dealToken);
        try {
            if (quoteDealResponse.getItems() == null || quoteDealResponse.getItems().isEmpty()) {
                return new FDIQuotationDto(80);
            }
            // Cotizaciones del plan puede tener más de un deducible, si es así, por
            // deducible se crea un nuevo plan
            Set<FDIQuotationPlanDto> plansDto = new HashSet<>();
            FDIQuoteDealPojo.Item itemPojo = quoteDealResponse.getItems().get(0);
            fdiQuotationDto.setItemId(itemPojo.getItemId());
            for (FDIQuoteDealPojo.Detail quotationPojo : itemPojo.getQuotations()) {
                // Como el plan es el mismo y solo cambia dependiendo el deducible, se busca los
                // gastos que cubre que será lo mismo independiente del deducible
                Set<FDIQuotationPlanCoverDto> coversDto = new HashSet<>();
                FDIQuoteDealPojo.Plan planDetailPojo = quotationPojo.getPlan();
                for (FDIQuoteDealPojo.PlanCover coverPojo : planDetailPojo.getCoverages()) {
                    coversDto.add(new FDIQuotationPlanCoverDto(coverPojo.getId(), coverPojo.getName(),
                            coverPojo.getMainDescription(), coverPojo.getGeneralDescription(), coverPojo.getIsMain(),
                            coverPojo.getIsParam(), coverPojo.getValueDescription(), coverPojo.getPolCad(),
                            coverPojo.getValue()));
                }
                // Ahora buscamos los deducibles. Y por deducibles creamos un plan de la
                // cotización específica
                for (FDIQuoteDealPojo.PlanParam parameterPojo : planDetailPojo.getParameters()) {
                    if (parameterPojo.getType().equals("Deducible")) {
                        // Se encuentra el parámetro del deducible, se buscan los deducibles disponibles
                        for (FDIQuoteDealPojo.PlanParamRangeValue valueDeducPojo : parameterPojo.getRanges().get(0)
                                .getValues()) {
                            Integer deductibleUF = (Integer) valueDeducPojo.getValue();
                            String deductibleDesc = "No definido";
                            if (deductibleUF != null) {
                                deductibleDesc = deductibleUF == 0 ? "Sin Deducible"
                                        : "Deducible " + deductibleUF + " UF";
                            }
                            String planId = "FDI_" + quotationPojo.getPlanId();
                            String uniquePlan = planId + "_" + String.valueOf(deductibleUF);
                            Integer totalMonths = 11;
                            BigDecimal grossPlusBrokerage = quotationPojo.getGrossWrittenPremium()
                                    .add(quotationPojo.getBrokerage());
                            BigDecimal monthlyPriceUF = grossPlusBrokerage.divide(BigDecimal.valueOf(totalMonths), 2,
                                    RoundingMode.HALF_UP);
                            BigDecimal monthlyPrice = monthlyPriceUF.multiply(quotationPojo.getValueUf()).setScale(2,
                                    RoundingMode.HALF_UP);
                            plansDto.add(new FDIQuotationPlanDto(uniquePlan, planDetailPojo.getName(), planId,
                                    quotationPojo.getId(), quotationPojo.getFIDId(), quotationPojo.getExpiryDate(),
                                    quotationPojo.getPolicyInceptionDate(), quotationPojo.getPolicyExpiryDate(),
                                    quotationPojo.getPolicyPeriodVigency(), quotationPojo.getNetPremium(),
                                    quotationPojo.getGrossWrittenPremium(), quotationPojo.getBrokerage(),
                                    quotationPojo.getLiabilityAmount(), quotationPojo.getGarageType(),
                                    quotationPojo.getVehicleReplacement(), quotationPojo.getInspectionRequired(),
                                    quotationPojo.getMonthlyPremium(), monthlyPriceUF, monthlyPrice,
                                    quotationPojo.getValueUf(), totalMonths, deductibleUF, deductibleDesc,
                                    BigDecimal.ZERO,
                                    planDetailPojo.getPaymentPlan(), planDetailPojo.getPaymentPipeline(),
                                    planDetailPojo.getQuotationPeriod(), planDetailPojo.getPaymentWay(), coversDto));
                        }
                        break; // Ya se crearon todos los planes por deducible de la cotización, se sigue con
                               // la otra
                    }
                }
            }
            // Entregar error si no se asignaron planes correctamente
            fdiQuotationDto.setPlans(plansDto);
            if (plansDto.size() <= 0) {
                LOGGER_MESSAGES.info("No se han encontrado planes al construir el dto de la cotización de FDI");
                return new FDIQuotationDto(80);
            }
            return fdiQuotationDto;
        } catch (Exception e) {
            logFailure("parse-quotations", e);
            return new FDIQuotationDto(81);
        }
    }

    private Object[] buildResponseQuotationFDI(FDIQuotationDto fdiQuotationDto) {
        List<QuotationPlanDto> plansDto = new ArrayList<>();
        for (FDIQuotationPlanDto planDtoFDI : fdiQuotationDto.getPlans()) {
            // Crear la lista de cobertura para agregarle al plan, antes de crearlo
            Set<QuotationPlanCoverDto> coveragesDto = new HashSet<>();
            for (FDIQuotationPlanCoverDto coverageDtoFDI : planDtoFDI.getCoverages()) {
                coveragesDto.add(new QuotationPlanCoverDto(coverageDtoFDI.getId(), coverageDtoFDI.getName(),
                        coverageDtoFDI.getGeneralDescription(), coverageDtoFDI.getPolCad(), coverageDtoFDI.getValue()));
            }
            String liabilityDesc = planDtoFDI.getLiabilityAmount() != null
                    ? "Hasta " + planDtoFDI.getLiabilityAmount() + " UF entre daño emergente, moral y lucro cesante"
                    : "No definido";
            String garageType = planDtoFDI.getGarageType() != null && !planDtoFDI.getGarageType().isBlank()
                    ? planDtoFDI.getGarageType()
                    : "No definido";

            plansDto.add(new QuotationPlanDto(planDtoFDI.getUniquePlan(), planDtoFDI.getPlanId(),
                    planDtoFDI.getPlanName(), planDtoFDI.getValueUF(), planDtoFDI.getGrossWrittenPremiumUF(),
                    planDtoFDI.getTotalMonths(), planDtoFDI.getMonthlyPriceUF(), planDtoFDI.getMonthlyPrice(),
                    planDtoFDI.getDeductibleUF(), planDtoFDI.getDeductibleDesc(), planDtoFDI.getDiscount(),
                    "Valor comercial", // stolenVehicle
                    "Valor comercial", // totalLoss
                    liabilityDesc, garageType, null, "", "",
                    fdiQuotationDto.getDealToken(), fdiQuotationDto.getItemId(), planDtoFDI.getQuotationId(),
                    planDtoFDI.getFIDId(), planDtoFDI.getExpiryDate(), planDtoFDI.getBrokerageUF(),
                    planDtoFDI.getVehicleReplacement(), planDtoFDI.getInspectionRequired(),
                    planDtoFDI.getMonthlyPremium(), planDtoFDI.getPaymentPlan(), planDtoFDI.getQuotationPeriod(),
                    planDtoFDI.getPaymentWay(), coveragesDto));
        }
        return new Object[] { -1, plansDto };
    }

}
