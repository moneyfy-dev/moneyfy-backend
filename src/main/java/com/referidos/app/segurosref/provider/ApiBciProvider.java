package com.referidos.app.segurosref.provider;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.dtos.TestPlanDto;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandInsurerModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.pojo.bci.QuoteBciPojo;
import com.referidos.app.segurosref.pojo.bci.QuoteProductBciPojo;
import com.referidos.app.segurosref.pojo.bci.QuoteRateBciPojo;
import com.referidos.app.segurosref.repositories.BrandRepository;

@Component
public class ApiBciProvider {

    @Value(value = "${url.bci.tarifacion}")
    private String urlBCITarifacion;

    @Value(value = "${api.key.bci.tarifacion}")
    private String apiKeyBCITarifacion;

    @Transactional
    public Map<String, Object> getPlansFromBCI(String purchaserId, String brandIdBCI, String modelIdBCI, int year) {
        String errorMessage = "";
        int code = 0;
        String requestBody = "";
        String responseStr = "";
        try {
            // Obtemos el rut del comprador de la póliza sin puntos y sin dv ("12.345.678-9" => "12345678")
            String purchaserIdFormatted = purchaserId.split("-")[0].replace(".", "");
            String purchaserDigit = purchaserId.substring(purchaserId.length()-1);
            RestTemplate restTemplate = new RestTemplate();
            
            // Configuración de encabezados
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Key", apiKeyBCITarifacion);

            // Utilizamos Object Mapper, para ingresar los datos del cuerpo de la solicitud
            ObjectMapper mapper = new ObjectMapper();
            // Elaboración de estructura para los productos para realizar la solicitud
            Map<String, Object> product1 = this.createProductBCI(22000653);
            Map<String, Object> product2 = this.createProductBCI(22000652);
            List<Map<String, Object>> productoMultianual = new ArrayList<>();
            productoMultianual.add(product1);
            productoMultianual.add(product2);
            Map<String, Object> lstProductos = new HashMap<>();
            lstProductos.put("ProductoMultianual", productoMultianual);
            // Elaboración del cuerpo de la solicitud
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("RutCliente", purchaserIdFormatted);
            requestBodyMap.put("DVRutCliente", purchaserDigit);
            requestBodyMap.put("TipoVehiculo", 1); // (1 = Usado, 2 = Nuevo)
            requestBodyMap.put("UsoVehiculo", 2); // (1 = Comercial, 2 = Particular)
            requestBodyMap.put("Compania", "BCI");
            requestBodyMap.put("LstProductos", lstProductos);
            requestBodyMap.put("IdMarca", brandIdBCI);
            requestBodyMap.put("IdModelo", modelIdBCI);
            requestBodyMap.put("AnioVehiculo", year);
            requestBodyMap.put("Edad", 30); // CONSULTAR VALOR POR DEFECTO PARA EDAD
            requestBodyMap.put("Homologa", 1);
            requestBodyMap.put("Usuario", "PRUEBA");
            requestBodyMap.put("Clave", "TEST");
            requestBodyMap.put("FormaPago", 2); // (1 = PAC, 2 = PAT, 3 = Contado, 4 = Aviso de Vencimiento, ejemplo: 2)
            requestBodyMap.put("Descuento", 0); // CONSULTAR VALOR POR DEFECTO PARA DESCUENTO
            requestBodyMap.put("LstIdCoberturasFlexibles", new ArrayList<>());
            requestBodyMap.put("CorredorId", 1163);
            requestBodyMap.put("RutCorredor", "78951950-1");
            requestBodyMap.put("RutEjecutivo", "78951950");
            requestBodyMap.put("NumeroPin", "1");
            requestBodyMap.put("EmiteCotizacion", false);

            // Creamos la solicitud con el cuerpo de la respuesta y los headers, y la realizamos
            requestBody = mapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("null")
            ResponseEntity<QuoteBciPojo> response = restTemplate.exchange(urlBCITarifacion, HttpMethod.POST, requestEntity, QuoteBciPojo.class);
            
            // Si el código de la respuesta es correcto seguimos con la lógica, si no, retornamos un error.
            if(response.getStatusCode() == HttpStatus.OK) {
                QuoteBciPojo quoteBci = mapper.convertValue(response.getBody(), QuoteBciPojo.class);
                if(quoteBci == null) {
                    return Map.of("errorPlanFinder", "10", "errorMessage", "El cuerpo de la respuesta es nulo", "requestBody", requestBody, "responseStr", responseStr); // Opción 9, error: objeto nulo
                }
                if(quoteBci.getError() != null) {
                    errorMessage = "Existe error mapeada de API: " + quoteBci.getError();
                    return Map.of("errorPlanFinder", "11", "errorMessage", errorMessage, "requestBody", requestBody, "responseStr", responseStr); // Opción 10, error: la cotización no se ha podido a llevar a cabo
                }
                // Obtenemos la data principal
                List<TestPlanDto> plans = new ArrayList<>();
                double valueUF = quoteBci.getTasaCambioUF();
                double discount = quoteBci.getDescuento();
                int totalMonths = quoteBci.getCantidadCuotas();
                // Iteramos por cada producto
                for(QuoteProductBciPojo product : quoteBci.getProductos()) {
                    String planName = product.getNombreProducto();
                    // Iteramos por cada tarifa del plan, que varía por el deducible
                    for(QuoteRateBciPojo rate : product.getTarifas()) {
                        String deductibleId = String.valueOf(rate.getIdDeducible());
                        String deductibleDesc = rate.getDescripcionDeducible();
                        double grossPriceUF = rate.getPrimaAnualBruta();
                        double monthlyPriceUF = grossPriceUF / totalMonths;
                        double monthlyPrice = (double) rate.getValorCuotaPesos();
                        // Ajustar deducible a String, como viene de la API
                        int deductible = this.getDeductibleBCI(deductibleId); 
                        deductible = (deductible == -1) ? this.getNoneDetectedDeductible(deductibleDesc) : deductible;
                        // Creamos el id del plan único con el id del tipo de plan y id del deducible
                        String planId = String.valueOf(product.getIdProducto()) + "_" + deductibleId;
                        // FALTA AGREGAR LOS DETALLES DEPENDIENDO DEL PLAN ----
                        TestPlanDto novaPlan = new TestPlanDto(planId, "BCI", planName, valueUF,
                                grossPriceUF, totalMonths, monthlyPriceUF, monthlyPrice, deductible, deductibleDesc,
                                discount, "", "", "", "");
                        plans.add(novaPlan);
                    }
                }
                errorMessage = "Se encontro la aseguradora con los planes";
                return Map.of("errorPlanFinder", "0", "errorMessage", errorMessage, "requestBody", requestBody, "responseStr", responseStr, "plans", plans);
            }
            code = response.getStatusCode().value();
        } catch(JsonProcessingException e) {
            errorMessage = "No se pudo construir el cuerpo de la solicitud: " + e.getMessage() + "\n\n" + e.getCause().getMessage();
            return Map.of("errorPlanFinder", "6", "errorMessage", errorMessage, "requestBody", requestBody, "responseStr", responseStr);
        } catch(RestClientException e) {
            errorMessage = "Error al construir objeto de solicitud: " + e.getMessage() + "\n\n" + e.getCause().getMessage();
            return Map.of("errorPlanFinder", "7", "errorMessage", errorMessage, "requestBody", requestBody, "responseStr", responseStr);
        } catch(Exception e) {
            errorMessage = "No se pudo realizar la consulta: " + e.getMessage() + "\n\n" + e.getCause().getMessage();
            return Map.of("errorPlanFinder", "8", "errorMessage", errorMessage, "requestBody", requestBody, "responseStr", responseStr);
        }
        errorMessage = "El código de error no es correcto: " + code;
        return Map.of("errorPlanFinder", "9", "errorMessage", errorMessage, "requestBody", requestBody, "responseStr", responseStr);
    }

    public String[] findBrandAndModelId(BrandRepository brandRepository, String insurer, String brand, String model) {
        List<BrandModel> brandsDB = brandRepository.findAll();
        String errorMessage = "";
        String brandId = "0";
        String modelId = "0";
        for(BrandModel brandDB : brandsDB) {
            String brandNameDB = brandDB.getBrand();
            // Primero buscamos para saber si existe la marca
            if(brand.equals(brandNameDB)) {
                // Existe la marca, ahora buscamos si la aseguradora tiene el id de la marca para ser cotizada
                boolean isInsurerBrandId = false;
                for(BrandInsurerModel insurerBrandId : brandDB.getInsurersId()) {
                    String insurerNameForBrandIdDB = insurerBrandId.getName();
                    if(insurer.equals(insurerNameForBrandIdDB)) {
                        // Existe el id de la marca en la aseguradora
                        isInsurerBrandId = true;
                        brandId = Integer.toString(insurerBrandId.getId());
                        break;
                    }
                }
                // Consultamos si se encontro el id de la marca en la aseguradora consultante
                if(isInsurerBrandId) {
                    // Existe el id de la marca en la aseguradora, ahora buscamos si existe el modelo
                    for(BrandDataModel modelDB : brandDB.getModels()) {
                        String modelNameDB = modelDB.getModel();
                        if(model.equals(modelNameDB)) {
                            // Existe el modelo, ahora buscamos si existe el id del modelo en la aseguradora
                            for(BrandInsurerModel insurerModelId : modelDB.getInsurersId()) {
                                String insurerNameForModelIdDB = insurerModelId.getName();
                                if(insurer.equals(insurerNameForModelIdDB)) {
                                    // Existe el id del modelo en la aseguradora
                                    modelId = Integer.toString(insurerModelId.getId());
                                    return new String[] {"", "", brandId, modelId};
                                }
                            }
                            errorMessage = "Existe el modelo, pero no se encontro el id del modelo en la aseguradora: " + insurer;
                            return new String[] {"5", errorMessage, "0", "0"};
                        }
                    }
                    errorMessage = "No existe el modelo consultado en la BD: " + model;
                    return new String[] {"4", errorMessage, "0", "0"};
                }
                errorMessage = "Existe la marca, pero no se encontro el id de la marca en la aseguradora: " + insurer;
                return new String[] {"3", errorMessage, "0", "0"};
            }
        }
        errorMessage = "No existe la marca consulta en la BD: " + brand;
        return new String[] {"2", errorMessage, "0", "0"};
    }

    // CREAR LA ESTRUCTURA PARA COTIZAR UN PRODUCTO/PLAN DE BCI
    private Map<String, Object> createProductBCI(long idProduct) {
        Map<String, Object> product = new HashMap<>();
        product.put("PmaId", idProduct);
        product.put("Ncuotas", 11);
        product.put("Vigencia", 12);
        return product;
    }

    private int getDeductibleBCI(String deductibleId) {
        switch (deductibleId) {
            case "1" -> {
                return 0; // UF
            }
            case "2" -> {
                return 3; // UF
            }
            case "3" -> {
                return 5; // UF
            }
            case "4" -> {
                return 7; // UF
            }
            case "5" -> {
                return 10; // UF
            }
            case "6" -> {
                return 20; // UF
            }
            case "7" -> {
                return 15; // UF
            }
            case "497" -> {
                return 25; // UF
            }
            default -> {
                return -1; // UF
            }
        }
    }

    private int getNoneDetectedDeductible(String deductibleDesc) {
        try {
            return Integer.parseInt(deductibleDesc.substring(10, deductibleDesc.length() - 3).strip()); // Ejemplo: "Deducible X UF"
        } catch(Exception e) {
            return -1;
        }
    }

}
