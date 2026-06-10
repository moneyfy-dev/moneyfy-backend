package com.referidos.app.segurosref.integrations.bci.clients;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.referidos.app.segurosref.integrations.bci.pojos.BCITokenCreatePojo;
import com.referidos.app.segurosref.integrations.bci.requests.BCIQuoteCarProdDetailOwnerRequest;
import com.referidos.app.segurosref.integrations.bci.requests.BCIQuoteCarProdDetailRequest;
import com.referidos.app.segurosref.integrations.bci.requests.BCIQuoteCarProdRequest;
import com.referidos.app.segurosref.integrations.bci.requests.BCIQuoteCarRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.dtos.quotation.QuotationPlanDto;
import com.referidos.app.segurosref.dtos.quotation.QuotationPlanCoverDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.integrations.bci.dtos.BCIQuotationDto;
import com.referidos.app.segurosref.integrations.bci.dtos.BCIQuotationPlanDto;
import com.referidos.app.segurosref.integrations.bci.dtos.BCIQuotationPlanCoverDto;
import com.referidos.app.segurosref.integrations.bci.pojos.BCIQuoteCarPojo;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandInsurerModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.repositories.BrandRepository;

@Component
public class BCIQuotationClient {

    @Autowired
    private BCIAuthClient bciAuthClient;

    @Value(value = "${bci.qa.base-url}")
    private String bciBaseUrl;

    @Transactional
    public Object[] quoteVehicle(int brandIdBCI, int modelIdBCI, int year) {
        // Crear token para realizar cotización y revisar respuesta
        BCITokenCreatePojo tokenCreateResponse = bciAuthClient.createToken();
        if (tokenCreateResponse.hasError()) {
            return new Object[] { tokenCreateResponse.getInternalErrorCode(),
                    Map.of("responseBody", tokenCreateResponse.getResponseBodyStr(), "responseOrError",
                            tokenCreateResponse.getStatusOrErrorStr()),
                    null };
        }
        // Realizar cotización en base al vehículo con token obtenido y revisar
        // respuesta
        String token = tokenCreateResponse.getToken();
        BCIQuoteCarPojo quoteCarResponse = this.quoteCar(token, brandIdBCI, modelIdBCI, year);
        if (quoteCarResponse.hasError()) {
            return new Object[] { quoteCarResponse.getInternalErrorCode(), Map.of("responseBody",
                    quoteCarResponse.getResponseBodyStr(), "responseOrError", quoteCarResponse.getStatusOrErrorStr()),
                    null };
        }
        // No hay error, se construye el DTO de servicio externo (BCI)
        BCIQuotationDto bciQuotationDto = this.buildBCIQuotationDto(quoteCarResponse);
        if (quoteCarResponse.hasError()) {
            return new Object[] { quoteCarResponse.getInternalErrorCode(), Map.of("responseBody", "", "responseOrError",
                    "No se han encontrado planes o ha ocurrido una excepción al construir el dto de la cotización."),
                    null };
        }
        // No hay error se construye objeto final que entiende aplicación interna (es
        // muy posible que se deba ajustar luego de coordinar con cambio de respuesta en
        // estructura).
        return this.buildResponseQuotationBCI(bciQuotationDto);
    }

    // Endpoint para cotizar vehículo (BCI)
    @SuppressWarnings("null")
    private BCIQuoteCarPojo quoteCar(String token, int brandId, int modelId, int year) {
        // Instanciamos el objeto que vamos a retornar al final del flujo
        BCIQuoteCarPojo bciPojoResult = new BCIQuoteCarPojo();
        ObjectMapper objectMapper = new ObjectMapper();
        // Variables de respaldo por si el exchange falla
        String responseJsonRaw = "";
        HttpStatusCode statusResponse = null;
        try {
            // Construcción de headers y url
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            headers.set(JwtConfig.HEADER_AUTHORIZATION, JwtConfig.PREFIX_TOKEN + token);
            String urlQuoteCar = bciBaseUrl + "/Tarificar";
            // Construir cuerpo de solicitud, objeto de entidad http y realizar petición
            BCIQuoteCarRequest requestQuoteCar = new BCIQuoteCarRequest(2, 10221656,
                    0, 12345678, "5", 10221656,
                    List.of(new BCIQuoteCarProdRequest(22000653, 11, 12,
                            List.of(new BCIQuoteCarProdDetailRequest(brandId, modelId, year, 1, 2,
                                    new BCIQuoteCarProdDetailOwnerRequest(12345678, "5", 35))))));
            HttpEntity<BCIQuoteCarRequest> entity = new HttpEntity<BCIQuoteCarRequest>(requestQuoteCar, headers);
            ResponseEntity<String> response = restTemplate.exchange(urlQuoteCar, HttpMethod.POST, entity, String.class);
            // Recuperamos los datos en crudo
            responseJsonRaw = response.getBody();
            statusResponse = response.getStatusCode();
            bciPojoResult.setResponseBodyStr(responseJsonRaw);
            bciPojoResult.setStatusOrErrorStr(statusResponse.toString());
            bciPojoResult.setStatusResponse(statusResponse);
            if (response.getStatusCode() == HttpStatus.OK) {
                if (responseJsonRaw != null) {
                    // Jackson actualiza el objeto existente inyectando los datos del JSON,
                    // manteniendo los setters de auditoría que tenía antes.
                    objectMapper.readerForUpdating(bciPojoResult).readValue(responseJsonRaw);
                }
                bciPojoResult.setInternalErrorCode(-1);
                return bciPojoResult;
            }
            LOGGER_MESSAGES.info("Respuesta no esperada al realizar cotización en servicio externo (BCI): "
                    + statusResponse.value());
            return new BCIQuoteCarPojo(43, responseJsonRaw, statusResponse.toString(), statusResponse);
        } catch (HttpStatusCodeException e) {
            // Manejo de errores 4xx & 5xx
            statusResponse = e.getStatusCode();
            bciPojoResult.setResponseBodyStr(e.getResponseBodyAsString());
            bciPojoResult.setStatusOrErrorStr("HTTP Error: " + statusResponse.value() + " - " + e.getStatusText());
            bciPojoResult.setStatusResponse(statusResponse);
            bciPojoResult.setInternalErrorCode(42);
            LOGGER_MESSAGES.info("Error HTTP de BCI | JSON de error: " + bciPojoResult.getResponseBodyStr());
            return bciPojoResult;
        } catch (Exception e) {
            // Error de Parseo o Red, es muy probable que se hayan recuperado datos de
            // respuesta
            bciPojoResult.setStatusOrErrorStr("Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            bciPojoResult.setInternalErrorCode(42);
            // Si la excepción ocurrió antes de obtener respuesta (ej. Timeout o Red),
            // evitamos que quede en null
            if (DataHelper.isNull(bciPojoResult.getResponseBodyStr())) {
                bciPojoResult.setResponseBodyStr("No se alcanzó a obtener respuesta del servidor externo.");
            }
            LOGGER_MESSAGES.info("Excepción al realizar cotización en servicio externo (BCI): "
                    + bciPojoResult.getStatusOrErrorStr());
            return bciPojoResult;
        }
    }

    // Construir DTO para respuesta de cotización de BCI
    private BCIQuotationDto buildBCIQuotationDto(BCIQuoteCarPojo quoteCarResponse) {
        try {
            BCIQuoteCarPojo.Resultado resultado = quoteCarResponse.getResultado();
            if (resultado == null) {
                LOGGER_MESSAGES.info("No se encontró resultado en la cotización de BCI");
                return new BCIQuotationDto(44);
            }

            Integer idCotizacion = resultado.getIntNroTarificacion();
            String nroCotizacion = resultado.getStrNroCotizacion();
            String vigenciaCotizacion = resultado.getDtFinVigencia();
            BigDecimal tasaCambioUF = resultado.getDecTasaDeCambioUF();
            BigDecimal iva = resultado.getDecIva();

            List<BCIQuoteCarPojo.ProductoWrapper> productos = resultado.getLstProductos();
            if (productos == null || productos.isEmpty()) {
                LOGGER_MESSAGES.info("No se encontraron productos en la cotización de BCI");
                return new BCIQuotationDto(44);
            }

            BCIQuoteCarPojo.ProductoWrapper prodWrapper = productos.get(0);
            BigDecimal tasaInteresCuota = prodWrapper.getDecTasaInteresCuota();
            Integer cantidadCuotas = 0;
            if (prodWrapper.getProducto() != null) {
                cantidadCuotas = prodWrapper.getProducto().getIntCantidadCuotas();
            }

            Integer rutCliente = null;
            String rutDV = null;
            Integer idMarca = null;
            Integer idModelo = null;
            Integer anioVehiculo = null;

            List<BCIQuoteCarPojo.TarifaVehiculo> tarifaVehiculos = prodWrapper.getLstTarifaVehiculos();
            if (tarifaVehiculos != null && !tarifaVehiculos.isEmpty()) {
                BCIQuoteCarPojo.TarifaVehiculo tv = tarifaVehiculos.get(0);
                idMarca = tv.getIntMarca();
                idModelo = tv.getIntModelo();
                anioVehiculo = tv.getIntAno();
                if (tv.getPropietario() != null) {
                    rutCliente = tv.getPropietario().getIntRut();
                    rutDV = tv.getPropietario().getStrDv();
                }
            }

            BCIQuotationDto bciQuotationDto = new BCIQuotationDto(
                    idCotizacion, nroCotizacion, vigenciaCotizacion, tasaCambioUF, iva,
                    rutCliente, rutDV, idMarca, idModelo, anioVehiculo, cantidadCuotas,
                    tasaInteresCuota, null);

            List<BCIQuotationPlanDto> plans = new ArrayList<>();
            for (BCIQuoteCarPojo.ProductoWrapper pw : productos) {
                if (pw.getProducto() == null)
                    continue;
                Integer planId = pw.getProducto().getIntCodigoProducto();
                String planName = pw.getProducto().getNombreProducto();

                List<BCIQuoteCarPojo.TarifaVehiculo> tvs = pw.getLstTarifaVehiculos();
                if (tvs == null || tvs.isEmpty())
                    continue;

                BCIQuoteCarPojo.TarifaVehiculo tv = tvs.get(0);
                if (tv.getLstTarifa() == null)
                    continue;

                String planIdStr = "BCI_" + planId;

                for (BCIQuoteCarPojo.Tarifa t : tv.getLstTarifa()) {
                    Integer deductibleId = t.getIntIdDeducible();
                    Integer deductibleValue = this.getDeductible(deductibleId);
                    String uniqueValue = planIdStr + "_" + deductibleValue;
                    String deductibleDescription = t.getStrDeducible();
                    BigDecimal netValueUF = t.getDecValorNetoUfConInteres();
                    BigDecimal grossValueUF = t.getDecValorBrutoUfConInteres();
                    BigDecimal taxValueUF = t.getDecImpuestoConInteres();
                    BigDecimal monthlyValue = t.getIntValorCuotaPesos();
                    BigDecimal monthlyValueUF = t.getIntValorCuotaUf();

                    plans.add(new BCIQuotationPlanDto(uniqueValue, planIdStr, planName, deductibleValue,
                            deductibleDescription, netValueUF, grossValueUF, taxValueUF, monthlyValue, monthlyValueUF,
                            new java.util.HashSet<>()));
                }
            }

            if (plans.isEmpty()) {
                LOGGER_MESSAGES.info("No se han encontrado planes al construir el dto de la cotización de BCI");
                return new BCIQuotationDto(44);
            }

            bciQuotationDto.setPlans(plans);
            bciQuotationDto.setInternalErrorCode(-1);
            return bciQuotationDto;
        } catch (Exception e) {
            LOGGER_MESSAGES.info("Error de excepción al construir el dto de la cotización de BCI: " + e.getMessage());
            return new BCIQuotationDto(45);
        }
    }

    private Object[] buildResponseQuotationBCI(BCIQuotationDto bciQuotationDto) {
        List<QuotationPlanDto> plansDto = new ArrayList<>();
        for (BCIQuotationPlanDto bciQuotationPlan : bciQuotationDto.getPlans()) {
            Set<QuotationPlanCoverDto> coveragesDto = new java.util.HashSet<>();
            if (bciQuotationPlan.getCoverages() != null) {
                for (BCIQuotationPlanCoverDto coverageDtoBCI : bciQuotationPlan.getCoverages()) {
                    coveragesDto.add(new QuotationPlanCoverDto(
                            coverageDtoBCI.getId(),
                            coverageDtoBCI.getName(),
                            coverageDtoBCI.getGeneralDescription(),
                            coverageDtoBCI.getPolCad(),
                            coverageDtoBCI.getValue()));
                }
            }
            plansDto.add(new QuotationPlanDto(
                    bciQuotationPlan.getUniquePlan(),
                    bciQuotationPlan.getPlanId(),
                    "BCI",
                    bciQuotationPlan.getPlanName(),
                    bciQuotationDto.getTasaCambioUF(),
                    bciQuotationPlan.getGrossValueUF(),
                    bciQuotationDto.getCantidadCuotas(),
                    bciQuotationPlan.getMonthlyPriceUF(),
                    bciQuotationPlan.getMonthlyPrice(),
                    bciQuotationPlan.getDeductible(),
                    bciQuotationPlan.getDeductibleDesc(),
                    BigDecimal.ZERO, // descuento
                    "Valor comercial",
                    "Valor comercial",
                    "Hasta UF 500 entre daño emergente, moral y lucro cesante",
                    "Multimarca",
                    bciQuotationDto.getIdCotizacion(),
                    bciQuotationDto.getVigenciaCotizacion(),
                    "", // dealTokenFDI
                    null, // itemIdFDI
                    null, // quotationIdFDI
                    "", // FIDId
                    "", // expiryDateFDI
                    null, // brokerageUfFDI
                    "", // vehicleReplacementFDI
                    null, // inspectionRequiredFDI
                    null, // monthlyPremiumFDI
                    "", // paymentPlanFDI
                    "", // quotationPeriodFDI
                    "", // paymentWayFDI
                    coveragesDto,
                    new ArrayList<>()));
        }
        return new Object[] { -1, null, plansDto };
    }

    private Integer getDeductible(Integer deductibleId) {
        return (deductibleId == 1) ? 0
                : (deductibleId == 2) ? 3
                        : (deductibleId == 3) ? 5
                                : (deductibleId == 4) ? 7
                                        : (deductibleId == 5) ? 10
                                                : -1;
    }

    // Flujo para realizar la búsqueda de id de modelo y id de marca de aseguradora
    // (en este caso BCI), pero esta ajustado para que sea flexible, porque se envía
    // nombre de aseguradora por parámetro.
    public Object[] findBrandAndModelId(BrandRepository brandRepository, String insurer, String brand, String model) {
        List<BrandModel> brandsDB = brandRepository.findAll();
        String errorMessage = "";
        int brandId = 0;
        int modelId = 0;
        for (BrandModel brandDB : brandsDB) {
            String brandNameDB = brandDB.getBrand();
            // Primero buscamos para saber si existe la marca
            if (brand.equals(brandNameDB)) {
                // Existe la marca, ahora buscamos si la aseguradora tiene el id de la marca
                // para ser cotizada
                boolean isInsurerBrandId = false;
                for (BrandInsurerModel insurerBrandId : brandDB.getInsurersId()) {
                    String insurerNameForBrandIdDB = insurerBrandId.getName();
                    if (insurer.equals(insurerNameForBrandIdDB)) {
                        // Existe el id de la marca en la aseguradora
                        isInsurerBrandId = true;
                        brandId = insurerBrandId.getId();
                        break;
                    }
                }
                // Consultamos si se encontro el id de la marca en la aseguradora consultante
                if (isInsurerBrandId) {
                    // Existe el id de la marca en la aseguradora, ahora buscamos si existe el
                    // modelo
                    for (BrandDataModel modelDB : brandDB.getModels()) {
                        String modelNameDB = modelDB.getModel();
                        if (model.equals(modelNameDB)) {
                            // Existe el modelo, ahora buscamos si existe el id del modelo en la aseguradora
                            for (BrandInsurerModel insurerModelId : modelDB.getInsurersId()) {
                                String insurerNameForModelIdDB = insurerModelId.getName();
                                if (insurer.equals(insurerNameForModelIdDB)) {
                                    // Existe el id del modelo en la aseguradora
                                    modelId = insurerModelId.getId();
                                    return new Object[] { "", "", brandId, modelId };
                                }
                            }
                            errorMessage = "Existe el modelo, pero no se encontro el id del modelo en la aseguradora: "
                                    + insurer;
                            return new Object[] { "5", errorMessage, 0, 0 };
                        }
                    }
                    errorMessage = "No existe el modelo consultado en la BD: " + model;
                    return new Object[] { "4", errorMessage, 0, 0 };
                }
                errorMessage = "Existe la marca, pero no se encontro el id de la marca en la aseguradora: " + insurer;
                return new Object[] { "3", errorMessage, 0, 0 };
            }
        }
        errorMessage = "No existe la marca consulta en la BD: " + brand;
        return new Object[] { "2", errorMessage, 0, 0 };
    }

}
