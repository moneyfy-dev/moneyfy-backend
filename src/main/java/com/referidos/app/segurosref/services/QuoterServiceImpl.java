package com.referidos.app.segurosref.services;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.dtos.VehicleBrandDto;
import com.referidos.app.segurosref.dtos.VehicleModelDto;
import com.referidos.app.segurosref.dtos.VehicleDto;
import com.referidos.app.segurosref.integrations.bci.clients.BCIVehicleClient;
import com.referidos.app.segurosref.integrations.bci.pojos.BCIVehicleResponsePojo;
import com.referidos.app.segurosref.dtos.quotation.QuotationDto;
import com.referidos.app.segurosref.dtos.quotation.QuotationPlanDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.QuoterHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.integrations.bci.clients.BCIQuotationClient;
import com.referidos.app.segurosref.integrations.email.providers.EmailAppProvider;
import com.referidos.app.segurosref.integrations.fdi.clients.FDIQuotationClient;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.models.PlanModel;
import com.referidos.app.segurosref.models.QuoterAddressModel;
import com.referidos.app.segurosref.models.QuoterCarModel;
import com.referidos.app.segurosref.models.QuoterOwnerModel;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.QuoterPlanModel;
import com.referidos.app.segurosref.models.QuoterPurchaserModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.repositories.PlanRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.responses.enums.BusinessCodeEnum;
import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;
import com.referidos.app.segurosref.validators.QuoterValidator;

@Service
public class QuoterServiceImpl implements QuoterService {

    @Value(value = "${moneyfy.api-key}")
    private String apiKeyMF;

    @Value(value = "${report.commission.cutoff-date}")
    private int commissionCutoffDate;

    @Value(value = "${report.commission.payment-date}")
    private int commissionPaymentDate;

    private final int commissionUserC = 35000;
    private final int commissionUserB = 10000;
    private final int commissionUserA = 5000;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private QuoterValidator quoterValidator;

    @Autowired
    private BCIQuotationClient bciQuotationClient;

    @Autowired
    private BCIVehicleClient bciVehicleClient;

    @Autowired
    private FDIQuotationClient fdiQuotationClient;

    @Autowired
    private EmailAppProvider emailAppProvider;

    @Autowired
    private QuoterHelper quoterHelper;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> searchVehicleBrands(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Error en caso de que el usuario aún no tenga cuentas bancarias registradas
        if (!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked(
                    "debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros",
                    null);
        }
        List<BrandModel> brandsDB = brandRepository.findAll();
        List<VehicleBrandDto> brandsDto = new ArrayList<>();
        // Por cada registro de marcas de vehículos, generamos un objeto dto de la
        // marca, que se le anidan los objetos dto de los modelos de la marca
        for (BrandModel brandDB : brandsDB) {
            List<VehicleModelDto> modelsDto = new ArrayList<>();
            for (BrandDataModel modelDB : brandDB.getModels()) {
                modelsDto.add(new VehicleModelDto(modelDB.getModelId(), modelDB.getModel()));
            }
            brandsDto.add(new VehicleBrandDto(brandDB.getBrandId(), brandDB.getBrand(), modelsDto));
        }
        return ResponseHelper.ok("se ha traido la lista de las marcas de los vehículos disponibles",
                DataHelper.buildUser(userDB, "brands", brandsDto));
    }

    @Transactional
    @Override
    public ResponseEntity<?> searchInsurers(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<String> insurers = new ArrayList<>();
        insurerRepository.findAll().forEach(insurerDB -> {
            insurers.add(insurerDB.getAlias());
        });
        return ResponseHelper.ok("se ha traido la lista de aseguradoras disponibles",
                DataHelper.buildUser(userDB, "insurers", insurers));
    }

    @Transactional
    @Override
    public ResponseEntity<?> searchVehicle(SearchVehicleRequest searchVehicle, String emailAuth) {
        UserModel userDB = this.userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Error en caso de que el usuario aún no tenga cuentas bancarias registradas
        if (!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked(
                    "debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros",
                    null);
        }
        String ppu = searchVehicle.ppu().toUpperCase(); // Patente del vehículo a mayúsculas
        String ownerId = searchVehicle.ownerId().toUpperCase(); // Rut de propietario a mayúsculas por la 'k'

        // Consultar cliente externo BCI para datos del vehículo
        BCIVehicleResponsePojo vehicleResponse = bciVehicleClient.searchVehicle(ppu);

        QuoterCarModel vehicleFound;
        VehicleDto vehicleDto;

        if (vehicleResponse.hasError()) {
            // Error en la API externa o no encontrado: se dejan los campos vacíos
            vehicleFound = new QuoterCarModel(ppu, "", "", "", "", "", "", "", "");
            vehicleDto = new VehicleDto(ppu, "", "", "", "", "", "", "", "", false);
        } else {
            // Búsqueda exitosa
            BCIVehicleResponsePojo.Resultado resultado = vehicleResponse.getResultado();
            vehicleFound = new QuoterCarModel(
                    ppu,
                    resultado.getStrMarca(),
                    resultado.getStrModelo(),
                    String.valueOf(resultado.getIntAnioFabricacion()),
                    resultado.getStrTipoVehiculo(),
                    resultado.getStrColor(),
                    resultado.getStrNumeroMotor(),
                    resultado.getStrNumeroChasis(),
                    "BCI");
            vehicleDto = new VehicleDto(
                    ppu,
                    resultado.getStrMarca(),
                    resultado.getStrModelo(),
                    String.valueOf(resultado.getIntAnioFabricacion()),
                    resultado.getStrTipoVehiculo(),
                    resultado.getStrColor(),
                    resultado.getStrNumeroMotor(),
                    resultado.getStrNumeroChasis(),
                    "BCI",
                    true // isFound = true
            );
        }

        // Buscamos si existe ya existe el registro para volver a cargarlo y no crear
        // duplicidad
        List<QuoterModel> quoters = userDB.getQuoters();
        QuoterModel userQuoter = null;
        String pointOfCurrentStatus = "Iniciando";
        for (QuoterModel quoterDB : quoters) {
            String quoterStatus = quoterDB.getQuoterStatus();
            String quoterOwnerId = quoterDB.getQuoterOwnerData().getPersonalId();
            QuoterCarModel quoterCar = quoterDB.getQuoterCarData();
            if (quoterStatus.equals(pointOfCurrentStatus) && quoterOwnerId.equals(ownerId) &&
                    quoterCar.getPpu().equals(ppu) && quoterCar.getBrand().equals(vehicleFound.getBrand()) &&
                    quoterCar.getModel().equals(vehicleFound.getModel())
                    && quoterCar.getYear().equals(vehicleFound.getYear())) {
                userQuoter = quoterDB;
                break;
            }
        }
        // Si no se encontró registro existente, se crea una nueva cotización
        if (userQuoter == null) {
            QuoterOwnerModel quoterOwner = new QuoterOwnerModel(ownerId, "", "", "");
            QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel("", "", "", "", "", "", "");
            userQuoter = quoterHelper.createQuoteStructure(quoterOwner, vehicleFound, quoterPurchaser,
                    pointOfCurrentStatus, LocalDateTime.now());
            userDB.addQuoter(userQuoter);
            userDB = userRepository.save(userDB);
        }

        Map<String, Object> dataResponse = new java.util.HashMap<>();
        dataResponse.put("vehicle", vehicleDto);
        dataResponse.put("quoterId", userQuoter.getQuoterId());
        if (vehicleResponse.hasError()) {
            dataResponse.put("internalErrorCode", vehicleResponse.getInternalErrorCode());
            dataResponse.put("internalErrorMessage", com.referidos.app.segurosref.responses.enums.BusinessCodeEnum
                    .fromCode(vehicleResponse.getInternalErrorCode()).getErrorDescription());
        }

        return ResponseHelper.created("se ha realizado la cotización exitosamente",
                DataHelper.buildUser(userDB, dataResponse));
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public ResponseEntity<?> searchPlan(SearchPlanRequest searchPlan, String emailAuth) {
        // Si llega, es porque se validaron los datos, por lo tanto, los recuperamos
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Campo opcional, porque se puede realizar una solitud directa sin pasar por la
        // búsqueda de vehículo
        String quoterId = (!DataHelper.isNull(searchPlan.quoterId())) ? searchPlan.quoterId() : "";
        String ppu = searchPlan.ppu().toUpperCase();
        String brand = searchPlan.brand().toUpperCase();
        String model = searchPlan.model().toUpperCase();
        String year = searchPlan.year();
        String insurerAlias = searchPlan.insurerAlias().strip();
        String purchaserId = searchPlan.purchaserId();
        String purchaserName = searchPlan.purchaserName().strip();
        String purchaserPaternalSur = searchPlan.purchaserPaternalSur().strip();
        String purchaserMaternalSur = searchPlan.purchaserMaternalSur().strip();
        String purchaserEmail = searchPlan.purchaserEmail();
        String purchaserPhone = !DataHelper.isNull(searchPlan.purchaserPhone()) ? searchPlan.purchaserPhone() : ""; // Opcional
        String ownerRelationOption = searchPlan.ownerRelationOption(); // Depende de la aseguradora si se usará el campo
        // Intentamos encontrar la cotización, si existe actualizamos los datos, si no
        // existe se crea la cotización
        QuoterModel userQuoter = null;
        List<QuoterModel> quoters = userDB.getQuoters();
        String pointOfQuoterCurrentStatus = "Cotizando";
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (!quoterId.equals("")) {
            for (QuoterModel quoterDB : quoters) {
                String quoterDBId = quoterDB.getQuoterId();
                if (quoterDBId.equals(quoterId)) {
                    if (!quoterDB.getQuoterStatus().equals("Iniciando")
                            && !quoterDB.getQuoterStatus().equals(pointOfQuoterCurrentStatus)) {
                        return ResponseHelper.locked("La cotización se esta procesando", null);
                    }
                    // En caso de que sea una cotización que venga del proceso anterior actualizamos
                    // los datos, recordar que este es un endpoint que se puede repetir como tantas
                    // aseguradoras existan. Se debe actualizar los datos del vehículo, porque puede
                    // que el ingreso se haya realizado manual, pero solo los datos que vengan en la
                    // solicitud, los otros datos que no vienen, deben mantenerse con el valor que
                    // tienen.
                    QuoterCarModel quoterCarDB = quoterDB.getQuoterCarData();
                    if (quoterCarDB != null) {
                        quoterCarDB.setPpu(ppu);
                        quoterCarDB.setBrand(brand);
                        quoterCarDB.setModel(model);
                        quoterCarDB.setYear(year);
                    } else {
                        quoterCarDB = new QuoterCarModel(ppu, brand, model, year, "", "", "", "", "");
                        quoterDB.setQuoterCarData(quoterCarDB);
                    }
                    // Se actualiza la data del comprador de la cotización
                    QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                    quoterPurchaserDB.setPersonalId(purchaserId);
                    quoterPurchaserDB.setName(purchaserName);
                    quoterPurchaserDB.setPaternalSurname(purchaserPaternalSur);
                    quoterPurchaserDB.setMaternalSurname(purchaserMaternalSur);
                    quoterPurchaserDB.setEmail(purchaserEmail);
                    quoterPurchaserDB.setPhone(purchaserPhone);
                    quoterPurchaserDB.setOwnerRelationOption(ownerRelationOption);
                    // Se actualiza el estado actual de la cotización y el usuario para que
                    // persistan los cambios
                    quoterDB.setQuoterStatus(pointOfQuoterCurrentStatus);
                    quoterDB.setUpdatedDate(currentDateTime);
                    userDB = userRepository.save(userDB);

                    userQuoter = quoterDB;
                    break;
                }
            }
        }
        // Si la cotización aún no existe se debe crear
        if (userQuoter == null) {
            // Primero se busca una cotización existente con los datos más relevante del
            // proceso actual, incluyendo el estado
            boolean isQuoter = false;
            for (QuoterModel quoterDB : quoters) {
                QuoterCarModel quoterCarDB = quoterDB.getQuoterCarData();
                QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                if (quoterDB.getQuoterStatus().equals(pointOfQuoterCurrentStatus) && quoterCarDB.getPpu().equals(ppu) &&
                        quoterCarDB.getBrand().equals(brand) && quoterCarDB.getModel().equals(model) &&
                        quoterCarDB.getYear().equals(year) && quoterPurchaserDB.getPersonalId().equals(purchaserId) &&
                        quoterPurchaserDB.getName().equals(purchaserName)
                        && quoterPurchaserDB.getEmail().equals(purchaserEmail)) {
                    userQuoter = quoterDB;
                    isQuoter = true;
                    break;
                }
            }
            // Si la cotización no se encontró con los datos actuales de la solicitud, se
            // crea porque definitivamente no existe
            if (!isQuoter) {
                String vehicleType = "";
                String vehicleColor = "";
                String vehicleMotor = "";
                String vehicleChassis = "";

                try {
                    BCIVehicleResponsePojo vehicleResponse = bciVehicleClient.searchVehicle(ppu);
                    if (vehicleResponse.getInternalErrorCode() == -1 && vehicleResponse.getResultado() != null) {
                        BCIVehicleResponsePojo.Resultado res = vehicleResponse.getResultado();
                        vehicleType = res.getStrTipoVehiculo() != null ? res.getStrTipoVehiculo() : "";
                        vehicleColor = res.getStrColor() != null ? res.getStrColor() : "";
                        vehicleMotor = res.getStrNumeroMotor() != null ? res.getStrNumeroMotor() : "";
                        vehicleChassis = res.getStrNumeroChasis() != null ? res.getStrNumeroChasis() : "";
                    }
                } catch (Exception e) {
                    LOGGER_MESSAGES
                            .info("Excepción al intentar autocompletar datos del vehículo BCI: " + e.getMessage());
                }

                QuoterOwnerModel quoterOwner = new QuoterOwnerModel("", "", "", "");
                QuoterCarModel quoterCar = new QuoterCarModel(ppu, brand, model, year, vehicleType, vehicleColor,
                        vehicleMotor,
                        vehicleChassis, "");
                QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel(purchaserId, purchaserName,
                        purchaserPaternalSur, purchaserMaternalSur, purchaserEmail, purchaserPhone,
                        ownerRelationOption);
                // Creamos nueva cotización y la persistimos
                userQuoter = quoterHelper.createQuoteStructure(quoterOwner, quoterCar, quoterPurchaser,
                        pointOfQuoterCurrentStatus, currentDateTime);
                userDB.addQuoter(userQuoter);
                userDB = userRepository.save(userDB);
            }
        }
        // Ahora entregaremos los planes, dependiendo de la aseguradora, enviando los
        // datos del vehículo verificado.
        List<QuotationPlanDto> planList = new ArrayList<>();
        InsurerModel returnInsurerDB = new InsurerModel(null, "", "");
        returnInsurerDB.setInsurerId(new ObjectId());
        Optional<InsurerModel> insurerOptional = insurerRepository.findByAlias(insurerAlias);
        String errorPlanFinder = "1"; // Error no se encontró una aseguradora para la búsqueda de planes
        String errorMessage = "No se encontro la aseguradora con el alias '" + insurerAlias + "'";
        String requestBody = "";
        String responseStr = "";
        // Si es una consulta a una aseguradora de prueba, se juega con delay para mejor
        // simulación
        if (insurerOptional.isPresent()) {
            returnInsurerDB = insurerOptional.get();
            switch (insurerAlias) {
                case "aseguradora1" -> {
                    planList = quoterHelper.planList1(); // Planes de pruebas
                    errorPlanFinder = "0";
                    errorMessage = "Se encontro la aseguradora con los planes";
                    break;
                }
                case "aseguradora2" -> {
                    try {
                        Thread.sleep(5000);
                        planList = quoterHelper.planList2(); // Planes de pruebas
                        errorPlanFinder = "0";
                        errorMessage = "Se encontro la aseguradora con los planes";
                    } catch (Exception e) {
                        LOGGER_MESSAGES.info("\n-----\nExcepción capturada: " + e.getMessage() + "\n-----");
                    }
                    break;
                }
                case "aseguradora3" -> {
                    try {
                        Thread.sleep(3000);
                        planList = quoterHelper.planList3(); // Planes de pruebas
                        errorPlanFinder = "0";
                        errorMessage = "Se encontro la aseguradora con los planes";
                    } catch (Exception e) {
                        LOGGER_MESSAGES.info("\n-----\nExcepción capturada: " + e.getMessage() + "\n-----");
                    }
                    break;
                }
                case "aseguradora4" -> { // ASEGURADORA 4 == BCI
                    Object[] brandAndModelId = bciQuotationClient.findBrandAndModelId(brandRepository, "BCI", brand,
                            model);
                    errorPlanFinder = (String) brandAndModelId[0];
                    errorMessage = (String) brandAndModelId[1];
                    if (errorPlanFinder.equals("") && errorMessage.equals("")) {
                        // Se pudo encontrar el ids de la aseguradora tanto para consultar por marca y
                        // modelo
                        errorPlanFinder = "0";
                        errorMessage = "Se encontro la aseguradora";
                        int brandId = (int) brandAndModelId[2];
                        int modelId = (int) brandAndModelId[3];
                        Object[] response = bciQuotationClient.quoteVehicle(brandId, modelId, Integer.parseInt(year));
                        int internalErrorCode = (int) response[0];
                        if (internalErrorCode != -1) {
                            // Hay error
                            BusinessCodeEnum enumError = BusinessCodeEnum.fromCode(internalErrorCode);
                            errorPlanFinder = String.valueOf(enumError.getErrorCode());
                            errorMessage = enumError.getErrorDescription();
                            Map<String, String> responseDetailError = (Map<String, String>) response[1];
                            requestBody = responseDetailError.get("responseBody");
                            responseStr = responseDetailError.get("responseOrError");
                        } else {
                            // No hay error
                            planList = (List<QuotationPlanDto>) response[2];
                        }
                    }
                    break;
                }
                case "aseguradora5" -> {
                    // Momentaneó antes del cambio de estructura de esta respuesta
                    errorPlanFinder = "0";
                    errorMessage = "Se encontro la aseguradora";
                    Object[] response = fdiQuotationClient.quoteVehicle();
                    Integer errorCode = (response[0] != null) ? (Integer) response[0] : null;
                    planList = (response[1] != null) ? (List<QuotationPlanDto>) response[1] : planList;
                    if (errorCode != null && errorCode != -1) {
                        BusinessCodeEnum businessCodeEnum = BusinessCodeEnum.fromCode(errorCode);
                        errorPlanFinder = String.valueOf(businessCodeEnum.getErrorCode());
                        errorMessage = businessCodeEnum.getErrorDescription();
                    }
                    break;
                }
            }
        }

        // Guardar planes en BD en caso de no existir
        for (QuotationPlanDto insurerPlan : planList) {
            String insurerPlanId = insurerPlan.getPlanId();
            @SuppressWarnings("null")
            Optional<PlanModel> optionalPlan = planRepository.findById(insurerPlanId);
            if (optionalPlan.isEmpty()) {
                PlanModel novaPlan = new PlanModel(insurerPlanId, insurerPlan.getInsurer(), insurerPlan.getPlanName(),
                        insurerPlan.getDeductibleDesc(), insurerPlan.getStolenVehicle(), insurerPlan.getTotalLoss(),
                        insurerPlan.getDamageThirdParty(), insurerPlan.getWorkshopType(), insurerPlan.getDetails(),
                        currentDateTime, currentDateTime);
                planRepository.save(novaPlan);
            }
        }

        // Actualizar QuoterPlanModel con metadata compartida
        if (planList != null && !planList.isEmpty()) {
            @SuppressWarnings("null")
            QuoterPlanModel quoterPlan = userQuoter.getQuoterPlanData();
            if ("aseguradora4".equals(insurerAlias)) { // BCI
                QuotationPlanDto firstPlan = planList.get(0);
                quoterPlan.setInsurerAlias(insurerAlias);
                quoterPlan.setExternalQuotationId(String.valueOf(firstPlan.getQuotationIdBCI()));
                try {
                    String expiryStr = firstPlan.getExpiryDateBCI();
                    if (expiryStr != null && expiryStr.length() >= 10) {
                        quoterPlan.setExpiryDate(LocalDate.parse(expiryStr.substring(0, 10)));
                    } else {
                        quoterPlan.setExpiryDate(DataHelper.deprecatedDate());
                    }
                } catch (Exception e) {
                    quoterPlan.setExpiryDate(DataHelper.deprecatedDate());
                }
                userRepository.save(userDB);
            } else if ("aseguradora5".equals(insurerAlias)) { // FDI
                QuotationPlanDto firstPlan = planList.get(0);
                quoterPlan.setInsurerAlias(insurerAlias);
                quoterPlan.setExternalQuotationId(String.valueOf(firstPlan.getQuotationIdFDI()));
                quoterPlan.setDealTokenFDI(firstPlan.getDealTokenFDI() != null ? firstPlan.getDealTokenFDI() : "");
                quoterPlan.setItemIdFDI(firstPlan.getItemIdFDI() != null ? firstPlan.getItemIdFDI() : 0);
                try {
                    String expiryStr = firstPlan.getExpiryDateFDI();
                    if (expiryStr != null && expiryStr.length() >= 10) {
                        quoterPlan.setExpiryDate(LocalDate.parse(expiryStr.substring(0, 10)));
                    } else {
                        quoterPlan.setExpiryDate(DataHelper.deprecatedDate());
                    }
                } catch (Exception e) {
                    quoterPlan.setExpiryDate(DataHelper.deprecatedDate());
                }
                userRepository.save(userDB);
            }
        }

        @SuppressWarnings("null")
        QuotationDto quotationDto = new QuotationDto(userQuoter.getQuoterId(), errorPlanFinder, errorMessage,
                requestBody, responseStr, returnInsurerDB, planList);
        return ResponseHelper.ok("se ha realizado la cotización", quotationDto);
    }

    @Override
    public ResponseEntity<?> selectPlan(SelectPlanRequest planSelected, String emailAuth) {
        // Los datos del plan seleccionado ya han sido validados
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<QuoterModel> quoters = userDB.getQuoters();
        String quoterId = planSelected.quoterId();
        String pointOfQuoterCurrentStatus = "Recopilando";
        // Buscamos al cotizador mediante al id y por el estado del flujo anterior o el
        // actual, en caso de que el
        // usuario desee cambiar de plan y datos de inspección
        for (QuoterModel quoterDB : quoters) {
            String quoterStatusDB = quoterDB.getQuoterStatus();
            String quoterDBId = quoterDB.getQuoterId();
            if ((quoterStatusDB.equals("Cotizando") || quoterStatusDB.equals(pointOfQuoterCurrentStatus)) &&
                    quoterId.equals(quoterDBId)) {
                // Se encontró la cotización por lo tanto se pueden obtener los datos y seguir
                // con el flujo
                QuoterOwnerModel quoterOwner = quoterDB.getQuoterOwnerData();
                quoterOwner.setName(planSelected.ownerName().strip()); // Usamos strip() para quitar espacios al inicio
                                                                       // y final
                quoterOwner.setPaternalSurname(planSelected.ownerPaternalSur().strip());
                quoterOwner.setMaternalSurname(planSelected.ownerMaternalSur().strip());
                // Actualizamos el plan seleccionado del cotizador
                QuoterPlanModel quoterPlan = quoterDB.getQuoterPlanData();
                quoterPlan.setQuoterPlanId(planSelected.planId());
                quoterPlan.setInsurer(planSelected.insurer().strip());
                quoterPlan.setPlanName(planSelected.planName().strip());
                quoterPlan.setValueUF(planSelected.valueUF());
                quoterPlan.setGrossPriceUF(planSelected.grossPriceUF());
                quoterPlan.setTotalMonths(planSelected.totalMonths());
                quoterPlan.setMonthlyPriceUF(planSelected.monthlyPriceUF());
                quoterPlan.setMonthlyPrice(planSelected.monthlyPrice());
                quoterPlan.setDeductibleDesc(planSelected.deductibleDesc());
                quoterPlan.setDiscount(planSelected.discount());

                // Actualizamos la dirección de la cotización para la inspección
                QuoterAddressModel quoterAddress = quoterDB.getQuoterAddressData();
                quoterAddress.setStreet(planSelected.street().strip());
                quoterAddress.setStreetNumber(planSelected.streetNumber().strip());
                quoterAddress.setDepartment(
                        (!DataHelper.isNull(planSelected.department())) ? planSelected.department().strip() : "");
                // Actualizamos el estado del flujo, la fecha de actualización del cotizador y
                // la base de datos.
                quoterDB.setQuoterStatus(pointOfQuoterCurrentStatus);
                quoterDB.setUpdatedDate(LocalDateTime.now());
                userDB = userRepository.save(userDB);
                return ResponseHelper.ok("se ha seleccionado el plan de la cotización",
                        DataHelper.buildUser(userDB, "quoterId", quoterId));
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    @Override
    public ResponseEntity<?> generateTransaction(GenerateTransactionRequest generateTransaction, String emailAuth,
            String requestEndpoint) {
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userCId = userC.getUserId();
        String quoterId = generateTransaction.quoterId();
        quoterId = (!DataHelper.isNull(quoterId) && ObjectId.isValid(quoterId)) ? quoterId : "";
        if (!quoterId.equals("")) {
            // El id del cotizador cumple con el formato, para buscar un registro específico
            List<QuoterModel> quoters = userC.getQuoters();
            for (QuoterModel quoterDB : quoters) {
                String quoterIdDB = quoterDB.getQuoterId();
                String quoterStatusDB = quoterDB.getQuoterStatus();
                if (quoterId.equals(quoterIdDB) && quoterStatusDB.equals("Recopilando")) {
                    if (transactionRepository.existsByUserIdAndQuoterId(userCId, quoterId)) {
                        return ResponseHelper.gone("transacción existente que está siendo procesada", null);
                    }
                    // Se comienza a generar la transacción con las comisiones debidas
                    String transactionId = new ObjectId().toString();
                    int commissionScope = 1; // Comienzo de nivel encontrado para entregar comisiones
                    int commissionTotal = commissionUserC; // Comienzo de la comisión total que se lleva la venta
                    String pointOfCurrentStatus = "Pendiente";
                    LocalDateTime currentDateTime = LocalDateTime.now();
                    TransactionModel novaTransaction = quoterHelper.generateNovaTransactionStructure(transactionId,
                            userCId, quoterDB, pointOfCurrentStatus, commissionTotal, commissionScope,
                            "La comisión está siendo procesada", currentDateTime);
                    List<UserModel> users = new ArrayList<>(); // Usuarios que se tienen que actualizar por el ajuste de
                                                               // la wallet
                    // Comenzamos a actualizar la data de la wallet del usuario.
                    WalletModel walletC = userC.getWallet();
                    walletC.setOutstandingBalance(walletC.getOutstandingBalance() + commissionUserC);
                    walletC.setTotalBalance(walletC.getOutstandingBalance() + walletC.getAvailableBalance());
                    users.add(userC);
                    // Ver si existe el userB y userA, para ajustar transacción
                    String emailUserB = "";
                    String emailUserA = "";
                    String message = "";
                    try {
                        // IMPORTANTE: Se busca un userB que haya referido al userC, para agregar la
                        // comisión correspondiente.
                        // Si el usuario que está refiriendo está activado, tiene que haber un registro
                        // en la colección de
                        // 'users', si no se encuentra se maneja con una respuesta errada con try/catch.
                        Optional<ReferredModel> referredByUserB = referredRepository.findByReferred(emailAuth);
                        if (referredByUserB.isPresent()
                                && referredByUserB.get().getUserReferringStatus().equals("Activado")) {
                            // Buscamos el usuario referidor y actualizamos wallet
                            emailUserB = referredByUserB.get().getUserReferring();
                            UserModel userB = userRepository.findByPersonalData_Email(emailUserB).orElseThrow();
                            WalletModel walletB = userB.getWallet();
                            walletB.setOutstandingBalance(walletB.getOutstandingBalance() + commissionUserB);
                            walletB.setTotalBalance(walletB.getOutstandingBalance() + walletB.getAvailableBalance());
                            users.add(userB);
                            // Ajustamos valores de transacción y agregamos comisión
                            commissionScope = 2;
                            commissionTotal += commissionUserB;
                            novaTransaction.addCommission(new TransactionComissionModel(userB.getUserId(),
                                    commissionUserB, pointOfCurrentStatus));
                            // IMPORTANTE: Se busca un userA que haya referido al userB, para agregar la
                            // comisión correspondiente.
                            Optional<ReferredModel> referredByUserA = referredRepository.findByReferred(emailUserB);
                            if (referredByUserA.isPresent()
                                    && referredByUserA.get().getUserReferringStatus().equals("Activado")) {
                                // Buscamos el usuario referidor y actualizamos wallet
                                emailUserA = referredByUserA.get().getUserReferring();
                                UserModel userA = userRepository.findByPersonalData_Email(emailUserA).orElseThrow();
                                WalletModel walletA = userA.getWallet();
                                walletA.setOutstandingBalance(walletA.getOutstandingBalance() + commissionUserA);
                                walletA.setTotalBalance(
                                        walletA.getOutstandingBalance() + walletA.getAvailableBalance());
                                users.add(userA);
                                // Ajustamos valores de transacción y agregamos comisión
                                commissionScope = 3;
                                commissionTotal += commissionUserA;
                                novaTransaction.addCommission(new TransactionComissionModel(userA.getUserId(),
                                        commissionUserA, pointOfCurrentStatus));
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // En caso de haber excepción, seguimos ya que no se alcanza a actualizar ningún
                        // dato esencial y entregamos mensaje de excepción
                        String referredNotFound = (novaTransaction.getCommissionScope() == 1) ? emailUserB : emailUserA;
                        message = "Ha ocurrido una excepción en la transacción N°" + transactionId
                                + ", el referido no fue encontrado: " + referredNotFound + "\n" + e.getMessage();
                        novaTransaction.setUserReferringFound(false);
                    }
                    // Se actualiza el nivel de comisiones que se alcanzo a entregar la transacción
                    // (referidos).
                    novaTransaction.setCommissionScope(commissionScope);
                    novaTransaction.setCommissionTotal(commissionTotal);
                    // Se actualizan el estado, fecha de actualización y se envía el detalle del
                    // plan que se está cotizando al usuario
                    quoterDB.setQuoterStatus(pointOfCurrentStatus);
                    quoterDB.setUpdatedDate(currentDateTime);
                    emailAppProvider.sendQuoteDetails(userC, quoterDB);
                    // Guardamos en la base de datos y retornamos el usuario de la consulta (userC),
                    // id del cotizador, y id de la transacción
                    userRepository.saveAll(users);
                    transactionRepository.save(novaTransaction);
                    Map<String, Object> data = Map.of("quoterId", quoterId, "transactionId", transactionId, "message",
                            message);
                    return ResponseHelper.ok("la trasacción se ha realizado correctamente",
                            DataHelper.buildUser(userC, data));
                }
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    @Override
    public ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, String emailAuth,
            String requestEndpoint) {
        // Obtenemos la data del cuerpo de la solicitud y corroboramos que sea correcta
        String quoterId = finalizeQuote.quoterId();
        String pointOfTransactionStatus = finalizeQuote.transactionStatus();
        if (DataHelper.isNull(quoterId) || !ObjectId.isValid(quoterId) || DataHelper.isNull(pointOfTransactionStatus) ||
                (!pointOfTransactionStatus.equals("Aprobado") && !pointOfTransactionStatus.equals("Rechazado") &&
                        !pointOfTransactionStatus.equals("Caducado"))) {
            return ResponseHelper.failedDependency("la data proporcionada no es correcta", "failed dependency");
        }
        // Buscamos un cotizador del usuario con el mismo id y que tenga el estado del
        // último flujo "Pendiente"
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        Map<String, Object> returnData = new HashMap<>();
        String transactionId = "";
        String message = "";
        for (QuoterModel quoterDB : userC.getQuoters()) {
            String quoterIdDB = quoterDB.getQuoterId();
            String quoterStatusDB = quoterDB.getQuoterStatus();
            if (quoterId.equals(quoterIdDB) && quoterStatusDB.equals("Pendiente")) {
                // Se intenta cerrar la venta, dependiendo del estado entregado
                TransactionModel transactionDB = transactionRepository
                        .findByUserIdAndQuoterIdAndStatus(userC.getUserId(), quoterId, "Pendiente").orElseThrow();
                if (!transactionDB.getUserReferringFound()) {
                    return ResponseHelper.failedDependency(
                            "Se necesita revisar transacción por referidor no encontrado", "failed dependency");
                }
                transactionId = transactionDB.getTransactionId();
                int commissionScope = transactionDB.getCommissionScope();
                boolean isTrasactionApproved = pointOfTransactionStatus.equals("Aprobado");
                LocalDateTime currentDateTime = LocalDateTime.now();
                List<UserModel> updateUsers = new ArrayList<>();
                // Obtenemos la wallet del usuario C, para comenzar con la actualización.
                WalletModel walletC = userC.getWallet();
                int outstandingBalanceC = walletC.getOutstandingBalance() - commissionUserC;
                walletC.setOutstandingBalance(outstandingBalanceC);
                // Actualizamos el dinero disponible dependiendo del estado de la transacción
                int availableBalanceC = walletC.getAvailableBalance();
                walletC.setAvailableBalance(
                        (isTrasactionApproved) ? (availableBalanceC + commissionUserC) : availableBalanceC);
                availableBalanceC = walletC.getAvailableBalance();
                // Actualizamos el saldo total
                walletC.setTotalBalance(outstandingBalanceC + availableBalanceC);
                updateUsers.add(userC);
                try {
                    // IMPORTANTE: Se busca un userB que haya referido al userC, para actualizar la
                    // comisión correspondiente,
                    // siempre y cuando confirmemos con el campo 'commissionScope'
                    if (commissionScope > 1) {
                        // La comisión alcanza a un referido y por lo tanto se encuentra 'Activado'
                        ReferredModel referredByUserB = referredRepository.findByReferred(emailAuth).orElseThrow();
                        String emailUserB = referredByUserB.getUserReferring();
                        String codeToReferB = referredByUserB.getCodeToRefer();
                        UserModel userB = userRepository
                                .findByPersonalData_EmailAndCodeToRefer(emailUserB, codeToReferB).orElseThrow();
                        // Actualizamos los valores de la wallet del usuario B
                        WalletModel walletB = userB.getWallet();
                        int outstandingBalanceB = walletB.getOutstandingBalance() - commissionUserB;
                        walletB.setOutstandingBalance(outstandingBalanceB);
                        // Actualizamos el dinero disponible dependiendo del estado de la transacción
                        int availableBalanceB = walletB.getAvailableBalance();
                        walletB.setAvailableBalance(
                                (isTrasactionApproved) ? (availableBalanceB + commissionUserB) : availableBalanceB);
                        availableBalanceB = walletB.getAvailableBalance();
                        // Actualizamos el saldo total
                        walletB.setTotalBalance(outstandingBalanceB + availableBalanceB);
                        updateUsers.add(userB);
                        // IMPORTANTE: Se busca un userA en caso de que el alcance de comisión sea mayor
                        // a 2
                        if (commissionScope > 2) {
                            // La comisión alcanzo a otro referido y por lo tanto se encuentra 'Activado'
                            ReferredModel referredByUserA = referredRepository.findByReferred(emailUserB).orElseThrow();
                            String emailUserA = referredByUserA.getUserReferring();
                            String codeToReferA = referredByUserA.getCodeToRefer();
                            UserModel userA = userRepository
                                    .findByPersonalData_EmailAndCodeToRefer(emailUserA, codeToReferA).orElseThrow();
                            // Actualizamos los valores de la wallet del usuario A
                            WalletModel walletA = userA.getWallet();
                            int outstandingBalanceA = walletA.getOutstandingBalance() - commissionUserA;
                            walletA.setOutstandingBalance(outstandingBalanceA);
                            // Actualizamos el dinero disponible dependiendo del estado de la transacción
                            int availableBalanceA = walletA.getAvailableBalance();
                            walletA.setAvailableBalance(
                                    (isTrasactionApproved) ? (availableBalanceA + commissionUserA) : availableBalanceA);
                            availableBalanceA = walletA.getAvailableBalance();
                            // Actualizamos el saldo total
                            walletA.setTotalBalance(outstandingBalanceA + availableBalanceA);
                            updateUsers.add(userA);
                        } // En caso que haya usuario A
                    } // En caso que haya usuario B
                } catch (NoSuchElementException e) {
                    // En caso de haber excepción, identificamos donde se dió la excepción durante
                    // la búsqueda del usuario referidor A o B, teniendo en consideración el obj
                    // para actualizar los usuarios y entregamos el mensaje informativo
                    int updateUsersSize = updateUsers.size();
                    message = "Ha ocurrido un excepción en la transacción N°" + transactionId
                            + ", el alcance de la comisión es " + String.valueOf(commissionScope) + ", y ";
                    if (updateUsersSize == 1) { // No se pudo encontrar userB
                        message += "no se ha podido encontrar el usuario referidor B";
                        message += (commissionScope == 2) ? message : " y por lo tanto, tampoco el usuario referidor A";
                    } else { // No se pudo encontrar userA
                        message += "no se ha podido encontrar el usuario referidor A";
                    }
                }
                // Se ajustan data general
                quoterDB.setQuoterStatus(pointOfTransactionStatus);
                quoterDB.setUpdatedDate(currentDateTime);
                transactionDB.setStatus(pointOfTransactionStatus);
                transactionDB.setUpdatedDate(currentDateTime);
                transactionDB
                        // Se actualiza la fecha de aprobación solo si la transacción fue aprobada
                        .setApprovalDate((isTrasactionApproved) ? currentDateTime : transactionDB.getApprovalDate());
                returnData.put("quoterId", quoterId);
                returnData.put("transactionId", transactionId);
                returnData.put("message", message);
                // Se comprueba que los usuarios referidores fueron encontrados en caso de que
                // la transacción tenga un alcance de comisión mayor a 1
                int updateUsersSize = updateUsers.size();
                if (commissionScope > 1 && updateUsersSize != commissionScope) { // Actualizar solo comisiones de los
                                                                                 // usuarios que se les actualizo la
                                                                                 // wallet
                    // Se actualiza el registro que si sabemos que actualizó su wallet (usuario de
                    // la transacción)
                    transactionDB.setUserReferringFound(false);
                    transactionDB.setObservation(
                            "La comisión ha sido " + pointOfTransactionStatus + ", pero hay usuarios pendientes");
                    String transactionUserId = transactionDB.getUserId();
                    for (TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                        String transactionCommissionUserId = transactionCommission.getUserId();
                        if (transactionUserId.equals(transactionCommissionUserId)) {
                            transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                            break;
                        }
                    }
                    // En caso de que no se encontró usuario referidor A, quiere decir que se
                    // encontró el usuario referidor B,
                    // por lo tanto, también se le actualiza su registro en las comisiones, ya que,
                    // se logró actualizar su wallet
                    if (commissionScope == 3 && updateUsersSize == 2) { // No se encontró usuario referidor A
                        for (UserModel updateUser : updateUsers) {
                            String userIdFromUpdateUser = updateUser.getUserId();
                            if (!transactionUserId.equals(userIdFromUpdateUser)) {
                                for (TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                                    String transactionCommissionUserId = transactionCommission.getUserId();
                                    if (userIdFromUpdateUser.equals(transactionCommissionUserId)) {
                                        transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                                        break; // Estado de comisión de usuario referidor B actualizado
                                    }
                                }
                                break; // Se encontró usuario referidor B
                            }
                        }
                    }
                    // Se actualizo lo necesario, por lo tanto, se realiza la petición entregando el
                    // mensaje informativo
                    userRepository.saveAll(updateUsers);
                    transactionRepository.save(transactionDB);
                    return ResponseHelper.accepted("la transacción se ha actualizado y necesita revisión",
                            DataHelper.buildUser(userC, returnData));
                }
                // Transacción sin usuario referidor, se actualiza y se termina solicitud
                for (TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                    transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                }
                transactionDB.setObservation("La comisión ha sido " + pointOfTransactionStatus);
                userRepository.saveAll(updateUsers);
                transactionRepository.save(transactionDB);
                return ResponseHelper.ok("la transacción se ha finalizado correctamente",
                        DataHelper.buildUser(userC, returnData));
            } else if (quoterId.equals(quoterIdDB) && (quoterStatusDB.equals("Aprobado")
                    || quoterStatusDB.equals("Rechazado") || quoterStatusDB.equals("Caducado"))) {
                try {
                    transactionId = transactionRepository.findByUserIdAndQuoterId(userC.getUserId(), quoterId)
                            .orElseThrow().getTransactionId();
                } catch (NoSuchElementException e) {
                    LOGGER_MESSAGES.info("No es posible identificar id de transacción: " + e.getMessage());
                }
                returnData.put("quoterId", quoterId);
                returnData.put("transactionId", transactionId);
                returnData.put("message", message);
                return ResponseHelper.imUsed("la cotización ya ha sido finalizada y se encuentra: " + quoterStatusDB,
                        DataHelper.buildUser(userC, returnData));
            }
        }
        String errorMessage = "no es posible encontrar la cotización N°" + quoterId + ", del usuario: " + emailAuth;
        return ResponseHelper.failedDependency(errorMessage, "failed dependency");
    }

    // SERVICIOS DE VALIDACIONES DE DATOS
    @Override
    public void validateVehicleFinder(SearchVehicleRequest searchVehicle, BindingResult bindingResult) {
        this.quoterValidator.validate(searchVehicle, bindingResult);
    }

    @Override
    public void validatePlanFinder(SearchPlanRequest searchPlan, BindingResult bindingResult) {
        this.quoterValidator.validatePlanFinder(searchPlan, bindingResult);
    }

    @Override
    public void validateSelectedPlan(SelectPlanRequest selectPlan, BindingResult bindingResult) {
        this.quoterValidator.validateSelectedPlan(selectPlan, bindingResult);
    }

}
