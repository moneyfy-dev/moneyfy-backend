package com.referidos.app.segurosref.services;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.bson.types.ObjectId;
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
@RequiredArgsConstructor
public class QuoterServiceImpl implements QuoterService {

    @Value(value = "${moneyfy.api-key}")
    private String apiKeyMF;

    @Value("${moneyfy.commissions.level1}")
    private int commissionUserC;

    @Value("${moneyfy.commissions.level2}")
    private int commissionUserB;

    @Value("${moneyfy.commissions.level3}")
    private int commissionUserA;

    private final UserRepository userRepository;

    private final InsurerRepository insurerRepository;

    private final BrandRepository brandRepository;

    private final PlanRepository planRepository;

    private final TransactionRepository transactionRepository;

    private final ReferredRepository referredRepository;

    private final QuoterValidator quoterValidator;

    private final BCIQuotationClient bciQuotationClient;

    private final BCIVehicleClient bciVehicleClient;

    private final FDIQuotationClient fdiQuotationClient;

    private final EmailAppProvider emailAppProvider;

    private final QuoterHelper quoterHelper;

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
                String message = "";
                if (quoterId.equals(quoterIdDB) && quoterStatusDB.equals("Recopilando")) {
                    if (transactionRepository.existsByUserIdAndQuoterId(userCId, quoterId)) {
                        message = "transacción existente que está siendo procesada";
                        LOGGER_MESSAGES.info(message);
                        return ResponseHelper.gone(message, null);
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
                        LOGGER_MESSAGES.info(message);
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

    @SuppressWarnings("null")
    @Transactional
    @Override
    public ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, String emailAuth,
            String requestEndpoint) {
        if (finalizeQuote == null || finalizeQuote.usersQuotes() == null) {
            return ResponseHelper.failedDependency("la data proporcionada no es correcta", "failed dependency");
        }

        List<Map<String, Object>> usersResultList = new ArrayList<>();
        Map<String, UserModel> usersToSave = new HashMap<>();
        Map<String, TransactionModel> transactionsToSave = new HashMap<>();
        LocalDateTime currentDateTime = LocalDateTime.now();

        for (FinalizeQuoteRequest.UserQuoteUpdate userQuoteUpdate : finalizeQuote.usersQuotes()) {
            String userId = userQuoteUpdate.userId();
            if (DataHelper.isNull(userId) || !ObjectId.isValid(userId)) {
                LOGGER_MESSAGES.info("El ID de usuario proporcionado no es válido: " + userId);
                continue;
            }

            Optional<UserModel> userOptional = userRepository.findById(new ObjectId(userId));
            if (userOptional.isEmpty()) {
                LOGGER_MESSAGES.info("Usuario no encontrado en la base de datos con id: " + userId);
                continue;
            }

            UserModel userC = userOptional.get();
            // If the user was already modified in previous iterations, get from map to
            // persist latest states
            if (usersToSave.containsKey(userC.getUserId())) {
                userC = usersToSave.get(userC.getUserId());
            }
            List<Map<String, Object>> quotesResultList = new ArrayList<>();

            for (FinalizeQuoteRequest.QuoteUpdate quoteUpdate : userQuoteUpdate.quotes()) {
                String quoterId = quoteUpdate.quoterId();
                String pointOfTransactionStatus = quoteUpdate.transactionStatus();
                String message = "";

                if (DataHelper.isNull(quoterId) || !ObjectId.isValid(quoterId)
                        || DataHelper.isNull(pointOfTransactionStatus) ||
                        (!pointOfTransactionStatus.equals("Aprobado") && !pointOfTransactionStatus.equals("Rechazado")
                                &&
                                !pointOfTransactionStatus.equals("Caducado"))) {
                    message = "Estado o ID de cotización inválido";
                    LOGGER_MESSAGES.info("Usuario " + userId + " - Cotización " + quoterId + ": " + message);
                    quotesResultList.add(Map.of("quoterId", quoterId, "message", message));
                    continue;
                }

                QuoterModel quoterDB = null;
                for (QuoterModel q : userC.getQuoters()) {
                    if (q.getQuoterId().equals(quoterId)) {
                        quoterDB = q;
                        break;
                    }
                }

                if (quoterDB == null || !quoterDB.getQuoterStatus().equals("Pendiente")) {
                    message = "Cotización no encontrada o no está en estado Pendiente";
                    LOGGER_MESSAGES.info("Usuario " + userId + " - Cotización " + quoterId + ": " + message);
                    quotesResultList.add(Map.of("quoterId", quoterId, "message", message));
                    continue;
                }

                Optional<TransactionModel> transactionOpt = transactionRepository
                        .findByUserIdAndQuoterIdAndStatus(userId, quoterId, "Pendiente");
                if (transactionOpt.isEmpty()) {
                    message = "Transacción Pendiente no encontrada para la cotización";
                    LOGGER_MESSAGES.info("Usuario " + userId + " - Cotización " + quoterId + ": " + message);
                    quotesResultList.add(Map.of("quoterId", quoterId, "message", message));
                    continue;
                }

                TransactionModel transactionDB = transactionOpt.get();
                if (!transactionDB.getUserReferringFound()) {
                    message = "Se necesita revisar transacción por referidor no encontrado previamente";
                    LOGGER_MESSAGES.info("Usuario " + userId + " - Cotización " + quoterId + ": " + message);
                    quotesResultList.add(Map.of("quoterId", quoterId, "message", message));
                    continue;
                }

                String transactionId = transactionDB.getTransactionId();
                int commissionScope = transactionDB.getCommissionScope();
                boolean isTrasactionApproved = pointOfTransactionStatus.equals("Aprobado");
                List<UserModel> updateUsers = new ArrayList<>();
                boolean errorEnReferidos = false;

                UserModel userB = null;
                UserModel userA = null;

                // Buscamos a los referidores PRIMERO, para no ensuciar la wallet en caso de
                // fallo
                try {
                    String currentUserEmail = userC.getPersonalData().getEmail();
                    if (commissionScope > 1) {
                        ReferredModel referredByUserB = referredRepository.findByReferred(currentUserEmail)
                                .orElseThrow();
                        String emailUserB = referredByUserB.getUserReferring();
                        String codeToReferB = referredByUserB.getCodeToRefer();
                        userB = userRepository.findByPersonalData_EmailAndCodeToRefer(emailUserB, codeToReferB)
                                .orElseThrow();
                        if (usersToSave.containsKey(userB.getUserId()))
                            userB = usersToSave.get(userB.getUserId());

                        if (commissionScope > 2) {
                            ReferredModel referredByUserA = referredRepository.findByReferred(emailUserB).orElseThrow();
                            String emailUserA = referredByUserA.getUserReferring();
                            String codeToReferA = referredByUserA.getCodeToRefer();
                            userA = userRepository.findByPersonalData_EmailAndCodeToRefer(emailUserA, codeToReferA)
                                    .orElseThrow();
                            if (usersToSave.containsKey(userA.getUserId()))
                                userA = usersToSave.get(userA.getUserId());
                        }
                    }
                } catch (NoSuchElementException e) {
                    errorEnReferidos = true;
                    message = "Ha ocurrido un excepción en la transacción N°" + transactionId
                            + ", el alcance de la comisión es " + commissionScope + ", y ";
                    if (userB == null) {
                        message += "no se ha podido encontrar el usuario referidor B";
                        message += (commissionScope == 2) ? "" : " y por lo tanto, tampoco el usuario referidor A";
                    } else {
                        message += "no se ha podido encontrar el usuario referidor A";
                    }
                    LOGGER_MESSAGES.info("Usuario " + userId + " - Cotización " + quoterId + ": " + message);
                }

                if (errorEnReferidos) {
                    // Si hay error en referidos, NO actualizamos wallets ni cotizaciones, solo el
                    // flag de la transaccion
                    transactionDB.setUserReferringFound(false);
                    transactionsToSave.put(transactionDB.getTransactionId(), transactionDB);
                    quotesResultList.add(Map.of("quoterId", quoterId, "message", message + " - Requiere revisión"));
                    continue;
                }

                // Sin error, ahora SI actualizamos Wallets
                WalletModel walletC = userC.getWallet();
                int outstandingBalanceC = walletC.getOutstandingBalance() - commissionUserC;
                walletC.setOutstandingBalance(outstandingBalanceC);
                int availableBalanceC = walletC.getAvailableBalance();
                walletC.setAvailableBalance(
                        (isTrasactionApproved) ? (availableBalanceC + commissionUserC) : availableBalanceC);
                walletC.setTotalBalance(walletC.getOutstandingBalance() + walletC.getAvailableBalance());
                updateUsers.add(userC);

                if (userB != null) {
                    WalletModel walletB = userB.getWallet();
                    int outstandingBalanceB = walletB.getOutstandingBalance() - commissionUserB;
                    walletB.setOutstandingBalance(outstandingBalanceB);
                    int availableBalanceB = walletB.getAvailableBalance();
                    walletB.setAvailableBalance(
                            (isTrasactionApproved) ? (availableBalanceB + commissionUserB) : availableBalanceB);
                    walletB.setTotalBalance(walletB.getOutstandingBalance() + walletB.getAvailableBalance());
                    updateUsers.add(userB);
                }

                if (userA != null) {
                    WalletModel walletA = userA.getWallet();
                    int outstandingBalanceA = walletA.getOutstandingBalance() - commissionUserA;
                    walletA.setOutstandingBalance(outstandingBalanceA);
                    int availableBalanceA = walletA.getAvailableBalance();
                    walletA.setAvailableBalance(
                            (isTrasactionApproved) ? (availableBalanceA + commissionUserA) : availableBalanceA);
                    walletA.setTotalBalance(walletA.getOutstandingBalance() + walletA.getAvailableBalance());
                    updateUsers.add(userA);
                }

                // Actualizamos estados y fechas
                quoterDB.setQuoterStatus(pointOfTransactionStatus);
                quoterDB.setUpdatedDate(currentDateTime);
                transactionDB.setStatus(pointOfTransactionStatus);
                transactionDB.setUpdatedDate(currentDateTime);
                transactionDB
                        .setApprovalDate((isTrasactionApproved) ? currentDateTime : transactionDB.getApprovalDate());

                for (TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                    transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                }
                transactionDB.setObservation("La comisión ha sido " + pointOfTransactionStatus);

                for (UserModel u : updateUsers) {
                    usersToSave.put(u.getUserId(), u);
                }
                transactionsToSave.put(transactionDB.getTransactionId(), transactionDB);

                message = "La transacción se ha finalizado correctamente (" + pointOfTransactionStatus + ")";
                LOGGER_MESSAGES.info("Usuario " + userId + " - Cotización " + quoterId + ": " + message);
                quotesResultList.add(Map.of("quoterId", quoterId, "message", message));
            }

            usersResultList.add(Map.of("userId", userId, "quotes", quotesResultList));
        }

        // Realizamos el guardado en bloque en Base de Datos
        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave.values());
        }
        if (!transactionsToSave.isEmpty()) {
            transactionRepository.saveAll(transactionsToSave.values());
        }

        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("generalMessage", "Actualización masiva procesada con éxito");
        finalResult.put("status", 200);
        finalResult.put("users", usersResultList);

        return ResponseHelper.ok("la actualización masiva de cotizaciones se ha completado", finalResult);
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
