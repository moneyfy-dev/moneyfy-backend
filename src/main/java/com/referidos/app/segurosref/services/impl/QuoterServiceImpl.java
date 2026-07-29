package com.referidos.app.segurosref.services.impl;

import com.referidos.app.segurosref.services.QuoterService;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.responses.enums.BusinessCodeEnum;
import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;
import com.referidos.app.segurosref.validators.QuoterValidator;

@Service
@RequiredArgsConstructor
public class QuoterServiceImpl implements QuoterService {

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
        // Error en caso de que el usuario aun no tenga cuentas bancarias registradas
        if (!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked(
                    "debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros",
                    null);
        }
        List<BrandModel> brandsDB = brandRepository.findAll();
        List<VehicleBrandDto> brandsDto = new ArrayList<>();
        // Por cada registro de marcas de vehiculos, generamos un objeto dto de la
        // marca, que se le anidan los objetos dto de los modelos de la marca
        for (BrandModel brandDB : brandsDB) {
            List<VehicleModelDto> modelsDto = new ArrayList<>();
            for (BrandDataModel modelDB : brandDB.getModels()) {
                modelsDto.add(new VehicleModelDto(modelDB.getModelId(), modelDB.getModel()));
            }
            brandsDto.add(new VehicleBrandDto(brandDB.getBrandId(), brandDB.getBrand(), modelsDto));
        }
        return ResponseHelper.ok("se ha traido la lista de las marcas de los vehiculos disponibles",
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
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Error en caso de que el usuario aun no tenga cuentas bancarias registradas
        if (!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked(
                    "debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros",
                    null);
        }
        String ppu = searchVehicle.ppu().toUpperCase(); // Patente del vehiculo a mayusculas
        String ownerId = searchVehicle.ownerId().toUpperCase(); // Rut de propietario a mayusculas por la 'k'

        // Consultar cliente externo BCI para datos del vehiculo
        BCIVehicleResponsePojo vehicleResponse = bciVehicleClient.searchVehicle(ppu);

        QuoterCarModel vehicleFound;
        VehicleDto vehicleDto;

        if (vehicleResponse.hasError()) {
            // Error en la API externa o no encontrado: se dejan los campos vacios
            vehicleFound = new QuoterCarModel(ppu, "", "", "", "", "", "", "", "");
            vehicleDto = new VehicleDto(ppu, "", "", "", "", "", "", "", "", false);
        } else {
            // Busqueda exitosa
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
                    quoterCar.getPpu().equals(ppu)) {
                userQuoter = quoterDB;
                break;
            }
        }
        // Si no se encontro registro existente, se crea una nueva cotizacion
        if (userQuoter == null) {
            QuoterOwnerModel quoterOwner = new QuoterOwnerModel(ownerId, "", "", "");
            QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel("", "", "", "", "", "", "");
            userQuoter = quoterHelper.createQuoteStructure(quoterOwner, vehicleFound, quoterPurchaser,
                    pointOfCurrentStatus, LocalDateTime.now());
            userDB.addQuoter(userQuoter);
            userDB = userRepository.save(userDB);
        }

        Map<String, Object> dataResponse = new HashMap<>();
        dataResponse.put("user", userDB);
        dataResponse.put("vehicle", vehicleDto);
        dataResponse.put("quoterId", userQuoter.getQuoterId());
        if (vehicleResponse.hasError()) {
            dataResponse.put("errorStatus", vehicleResponse.getStatusResponse());
            dataResponse.put("errorStatusInfo", vehicleResponse.getStatusOrErrorStr());
            dataResponse.put("errorResponseBodyStr", vehicleResponse.getResponseBodyStr());
            dataResponse.put("internalErrorCode", vehicleResponse.getInternalErrorCode());
            dataResponse.put("internalErrorMessage", BusinessCodeEnum
                    .fromCode(vehicleResponse.getInternalErrorCode()).getErrorDescription());
        }

        return ResponseHelper.created("se ha realizado la cotizacion exitosamente", dataResponse);
    }

    @SuppressWarnings({ "unchecked", "null" })
    @Transactional
    @Override
    public ResponseEntity<?> searchPlan(SearchPlanRequest searchPlan, String emailAuth) {
        // Si llega, es porque se validaron los datos, por lo tanto, los recuperamos
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Campo opcional, porque se puede realizar una solitud directa sin pasar por la
        // busqueda de vehiculo
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
        String ownerRelationOption = searchPlan.ownerRelationOption(); // Depende de la aseguradora si se usara el
                                                                       // campo
        // Intentamos encontrar la cotizacion, si existe actualizamos los datos, si no
        // existe se crea la cotizacion
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
                        return ResponseHelper.locked("La cotizacion se esta procesando", null);
                    }
                    // En caso de que sea una cotizacion que venga del proceso anterior
                    // actualizamos
                    // los datos, recordar que este es un endpoint que se puede repetir como tantas
                    // aseguradoras existan. Se debe actualizar los datos del vehiculo, porque
                    // puede
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
                    // Se actualiza la data del comprador de la cotizacion
                    QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                    quoterPurchaserDB.setPersonalId(purchaserId);
                    quoterPurchaserDB.setName(purchaserName);
                    quoterPurchaserDB.setPaternalSurname(purchaserPaternalSur);
                    quoterPurchaserDB.setMaternalSurname(purchaserMaternalSur);
                    quoterPurchaserDB.setEmail(purchaserEmail);
                    quoterPurchaserDB.setPhone(purchaserPhone);
                    quoterPurchaserDB.setOwnerRelationOption(ownerRelationOption);
                    // Se actualiza el estado actual de la cotizacion y sus metadatos
                    quoterDB.setQuoterStatus(pointOfQuoterCurrentStatus);
                    quoterDB.setUpdatedDate(currentDateTime);

                    userQuoter = quoterDB;
                    break;
                }
            }
        }
        // Si la cotizacion aun no existe se debe crear
        if (userQuoter == null) {
            // Primero se busca una cotizacion existente con los datos mas relevante del
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
            // Si la cotizacion no se encontro con los datos actuales de la solicitud, se
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
                            .info("Excepcion al intentar autocompletar datos del vehiculo BCI: " + e.getMessage());
                }

                QuoterOwnerModel quoterOwner = new QuoterOwnerModel("", "", "", "");
                QuoterCarModel quoterCar = new QuoterCarModel(ppu, brand, model, year, vehicleType, vehicleColor,
                        vehicleMotor,
                        vehicleChassis, "");
                QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel(purchaserId, purchaserName,
                        purchaserPaternalSur, purchaserMaternalSur, purchaserEmail, purchaserPhone,
                        ownerRelationOption);
                // Creamos nueva cotizacion y la persistimos
                userQuoter = quoterHelper.createQuoteStructure(quoterOwner, quoterCar, quoterPurchaser,
                        pointOfQuoterCurrentStatus, currentDateTime);
                userDB.addQuoter(userQuoter);
            }
        }
        // Ahora entregaremos los planes, dependiendo de la aseguradora, enviando los
        // datos del vehiculo verificado.
        List<QuotationPlanDto> planList = new ArrayList<>();
        InsurerModel returnInsurerDB = new InsurerModel(null, "", "");
        returnInsurerDB.setInsurerId(new ObjectId());
        Optional<InsurerModel> insurerOptional = insurerRepository.findByAlias(insurerAlias);
        String errorPlanFinder = "1"; // Error no se encontro una aseguradora para la busqueda de planes
        String errorMessage = "No se encontro la aseguradora con el alias '" + insurerAlias + "'";
        String requestBody = "";
        String responseStr = "";
        if (insurerOptional.isPresent()) {
            returnInsurerDB = insurerOptional.get();
            switch (insurerAlias) {
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
                    // Momentaneo antes del cambio de estructura de esta respuesta
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

        // Guardar planes en BD consolidando coberturas sin N+1
        if (planList != null && !planList.isEmpty()) {
            List<String> planIds = planList.stream().map(QuotationPlanDto::getPlanId).distinct().toList();
            List<PlanModel> existingPlans = planRepository.findAllById(planIds);
            Map<String, PlanModel> plansMap = existingPlans.stream()
                    .collect(Collectors.toMap(PlanModel::getPlanId, p -> p));
            List<PlanModel> plansToSave = new ArrayList<>();

            for (QuotationPlanDto insurerPlan : planList) {
                String pId = insurerPlan.getPlanId();
                PlanModel pModel = plansMap.get(pId);

                if (pModel == null) {
                    pModel = new PlanModel(pId, returnInsurerDB.getName(), insurerPlan.getPlanName(),
                            insurerPlan.getDeductibleDesc(), insurerPlan.getStolenVehicle(), insurerPlan.getTotalLoss(),
                            insurerPlan.getDamageThirdParty(), insurerPlan.getWorkshopType(),
                            new HashSet<>(insurerPlan.getCoverages()),
                            currentDateTime, currentDateTime);
                    plansMap.put(pId, pModel);
                    plansToSave.add(pModel);
                } else {
                    // Reemplazar las coberturas existentes para no acumular basura historica
                    if (insurerPlan.getCoverages() != null) {
                        pModel.setCoverages(new HashSet<>(insurerPlan.getCoverages()));
                    }
                    if (!plansToSave.contains(pModel)) {
                        plansToSave.add(pModel);
                    }
                }
            }
            if (!plansToSave.isEmpty()) {
                planRepository.saveAll(plansToSave);
            }
        }

        // Guardado consolidado de toda la cotizacion en base de datos al final del
        // flujo
        userRepository.save(userDB);

        QuotationDto quotationDto = new QuotationDto(userQuoter.getQuoterId(), errorPlanFinder, errorMessage,
                requestBody, responseStr, returnInsurerDB, planList);
        return ResponseHelper.ok("se ha realizado la cotizacion", quotationDto);
    }

    @Transactional
    @Override
    public ResponseEntity<?> selectPlan(SelectPlanRequest planSelected, String emailAuth) {
        // Los datos del plan seleccionado ya han sido validados
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<QuoterModel> quoters = userDB.getQuoters();
        String quoterId = planSelected.quoterId();
        String pointOfQuoterCurrentStatus = "Recopilando";
        // Buscamos al cotizador mediante al id y por el estado del flujo anterior o el
        // actual, en caso de que el
        // usuario desee cambiar de plan y datos de inspeccion
        for (QuoterModel quoterDB : quoters) {
            String quoterStatusDB = quoterDB.getQuoterStatus();
            String quoterDBId = quoterDB.getQuoterId();
            if ((quoterStatusDB.equals("Cotizando") || quoterStatusDB.equals(pointOfQuoterCurrentStatus)) &&
                    quoterId.equals(quoterDBId)) {
                // Se encuentra la cotizacion por lo tanto se pueden obtener los datos y seguir
                // con el flujo
                QuoterOwnerModel quoterOwner = quoterDB.getQuoterOwnerData();
                quoterOwner.setName(planSelected.ownerName().strip()); // Usamos strip() para quitar espacios al inicio
                                                                       // y final
                quoterOwner.setPaternalSurname(planSelected.ownerPaternalSur().strip());
                quoterOwner.setMaternalSurname(planSelected.ownerMaternalSur().strip());
                // Actualizamos el plan seleccionado del cotizador
                QuoterPlanModel quoterPlan = quoterDB.getQuoterPlanData();

                String finalPlanId = planSelected.planId();
                if (finalPlanId != null && (finalPlanId.startsWith("BCI_") || finalPlanId.startsWith("FDI_"))) {
                    finalPlanId = finalPlanId.substring(4);
                }
                quoterPlan.setQuoterPlanId(finalPlanId);

                quoterPlan.setInsurer(planSelected.insurer().strip());

                String insurerAlias = planSelected.insurerAlias() != null ? planSelected.insurerAlias().strip() : "";
                quoterPlan.setInsurerAlias(insurerAlias);

                quoterPlan.setPlanName(planSelected.planName().strip());
                quoterPlan.setValueUF(planSelected.valueUF());
                quoterPlan.setGrossPriceUF(planSelected.grossPriceUF());
                quoterPlan.setTotalMonths(planSelected.totalMonths());
                quoterPlan.setMonthlyPriceUF(planSelected.monthlyPriceUF());
                quoterPlan.setMonthlyPrice(planSelected.monthlyPrice());
                quoterPlan.setDeductibleDesc(planSelected.deductibleDesc());
                quoterPlan.setDiscount(planSelected.discount());

                if ("aseguradora4".equals(insurerAlias)) { // BCI
                    quoterPlan.setIntNroTarificacionBCI(planSelected.intNroTarificacionBCI());
                    quoterPlan.setStrNroCotizacionBCI(planSelected.strNroCotizacionBCI());
                    quoterPlan.setDtFinVigenciaBCI(planSelected.dtFinVigenciaBCI());

                    quoterPlan.setDealTokenFDI("");
                    quoterPlan.setItemIdFDI(null);
                    quoterPlan.setQuotationIdFDI(null);
                    quoterPlan.setFidIdFDI("");
                    quoterPlan.setExpiryDateFDI("");
                } else if ("aseguradora5".equals(insurerAlias)) { // FDI
                    quoterPlan.setIntNroTarificacionBCI(null);
                    quoterPlan.setStrNroCotizacionBCI("");
                    quoterPlan.setDtFinVigenciaBCI("");

                    quoterPlan.setDealTokenFDI(planSelected.dealTokenFDI());
                    quoterPlan.setItemIdFDI(planSelected.itemIdFDI());
                    quoterPlan.setQuotationIdFDI(planSelected.quotationIdFDI());
                    quoterPlan.setFidIdFDI(planSelected.fidIdFDI());
                    quoterPlan.setExpiryDateFDI(planSelected.expiryDateFDI());
                } else { // Test insurers or others
                    quoterPlan.setIntNroTarificacionBCI(null);
                    quoterPlan.setStrNroCotizacionBCI("");
                    quoterPlan.setDtFinVigenciaBCI("");

                    quoterPlan.setDealTokenFDI("");
                    quoterPlan.setItemIdFDI(null);
                    quoterPlan.setQuotationIdFDI(null);
                    quoterPlan.setFidIdFDI("");
                    quoterPlan.setExpiryDateFDI("");
                }

                // Actualizamos la direccion de la cotizacion para la inspeccion
                QuoterAddressModel quoterAddress = quoterDB.getQuoterAddressData();
                quoterAddress.setStreet(planSelected.street().strip());
                quoterAddress.setStreetNumber(planSelected.streetNumber().strip());
                quoterAddress.setDepartment(
                        (!DataHelper.isNull(planSelected.department())) ? planSelected.department().strip() : "");
                quoterAddress.setRegion(
                        (!DataHelper.isNull(planSelected.region())) ? planSelected.region().strip() : "");
                quoterAddress.setCommune(
                        (!DataHelper.isNull(planSelected.commune())) ? planSelected.commune().strip() : "");
                // Actualizamos el estado del flujo, la fecha de actualizacion del cotizador y
                // la base de datos.
                quoterDB.setQuoterStatus(pointOfQuoterCurrentStatus);
                quoterDB.setUpdatedDate(LocalDateTime.now());
                userDB = userRepository.save(userDB);
                return ResponseHelper.ok("se ha seleccionado el plan de la cotizacion",
                        DataHelper.buildUser(userDB, "quoterId", quoterId));
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    @Override
    public ResponseEntity<?> generateTransaction(GenerateTransactionRequest generateTransaction, String emailAuth) {
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userCId = userC.getUserId();
        String quoterId = generateTransaction.quoterId();
        quoterId = (!DataHelper.isNull(quoterId) && ObjectId.isValid(quoterId)) ? quoterId : "";
        if (!quoterId.equals("")) {
            // El id del cotizador cumple con el formato, para buscar un registro
            // especifico
            List<QuoterModel> quoters = userC.getQuoters();
            for (QuoterModel quoterDB : quoters) {
                String quoterIdDB = quoterDB.getQuoterId();
                String quoterStatusDB = quoterDB.getQuoterStatus();
                String message = "";
                if (quoterId.equals(quoterIdDB)) {
                    if (transactionRepository.existsByUserIdAndQuoterId(userCId, quoterId)) {
                        message = "Su transaccion para esta cotizacion ya ha sido generada y esta siendo procesada.";
                        LOGGER_MESSAGES.info("Intento duplicado bloqueado: " + message);
                        Map<String, Object> data = Map.of("quoterId", quoterId, "message", message);
                        return ResponseHelper.ok("la transaccion se encuentra en proceso",
                                DataHelper.buildUser(userC, data));
                    }
                    if (quoterStatusDB.equals("Recopilando")) {
                        // Se comienza a generar la transaccion con las comisiones debidas
                        String transactionId = new ObjectId().toString();
                        int commissionScope = 1; // Comienzo de nivel encontrado para entregar comisiones
                        int commissionTotal = commissionUserC; // Comienzo de la comision total que se lleva la venta
                        String pointOfCurrentStatus = "Pendiente";
                        LocalDateTime currentDateTime = LocalDateTime.now();
                        TransactionModel novaTransaction = quoterHelper.generateNovaTransactionStructure(transactionId,
                                userCId, quoterDB, pointOfCurrentStatus, commissionTotal, commissionScope,
                                "La comisión está siendo procesada", currentDateTime);
                        List<UserModel> users = new ArrayList<>();
                        // Usuarios que se tienen que actualizar por el ajuste de la wallet
                        // Comenzamos a actualizar la data de la wallet del usuario.
                        WalletModel walletC = userC.getWallet();
                        walletC.setOutstandingBalance(walletC.getOutstandingBalance() + commissionUserC);
                        walletC.setTotalBalance(walletC.getOutstandingBalance() + walletC.getAvailableBalance());
                        users.add(userC);
                        // Ver si existe el userB y userA, para ajustar transaccion
                        String emailUserB = "";
                        String emailUserA = "";
                        try {
                            // IMPORTANTE: Se busca un userB que haya referido al userC, para agregar la
                            // comisión correspondiente. Si el usuario que esta refiriendo esta activado,
                            // tiene que haber un registro en la coleccion de 'users', si no se encuentra se
                            // maneja con una respuesta errada con try/catch.
                            Optional<ReferredModel> referredByUserB = referredRepository.findByReferred(emailAuth);
                            if (referredByUserB.isPresent()
                                    && referredByUserB.get().getUserReferringStatus().equals("Activado")) {
                                // Buscamos el usuario referidor y actualizamos wallet
                                emailUserB = referredByUserB.get().getUserReferring();
                                UserModel userB = userRepository.findByPersonalData_Email(emailUserB).orElseThrow();
                                WalletModel walletB = userB.getWallet();
                                walletB.setOutstandingBalance(walletB.getOutstandingBalance() + commissionUserB);
                                walletB.setTotalBalance(
                                        walletB.getOutstandingBalance() + walletB.getAvailableBalance());
                                users.add(userB);
                                // Ajustamos valores de transaccion y agregamos comision
                                commissionScope = 2;
                                commissionTotal += commissionUserB;
                                novaTransaction.addCommission(new TransactionComissionModel(userB.getUserId(),
                                        commissionUserB, pointOfCurrentStatus, "", DataHelper.deprecatedDateTime()));
                                // IMPORTANTE: Se busca un userA que haya referido al userB, para agregar la
                                // comision correspondiente.
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
                                    // Ajustamos valores de transaccion y agregamos comision
                                    commissionScope = 3;
                                    commissionTotal += commissionUserA;
                                    novaTransaction.addCommission(new TransactionComissionModel(userA.getUserId(),
                                            commissionUserA, pointOfCurrentStatus, "",
                                            DataHelper.deprecatedDateTime()));
                                }
                            }
                        } catch (NoSuchElementException e) {
                            // En caso de haber excepcion, seguimos ya que no se alcanza a actualizar
                            // ningun dato esencial y entregamos mensaje de excepcion
                            String referredNotFound = (novaTransaction.getCommissionScope() == 1) ? emailUserB
                                    : emailUserA;
                            message = "Ha ocurrido una excepción en la transacción N°" + transactionId
                                    + ", el referido no fue encontrado: " + referredNotFound + "\n" + e.getMessage();
                            LOGGER_MESSAGES.info(message);
                        }
                        // Se actualiza el nivel de comisiones que se alcanzo a entregar la transaccion
                        // (referidos).
                        novaTransaction.setCommissionScope(commissionScope);
                        novaTransaction.setCommissionTotal(commissionTotal);
                        // Se actualizan el estado, fecha de actualizacion y se envia el detalle del
                        // plan que se esta cotizando al usuario
                        quoterDB.setQuoterStatus(pointOfCurrentStatus);
                        quoterDB.setUpdatedDate(currentDateTime);
                        emailAppProvider.sendQuoteDetails(userC, quoterDB);
                        // Guardamos en la base de datos y retornamos el usuario de la consulta (userC),
                        // id del cotizador, y id de la transaccion
                        userRepository.saveAll(users);
                        transactionRepository.save(novaTransaction);
                        Map<String, Object> data = Map.of("quoterId", quoterId, "transactionId", transactionId,
                                "message",
                                message);
                        return ResponseHelper.ok("la transaccion se ha realizado correctamente",
                                DataHelper.buildUser(userC, data));
                    }
                    break;
                }
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
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
