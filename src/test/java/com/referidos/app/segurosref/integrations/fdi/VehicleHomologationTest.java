package com.referidos.app.segurosref.integrations.fdi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.referidos.app.segurosref.helpers.QuoterHelper;
import com.referidos.app.segurosref.integrations.bci.clients.BCIAuthClient;
import com.referidos.app.segurosref.integrations.bci.clients.BCIQuotationClient;
import com.referidos.app.segurosref.integrations.bci.pojos.BCITokenCreatePojo;
import com.referidos.app.segurosref.integrations.fdi.clients.FDIQuotationClient;
import com.referidos.app.segurosref.integrations.fdi.requests.FDIDealUpAddressRequest;
import com.referidos.app.segurosref.models.*;
import com.referidos.app.segurosref.repositories.*;
import com.referidos.app.segurosref.requests.SearchPlanRequest;
import com.referidos.app.segurosref.seeders.FdiVehicleMappingImporter;
import com.sun.net.httpserver.HttpServer;

class VehicleHomologationTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer http;
    private final List<String> calls = new ArrayList<>();
    private final AtomicReference<String> itemBody = new AtomicReference<>();
    private final AtomicReference<String> bciBody = new AtomicReference<>();
    private final AtomicReference<String> updateBody = new AtomicReference<>();
    private final AtomicReference<String> quotationQuery = new AtomicReference<>();
    private final FdiVehicleMappingRepository mappings = mock(FdiVehicleMappingRepository.class);
    private FdiVehicleMappingService resolver;

    @BeforeEach void setup() throws Exception {
        var catalog = FdiVehicleMappingImporter.readCatalog(json);
        when(mappings.findByBrandKeyAndModelKey(anyString(), anyString())).thenAnswer(invocation ->
                catalog.stream().filter(row -> row.brandKey().equals(invocation.getArgument(0))
                        && row.modelKey().equals(invocation.getArgument(1))).toList());
        resolver = new FdiVehicleMappingService(mappings);
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            calls.add(exchange.getRequestMethod() + " " + path);
            int status = 201;
            String response = "{}";
            if (path.equals("/deals")) {
                response = "{\"token\":\"local-deal\"}";
            } else if (path.equals("/deals/local-deal")) {
                updateBody.set(body);
                status = 204;
            } else if (path.equals("/deals/local-deal/items")) {
                itemBody.set(body);
                response = "{\"itemId\":1}";
            } else if (path.endsWith("/quotations")) {
                quotationQuery.set(exchange.getRequestURI().getRawQuery());
                // Deliberadamente sin tarifas: este test verifica el transporte del request.
                status = 503;
            } else if (path.equals("/Tarificar")) {
                bciBody.set(body);
                status = 503;
            } else {
                status = 404;
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            try (var output = exchange.getResponseBody()) { if (status != 204) output.write(bytes); }
        });
        http.start();
    }

    @AfterEach void stop() { http.stop(0); }

    private String baseUrl() { return "http://127.0.0.1:" + http.getAddress().getPort(); }

    private SearchPlanRequest request(String model) {
        return new SearchPlanRequest("", "TEST12", "CHEVROLET", model, "2018", "aseguradora5",
                "11.111.111-1", "Test", "Local", "Only", "test@example.invalid", "+56911111111", "0", address());
    }

    private FDIDealUpAddressRequest address() {
        return new FDIDealUpAddressRequest("Test only", 100, "15", "13", "13101");
    }

    private QuoterModel quote() {
        return new QuoterHelper().createQuoteStructure(null,
                new QuoterCarModel("TEST12", "CHEVROLET", "SPARK GT", "2018", "CITYCAR", "", "ENGINE-TEST", "CHASSIS-TEST", ""),
                null, "Cotizando", LocalDateTime.now());
    }

    private FDIQuotationClient client(FdiVehicleMappingService mappingService) {
        var client = new FDIQuotationClient(mappingService);
        ReflectionTestUtils.setField(client, "homologationEnabled", true);
        ReflectionTestUtils.setField(client, "fdiBaseUrl", baseUrl());
        ReflectionTestUtils.setField(client, "fdiVersion", "1.0");
        ReflectionTestUtils.setField(client, "fdiApiKey", "local-test-not-a-credential");
        ReflectionTestUtils.setField(client, "fdiBrokerId", "local-test");
        return client;
    }

    private void assertFdiRequest(FdiVehicleMappingService mappingService) throws Exception {
        var result = client(mappingService).quoteVehicle(request("SPARK GT"), quote());
        assertEquals(76, result[0]); // El servidor local simula la falta de tarifas, no un exito real.
        JsonNode vehicle = json.readTree(itemBody.get()).get("vehicle");
        assertEquals(2058, vehicle.get("modelId").asInt());
        assertFalse(vehicle.has("versionId")); // QA rechaza propiedades adicionales.
        assertFalse(vehicle.has("typeId"));
        assertFalse(vehicle.has("brandId")); // El Excel no define una marca FDI.
        assertEquals(2018, vehicle.get("modelYear").asInt());
        assertEquals("TEST12", vehicle.get("plateNumber").asText());
        assertEquals("CHASSIS-TEST", vehicle.get("chasisNumber").asText());
        assertEquals("ENGINE-TEST", vehicle.get("engineNumber").asText());
        assertEquals("test@example.invalid", json.readTree(updateBody.get()).at("/contractor/email").asText());
        assertEquals("11111111-1", json.readTree(updateBody.get()).at("/contractor/idNumber").asText());
        assertEquals("11111111-1", json.readTree(itemBody.get()).at("/policyHolder/idNumber").asText());
        assertEquals(911111111, json.readTree(updateBody.get()).at("/payer/phone").asInt());
        assertEquals("13101", json.readTree(updateBody.get()).at("/payer/address/communeCode").asText());
        assertEquals("15", json.readTree(itemBody.get()).at("/policyHolder/address/countryCode").asText());
        assertEquals("deductible=-1", quotationQuery.get());
        assertEquals(List.of("POST /deals", "PUT /deals/local-deal", "POST /deals/local-deal/items",
                "GET /deals/local-deal/items/quotations"), calls);
    }

    @Test void fdiTransmitsExcelCodesNotBciCodesOrOldConstants() throws Exception {
        assertFdiRequest(resolver);
    }

    @Test void missingMappingPreventsEveryExternalRequest() {
        assertEquals(78, client(resolver).quoteVehicle(request("MODELO INEXISTENTE"), quote())[0]);
        assertTrue(calls.isEmpty());
    }

    @Test void anotherVehicleTransmitsItsOwnMappingAndYear() throws Exception {
        var request = new SearchPlanRequest("", "TEST13", "ALFA ROMEO", "BRERA", "2020", "aseguradora5",
                "11.111.111-1", "Test", "Local", "Only", "test@example.invalid", "+56911111111", "0", address());
        client(resolver).quoteVehicle(request, quote());
        JsonNode vehicle = json.readTree(itemBody.get()).get("vehicle");
        assertEquals(2012, vehicle.get("modelId").asInt());
        assertFalse(vehicle.has("versionId"));
        assertFalse(vehicle.has("typeId"));
        assertEquals(2020, vehicle.get("modelYear").asInt());
        assertEquals("TEST13", vehicle.get("plateNumber").asText());
    }

    @Test void missingContactDataBlocksBeforeCreatingDeal() {
        var request = new SearchPlanRequest("", "TEST12", "CHEVROLET", "SPARK GT", "2018", "aseguradora5",
                "11.111.111-1", "Test", "Local", "Only", "test@example.invalid", "", "0");
        assertEquals(82, client(resolver).quoteVehicle(request, quote())[0]);
        assertTrue(calls.isEmpty());
    }

    @Test void invalidAddressAndPhoneDoNotCreateExternalDeals() {
        var request = new SearchPlanRequest("", "TEST12", "CHEVROLET", "SPARK GT", "2018", "aseguradora5",
                "11.111.111-1", "Test", "Local", "Only", "test@example.invalid", "+56123", "0", address());
        assertEquals(82, client(resolver).quoteVehicle(request, quote())[0]);
        var invalidAddress = new FDIDealUpAddressRequest("Test", 100, "Chile", "13", "Santiago");
        request = new SearchPlanRequest("", "TEST12", "CHEVROLET", "SPARK GT", "2018", "aseguradora5",
                "11.111.111-1", "Test", "Local", "Only", "test@example.invalid", "+56911111111", "0", invalidAddress);
        assertEquals(82, client(resolver).quoteVehicle(request, quote())[0]);
        assertTrue(calls.isEmpty());
    }

    @Test void previousJsonWithoutAddressRemainsDeserializableForBci() throws Exception {
        String body = "{\"brand\":\"CHEVROLET\",\"model\":\"SPARK GT\",\"insurerAlias\":\"aseguradora4\"}";
        var request = json.readValue(body, SearchPlanRequest.class);
        assertEquals("aseguradora4", request.insurerAlias());
        assertNull(request.fdiAddress());
    }

    @Test void incompleteAndAmbiguousMappingsFailClosed() throws Exception {
        var catalog = FdiVehicleMappingImporter.readCatalog(json);
        var incomplete = catalog.stream().filter(row -> !row.enabled()).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(incomplete.brand(), incomplete.model()));
        var spark = resolver.resolve("CHEVROLET", "SPARK GT");
        when(mappings.findByBrandKeyAndModelKey("CHEVROLET", "SPARK GT")).thenReturn(List.of(spark, spark));
        assertEquals(78, client(resolver).quoteVehicle(request("SPARK GT"), quote())[0]);
        assertTrue(calls.isEmpty());
    }

    @Test void adapterDisabledPreventsEveryExternalRequest() {
        var client = client(resolver);
        ReflectionTestUtils.setField(client, "homologationEnabled", false);
        assertEquals(79, client.quoteVehicle(request("SPARK GT"), quote())[0]);
        assertTrue(calls.isEmpty());
    }

    @Test void catalogContains723Rows614EnabledAnd109Incomplete() throws Exception {
        var catalog = FdiVehicleMappingImporter.readCatalog(json);
        assertEquals(723, catalog.size());
        assertEquals(614, catalog.stream().filter(FdiVehicleMapping::usable).count());
        assertEquals(109, catalog.stream().filter(row -> !row.enabled()).count());
        var spark = resolver.resolve(" chevrolet ", "spark   gt");
        assertEquals("4_1570", spark.portalCode());
        assertEquals(2058, spark.fdiModelId());
    }

    @Test void bciStillTransmits4And1570AndExistingTypeAndUse() throws Exception {
        var brand = new BrandModel("CHEVROLET", new ArrayList<>(), new ArrayList<>())
                .addInsurerBrandId(new BrandInsurerModel(4, "BCI"))
                .addModel(new BrandDataModel(new ObjectId(), "SPARK GT", "CITYCAR", new ArrayList<>())
                        .addInsurerModelId(new BrandInsurerModel(1570, "BCI")));
        var brands = mock(BrandRepository.class);
        when(brands.findAll()).thenReturn(List.of(brand));
        var auth = mock(BCIAuthClient.class);
        var token = new BCITokenCreatePojo();
        token.setToken("local-test-token");
        token.setInternalErrorCode(-1);
        when(auth.createToken()).thenReturn(token);
        var bci = new BCIQuotationClient(auth);
        ReflectionTestUtils.setField(bci, "bciBaseUrl", baseUrl());
        var codes = bci.findBrandAndModelId(brands, "BCI", "CHEVROLET", "SPARK GT");
        assertEquals(4, codes[2]);
        assertEquals(1570, codes[3]);
        bci.quoteVehicle((int) codes[2], (int) codes[3], 2018);
        JsonNode vehicle = json.readTree(bciBody.get()).at("/Lst_Productos/0/Lst_Vehiculos/0");
        assertEquals(4, vehicle.get("Int_Marca").asInt());
        assertEquals(1570, vehicle.get("Int_Modelo").asInt());
        assertEquals(1, vehicle.get("Int_TipoVehiculo").asInt());
        assertEquals(2, vehicle.get("Int_UsoVehiculo").asInt());
        assertEquals(2018, vehicle.get("Int_Ano").asInt());
        verifyNoInteractions(mappings); // BCI no consulta la homologacion FDI.
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MONEYFY_LOCAL_MAPPING_MONGO", matches = "true")
    void importsRealLocalMongoIdempotentlyAndTransmitsStoredCodes() throws Exception {
        // Destino fijo de pruebas: no acepta URI ni nombre de base de produccion.
        try (var mongo = MongoClients.create("mongodb://127.0.0.1:27019/?serverSelectionTimeoutMS=5000")) {
            var template = new MongoTemplate(mongo, "moneyfy_fdi_mapping_local_test");
            var repository = new MongoRepositoryFactory(template).getRepository(FdiVehicleMappingRepository.class);
            var importer = new FdiVehicleMappingImporter(repository, json);
            importer.run();
            importer.run();
            assertEquals(723, repository.count());
            assertEquals(2058, repository.findById("4_1570").orElseThrow().fdiModelId());
            assertFdiRequest(new FdiVehicleMappingService(repository));
        }
    }
}
