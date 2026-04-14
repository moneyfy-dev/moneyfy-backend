package com.referidos.app.segurosref.services;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.referidos.app.segurosref.dtos.ResultQuoteDto;
import com.referidos.app.segurosref.dtos.TestPlanDto;
import com.referidos.app.segurosref.dtos.VehicleBrandDto;
import com.referidos.app.segurosref.dtos.VehicleModelDto;
import com.referidos.app.segurosref.dtos.commission.CommissionAccountDto;
import com.referidos.app.segurosref.dtos.commission.CommissionPaymentDto;
import com.referidos.app.segurosref.dtos.report.ReportUserDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.QuoterHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.DeviceModel;
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
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.BrandDataModel;
import com.referidos.app.segurosref.models.BrandModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.provider.ApiBciProvider;
import com.referidos.app.segurosref.provider.EmailServiceProvider;
import com.referidos.app.segurosref.repositories.InsurerRepository;
import com.referidos.app.segurosref.repositories.LogRepository;
import com.referidos.app.segurosref.repositories.PaymentRepository;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.PlanRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.repositories.BrandRepository;
import com.referidos.app.segurosref.requests.CommissionPaymentRequest;
import com.referidos.app.segurosref.requests.CommissionReportRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;
import com.referidos.app.segurosref.validators.QuoterValidator;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class QuoterServiceImpl implements QuoterService {

    @Value(value = "${api.key.moneyfy}")
    private String apiKeyMF;

    @Value(value="${report.commission.cutoff-date}")
    private int commissionCutoffDate;

    @Value(value="${report.commission.payment-date}")
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
    private DeviceRepository deviceRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private QuoterValidator quoterValidator;

    @Autowired
    private ApiBciProvider apiBciProvider;

    @Autowired
    private EmailServiceProvider emailProvider;

    @Autowired
    private QuoterHelper quoterHelper;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> searchVehicleBrands(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Error en caso de que el usuario aún no tenga cuentas bancarias registradas
        if(!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked("debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros", null);
        }
        List<BrandModel> brandsDB = brandRepository.findAll();
        List<VehicleBrandDto> brandsDto = new ArrayList<>();
        // Por cada registro de marcas de vehículos, generamos un objeto dto de la marca, que se le anidan los objetos dto de los modelos de la marca
        for(BrandModel brandDB : brandsDB) {
            List<VehicleModelDto> modelsDto = new ArrayList<>();
            for(BrandDataModel modelDB : brandDB.getModels()) {
                modelsDto.add(new VehicleModelDto(modelDB.getModelId(), modelDB.getModel()));
            }
            brandsDto.add(new VehicleBrandDto(brandDB.getBrandId(), brandDB.getBrand(), modelsDto));
        }
        return ResponseHelper.ok("se ha traido la lista de las marcas de los vehículos disponibles", DataHelper.buildUser(userDB, "brands", brandsDto));
    }

    @Transactional
    @Override
    public ResponseEntity<?> searchInsurers(String emailAuth, String updateCredential, String device) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<String> insurers = new ArrayList<>();
        insurerRepository.findAll().forEach(insurerDB -> {
            insurers.add(insurerDB.getAlias());
        });
        // Endpoint que se utiliza para actualizar token de refresco, si es necesario
        if(updateCredential.equals("Dated")) {
            Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(emailAuth, device);
            if(deviceOptional.isPresent()) {
                UserHelper.updateRefreshToken(userRepository, userDB, deviceOptional.get(), deviceRepository);
            }
        }
        return ResponseHelper.ok("se ha traido la lista de aseguradoras disponibles", DataHelper.buildUser(userDB, "insurers", insurers));
    }

    @Transactional
    @Override
    public ResponseEntity<?> searchVehicle(SearchVehicleRequest searchVehicle, String emailAuth) {
        UserModel userDB = this.userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Error en caso de que el usuario aún no tenga cuentas bancarias registradas
        if(!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked("debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros", null);
        }
        String ppu = searchVehicle.ppu().toUpperCase(); // Patente del vehículo a mayúsculas
        String ownerId = searchVehicle.ownerId().toUpperCase(); // Rut de propietario a mayúsculas por la 'k'
        // Búsqueda temporal 'simulada' con lista de vehículos de prueba, ya que, luego se debería buscar vehículo por patente o rut del propietario
        List<QuoterCarModel> testVehicles = quoterHelper.vehicleList();
        QuoterCarModel vehicleFound = null;
        for(QuoterCarModel testVehicle : testVehicles) {
            if(testVehicle.getPpu().equals(ppu)) {
                vehicleFound = testVehicle;
                break;
            }
        }
        // Si no se encontro vehículo de prueba por ppu (patente), se asigna uno por defecto
        if(vehicleFound == null) {
            vehicleFound = quoterHelper.buildDefaultVehicle(false, ppu, "", "", "");
        }
        // Buscamos si existe ya existe el registro para volver a cargarlo y no crear duplicidad
        List<QuoterModel> quoters = userDB.getQuoters();
        QuoterModel userQuoter = null;
        for(QuoterModel quoterDB : quoters) {
            String quoterStatus = quoterDB.getQuoterStatus();
            String quoterOwnerId = quoterDB.getQuoterOwnerData().getPersonalId();
            QuoterCarModel quoterCar = quoterDB.getQuoterCarData();
            if(quoterStatus.equals("Iniciando") && quoterOwnerId.equals(ownerId) &&
                    quoterCar.getPpu().equals(ppu) && quoterCar.getBrand().equals(vehicleFound.getBrand()) &&
                    quoterCar.getModel().equals(vehicleFound.getModel()) && quoterCar.getYear().equals(vehicleFound.getYear())) {
                userQuoter = quoterDB;
                break;
            }
        }
        // Si no se encontró registro existente, se crea una nueva cotización
        if(userQuoter == null) {
            QuoterOwnerModel quoterOwner = new QuoterOwnerModel(ownerId, "", "", "");
            QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel("", "", "", "", "", "", "");
            userQuoter = quoterHelper.createQuoteStructure(quoterOwner, vehicleFound, quoterPurchaser,
                    "Iniciando", LocalDateTime.now());
            userDB.addQuoter(userQuoter);
            userDB = userRepository.save(userDB);
        }
        return ResponseHelper.created("se ha realizado la cotización exitosamente", DataHelper.buildUser(userDB, "vehicle", vehicleFound, "quoterId", userQuoter.getQuoterId()));
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public ResponseEntity<?> searchPlan(SearchPlanRequest searchPlan, String emailAuth) {
        // Si llega, es porque se validaron los datos, por lo tanto, los recuperamos
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Campo opcional, porque se puede realizar una solitud directa sin pasar por la búsqueda de vehículo
        String quoterId = (!DataHelper.isNull(searchPlan.quoterId())) ? searchPlan.quoterId() : "";
        String ppu = searchPlan.ppu().toUpperCase();
        String brand = searchPlan.brand().toUpperCase();
        String model = searchPlan.model().toUpperCase();
        String year = searchPlan.year();
        String insurerAlias = searchPlan.insurerAlias().strip(); // Usamos strip() para quitar espacios al inicio y final
        // Hay 2 tipos de busqueda para el vehículo 'Manual' o 'Auto', pero por ahora se simula la búsqueda del auto solamente
        // String requestType = searchPlan.requestType();
        String purchaserId = searchPlan.purchaserId();
        String purchaserName = searchPlan.purchaserName().strip();
        String purchaserPaternalSur = searchPlan.purchaserPaternalSur().strip();
        String purchaserMaternalSur = searchPlan.purchaserMaternalSur().strip();
        String purchaserEmail = searchPlan.purchaserEmail();
        String purchaserPhone = !DataHelper.isNull(searchPlan.purchaserPhone()) ? searchPlan.purchaserPhone() : ""; // Opcional
        String ownerRelationOption = searchPlan.ownerRelationOption(); // Depende de la aseguradora si se usará el campo
        // Intentamos encontrar la cotización, si existe actualizamos los datos, si no existe se crea la cotización
        QuoterModel userQuoter = null;
        List<QuoterModel> quoters = userDB.getQuoters();
        String pointOfQuoterCurrentStatus = "Cotizando";
        LocalDateTime currentDateTime = LocalDateTime.now();
        if(!quoterId.equals("")) {
            for(QuoterModel quoterDB : quoters) {
                String quoterDBId = quoterDB.getQuoterId();
                if(quoterDBId.equals(quoterId)) {
                    if(!quoterDB.getQuoterStatus().equals("Iniciando") && !quoterDB.getQuoterStatus().equals(pointOfQuoterCurrentStatus)) {
                        return ResponseHelper.locked("La cotización se esta procesando", null);
                    }
                    // En caso de que sea una cotización que venga del proceso anterior actualizamos los datos, recordar que este es un endpoint que se puede repetir como tantas aseguradoras existan
                    if(quoterDB.getQuoterStatus().equals("Iniciando")) {
                        // Se actualiza la data del vehículo del cotizador
                        quoterDB.setQuoterCarData(quoterHelper.buildDefaultVehicle(true, ppu, brand, model, year));
                        // Se actualiza la data del comprador de la cotización
                        QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                        quoterPurchaserDB.setPersonalId(purchaserId);
                        quoterPurchaserDB.setName(purchaserName);
                        quoterPurchaserDB.setPaternalSurname(purchaserPaternalSur);
                        quoterPurchaserDB.setMaternalSurname(purchaserMaternalSur);
                        quoterPurchaserDB.setEmail(purchaserEmail);
                        quoterPurchaserDB.setPhone(purchaserPhone);
                        quoterPurchaserDB.setOwnerRelationOption(ownerRelationOption);
                        // Se actualiza el estado actual de la cotización y el usuario para que persistan los cambios
                        quoterDB.setQuoterStatus(pointOfQuoterCurrentStatus);
                        quoterDB.setUpdatedDate(currentDateTime);
                        userDB = userRepository.save(userDB);
                    }
                    userQuoter = quoterDB;
                    break;
                }
            }
        }
        // Si la cotización aún no existe se debe crear
        if(userQuoter == null) {
            // Primero se busca una cotización existente con los datos más relevante del proceso actual, incluyendo el estado
            boolean isQuoter = false;
            for(QuoterModel quoterDB : quoters) {
                QuoterCarModel quoterCarDB = quoterDB.getQuoterCarData();
                QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                if(quoterDB.getQuoterStatus().equals(pointOfQuoterCurrentStatus) && quoterCarDB.getPpu().equals(ppu) && 
                        quoterCarDB.getBrand().equals(brand) && quoterCarDB.getModel().equals(model) &&
                        quoterCarDB.getYear().equals(year) && quoterPurchaserDB.getPersonalId().equals(purchaserId) &&
                        quoterPurchaserDB.getName().equals(purchaserName) && quoterPurchaserDB.getEmail().equals(purchaserEmail)) {
                    userQuoter = quoterDB;
                    isQuoter = true;
                    break;
                }
            }
            // Si la cotización no se encontró con los datos actuales de la solicitud, se crea porque definitivamente no existe
            if(!isQuoter) {
                QuoterOwnerModel quoterOwner = new QuoterOwnerModel("", "", "", "");
                QuoterCarModel quoterCar = quoterHelper.buildDefaultVehicle(true, ppu, brand, model, year);
                QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel(purchaserId, purchaserName, purchaserPaternalSur, purchaserMaternalSur, purchaserEmail, purchaserPhone, ownerRelationOption);
                // Creamos nueva cotización y la persistimos
                userQuoter = quoterHelper.createQuoteStructure(quoterOwner, quoterCar, quoterPurchaser, pointOfQuoterCurrentStatus, currentDateTime);
                userDB.addQuoter(userQuoter);
                userDB = userRepository.save(userDB);
            }
        }
        // Ahora entregaremos los planes, dependiendo de la aseguradora, enviando los datos del vehículo verificado.
        List<TestPlanDto> planList = new ArrayList<>();
        InsurerModel returnInsurerDB = new InsurerModel("", "", "", "", "");
        returnInsurerDB.setInsurerId(new ObjectId());
        Optional<InsurerModel> insurerOptional = insurerRepository.findByAlias(insurerAlias);
        String errorPlanFinder = "1"; // Error no se encontró una aseguradora para la búsqueda de planes
        String errorMessage = "No se encontro la aseguradora con el alias '" + insurerAlias + "'";
        String requestBody = "";
        String responseStr = "";
        // Si es una consulta a una aseguradora de prueba, se juega con delay para mejor simulación
        if(insurerOptional.isPresent()) {
            returnInsurerDB = insurerOptional.get();
            switch(insurerAlias) {
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
                    String[] brandAndModelId = apiBciProvider.findBrandAndModelId(brandRepository, "BCI", brand, model);
                    errorPlanFinder = brandAndModelId[0];
                    errorMessage = brandAndModelId[1];
                    if(errorPlanFinder.equals("") && errorMessage.equals("")) {
                        // Se pudo encontrar el ids de la aseguradora tanto para consultar por marca y modelo
                        String brandId = brandAndModelId[2];
                        String modelId = brandAndModelId[3];
                        Map<String, Object> searchPlanBCI = apiBciProvider.getPlansFromBCI(purchaserId, brandId, modelId, Integer.parseInt(year));
                        errorPlanFinder = (String) searchPlanBCI.get("errorPlanFinder");
                        errorMessage = (String) searchPlanBCI.get("errorMessage");
                        requestBody = (String) searchPlanBCI.get("requestBody");
                        responseStr = (String) searchPlanBCI.get("responseStr");
                        if(errorPlanFinder.equals("0")) {
                            planList = (List<TestPlanDto>) searchPlanBCI.get("plans");
                        }
                    }
                    
                }
            }
        }

        // Guardar planes en BD en caso de no existir
        for(TestPlanDto insurerPlan : planList) {
            String insurerPlanId = insurerPlan.getPlanId();
            @SuppressWarnings("null")
            Optional<PlanModel> optionalPlan = planRepository.findById(insurerPlanId);
            if(optionalPlan.isEmpty()) {
                PlanModel novaPlan = new PlanModel(insurerPlanId, insurerPlan.getInsurer(), insurerPlan.getPlanName(),
                        insurerPlan.getDeductibleDesc(), insurerPlan.getStolenVehicle(), insurerPlan.getTotalLoss(),
                        insurerPlan.getDamageThirdParty(), insurerPlan.getWorkshopType(), insurerPlan.getDetails(),
                        currentDateTime, currentDateTime);
                planRepository.save(novaPlan);
            }
        }

        @SuppressWarnings("null") // Se validaron todos los escenarios y siempre se crea una cotización, por lo tanto, existe id
        ResultQuoteDto resultQuote = new ResultQuoteDto(userQuoter.getQuoterId(), errorPlanFinder, errorMessage, requestBody, responseStr, returnInsurerDB, planList);
        return ResponseHelper.ok("se ha realizado la cotización", resultQuote);
    }

    @Override
    public ResponseEntity<?> selectPlan(SelectPlanRequest planSelected, String emailAuth) {
        // Los datos del plan seleccionado ya han sido validados
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<QuoterModel> quoters = userDB.getQuoters();
        String quoterId = planSelected.quoterId();
        String pointOfQuoterCurrentStatus = "Recopilando";
        // Buscamos al cotizador mediante al id y por el estado del flujo anterior o el actual, en caso de que el
        // usuario desee cambiar de plan y datos de inspección
        for(QuoterModel quoterDB : quoters) {
            String quoterStatusDB = quoterDB.getQuoterStatus();
            String quoterDBId = quoterDB.getQuoterId();
            if((quoterStatusDB.equals("Cotizando") || quoterStatusDB.equals(pointOfQuoterCurrentStatus)) &&
                    quoterId.equals(quoterDBId)) {
                // Se encontró la cotización por lo tanto se pueden obtener los datos y seguir con el flujo
                QuoterOwnerModel quoterOwner = quoterDB.getQuoterOwnerData();
                quoterOwner.setName(planSelected.ownerName().strip()); // Usamos strip() para quitar espacios al inicio y final
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
                // Actualizamos la dirección de la cotización
                QuoterAddressModel quoterAddress = quoterDB.getQuoterAddressData();
                quoterAddress.setStreet(planSelected.street().strip()); 
                quoterAddress.setStreetNumber(planSelected.streetNumber().strip());
                quoterAddress.setDepartment((!DataHelper.isNull(planSelected.department())) ? planSelected.department().strip() : "");
                // Actualizamos el estado del flujo, la fecha de actualización del cotizador y la base de datos.
                quoterDB.setQuoterStatus(pointOfQuoterCurrentStatus);
                quoterDB.setUpdatedDate(LocalDateTime.now());
                userDB = userRepository.save(userDB);
                return ResponseHelper.ok("se ha seleccionado el plan de la cotización", DataHelper.buildUser(userDB, "quoterId", quoterId));
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    @Override
    public ResponseEntity<?> generateTransaction(GenerateTransactionRequest generateTransaction, String emailAuth, String requestEndpoint) {
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userCId = userC.getUserId();
        String quoterId = generateTransaction.quoterId();
        quoterId = (!DataHelper.isNull(quoterId) && ObjectId.isValid(quoterId)) ? quoterId : "";
        if(!quoterId.equals("")) {
            // El id del cotizador cumple con el formato, para buscar un registro específico
            List<QuoterModel> quoters = userC.getQuoters();
            for(QuoterModel quoterDB : quoters) {
                String quoterIdDB = quoterDB.getQuoterId();
                String quoterStatusDB = quoterDB.getQuoterStatus();
                if(quoterId.equals(quoterIdDB) && quoterStatusDB.equals("Recopilando")) {
                    if(transactionRepository.existsByUserIdAndQuoterId(userCId, quoterId)) {
                        return ResponseHelper.gone("transacción existente que está siendo procesada", null);
                    }
                    // Se comienza a generar la transacción con las comisiones debidas
                    String transactionId = new ObjectId().toString();
                    int commissionScope = 1; // Comienzo de nivel encontrado para entregar comisiones
                    int commissionTotal = commissionUserC; // Comienzo de la comisión total que se lleva la venta
                    String pointOfCurrentStatus = "Pendiente";
                    LocalDateTime currentDateTime = LocalDateTime.now();
                    TransactionModel novaTransaction = quoterHelper.generateNovaTransactionStructure(transactionId, userCId, quoterDB, pointOfCurrentStatus, commissionTotal, commissionScope, "La comisión está siendo procesada", currentDateTime);
                    List<UserModel> users = new ArrayList<>(); // Usuarios que se tienen que actualizar por el ajuste de la wallet
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
                        // IMPORTANTE: Se busca un userB que haya referido al userC, para agregar la comisión correspondiente.
                        // Si el usuario que está refiriendo está activado, tiene que haber un registro en la colección de
                        // 'users', si no se encuentra se maneja con una respuesta errada con try/catch.
                        Optional<ReferredModel> referredByUserB = referredRepository.findByReferred(emailAuth);
                        if(referredByUserB.isPresent() && referredByUserB.get().getUserReferringStatus().equals("Activado")) {
                            // Buscamos el usuario referidor y actualizamos wallet
                            emailUserB = referredByUserB.get().getUserReferring();
                            UserModel userB = userRepository.findByPersonalData_Email(emailUserB).orElseThrow();
                            WalletModel walletB = userB.getWallet();
                            walletB.setOutstandingBalance(walletB.getOutstandingBalance() + commissionUserB);
                            walletB.setTotalBalance(walletB.getOutstandingBalance() + walletB.getAvailableBalance());
                            users.add(userB);
                            // Ajustamos valores de transacción y agregamos comisión
                            commissionScope=2;
                            commissionTotal += commissionUserB;
                            novaTransaction.addCommission(new TransactionComissionModel(userB.getUserId(), commissionUserB, pointOfCurrentStatus));
                            // IMPORTANTE: Se busca un userA que haya referido al userB, para agregar la comisión correspondiente.
                            Optional<ReferredModel> referredByUserA = referredRepository.findByReferred(emailUserB);
                            if(referredByUserA.isPresent() && referredByUserA.get().getUserReferringStatus().equals("Activado")) {
                                // Buscamos el usuario referidor y actualizamos wallet
                                emailUserA = referredByUserA.get().getUserReferring();
                                UserModel userA = userRepository.findByPersonalData_Email(emailUserA).orElseThrow();
                                WalletModel walletA = userA.getWallet();
                                walletA.setOutstandingBalance(walletA.getOutstandingBalance() + commissionUserA);
                                walletA.setTotalBalance(walletA.getOutstandingBalance() + walletA.getAvailableBalance());
                                users.add(userA);
                                // Ajustamos valores de transacción y agregamos comisión
                                commissionScope=3;
                                commissionTotal += commissionUserA;
                                novaTransaction.addCommission(new TransactionComissionModel(userA.getUserId(), commissionUserA, pointOfCurrentStatus));
                            }
                        }
                    } catch(NoSuchElementException e) {
                        // En caso de haber excepción, seguimos ya que no se alcanza a actualizar ningún dato esencial y entregamos mensaje de excepción
                        String referredNotFound = (novaTransaction.getCommissionScope() == 1) ? emailUserB : emailUserA;
                        message = "Ha ocurrido una excepción en la transacción N°" + transactionId + ", el referido no fue encontrado: " + referredNotFound + "\n" + e.getMessage();
                        novaTransaction.setUserReferringFound(false);
                    }
                    // Se actualiza el nivel de comisiones que se alcanzo a entregar la transacción (referidos).
                    novaTransaction.setCommissionScope(commissionScope);
                    novaTransaction.setCommissionTotal(commissionTotal);
                    // Se actualizan el estado, fecha de actualización y se envía el detalle del plan que se está cotizando al usuario
                    quoterDB.setQuoterStatus(pointOfCurrentStatus);
                    quoterDB.setUpdatedDate(currentDateTime);
                    emailProvider.sendQuoteDetails(userC, quoterDB);
                    // Guardamos en la base de datos y retornamos el usuario de la consulta (userC), id del cotizador, y id de la transacción
                    userRepository.saveAll(users);
                    transactionRepository.save(novaTransaction);
                    Map<String, Object> data = Map.of("quoterId", quoterId, "transactionId", transactionId, "message", message);
                    return ResponseHelper.ok("la trasacción se ha realizado correctamente", DataHelper.buildUser(userC, data));
                }
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    @Override
    public ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, String emailAuth, String requestEndpoint) {
        // Obtenemos la data del cuerpo de la solicitud y corroboramos que sea correcta
        String quoterId = finalizeQuote.quoterId();
        String pointOfTransactionStatus = finalizeQuote.transactionStatus();
        if(DataHelper.isNull(quoterId) || !ObjectId.isValid(quoterId) || DataHelper.isNull(pointOfTransactionStatus) ||
                (!pointOfTransactionStatus.equals("Aprobado") && !pointOfTransactionStatus.equals("Rechazado") &&
                !pointOfTransactionStatus.equals("Caducado")) ) {
            return ResponseHelper.failedDependency("la data proporcionada no es correcta", "failed dependency");
        }
        // Buscamos un cotizador del usuario con el mismo id y que tenga el estado del último flujo "Pendiente"
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        Map<String, Object> returnData = new HashMap<>();
        String transactionId = "";
        String message = "";
        for(QuoterModel quoterDB : userC.getQuoters()) {
            String quoterIdDB = quoterDB.getQuoterId();
            String quoterStatusDB = quoterDB.getQuoterStatus();
            if(quoterId.equals(quoterIdDB) && quoterStatusDB.equals("Pendiente")) {
                // Se intenta cerrar la venta, dependiendo del estado entregado
                TransactionModel transactionDB = transactionRepository.findByUserIdAndQuoterId(userC.getUserId(), quoterId).orElseThrow();
                String transactionStatusDB = transactionDB.getStatus();
                if(!transactionStatusDB.equals("Pendiente") || !transactionDB.getUserReferringFound()) {
                    return ResponseHelper.failedDependency("el estado de la transacción es: " + transactionStatusDB, "failed dependency");
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
                walletC.setAvailableBalance((isTrasactionApproved) ? (availableBalanceC + commissionUserC) : availableBalanceC);
                availableBalanceC = walletC.getAvailableBalance();
                // Actualizamos el saldo total
                walletC.setTotalBalance(outstandingBalanceC + availableBalanceC);
                updateUsers.add(userC);
                try {
                    // IMPORTANTE: Se busca un userB que haya referido al userC, para actualizar la comisión correspondiente,
                    // siempre y cuando confirmemos con el campo 'commissionScope'
                    if(commissionScope > 1) {
                        // La comisión alcanza a un referido y por lo tanto se encuentra 'Activado'
                        ReferredModel referredByUserB = referredRepository.findByReferred(emailAuth).orElseThrow();
                        String emailUserB = referredByUserB.getUserReferring();
                        String codeToReferB = referredByUserB.getCodeToRefer();
                        UserModel userB = userRepository.findByPersonalData_EmailAndCodeToRefer(emailUserB, codeToReferB).orElseThrow();
                        // Actualizamos los valores de la wallet del usuario B
                        WalletModel walletB = userB.getWallet();
                        int outstandingBalanceB = walletB.getOutstandingBalance() - commissionUserB;
                        walletB.setOutstandingBalance(outstandingBalanceB);
                        // Actualizamos el dinero disponible dependiendo del estado de la transacción
                        int availableBalanceB = walletB.getAvailableBalance();
                        walletB.setAvailableBalance((isTrasactionApproved) ? (availableBalanceB + commissionUserB) : availableBalanceB);
                        availableBalanceB = walletB.getAvailableBalance();
                        // Actualizamos el saldo total
                        walletB.setTotalBalance(outstandingBalanceB + availableBalanceB);
                        updateUsers.add(userB);
                        // IMPORTANTE: Se busca un userA en caso de que el alcance de comisión sea mayor a 2
                        if(commissionScope > 2) {
                            // La comisión alcanzo a otro referido y por lo tanto se encuentra 'Activado'
                            ReferredModel referredByUserA = referredRepository.findByReferred(emailUserB).orElseThrow();
                            String emailUserA = referredByUserA.getUserReferring();
                            String codeToReferA = referredByUserA.getCodeToRefer();
                            UserModel userA = userRepository.findByPersonalData_EmailAndCodeToRefer(emailUserA, codeToReferA).orElseThrow();
                            // Actualizamos los valores de la wallet del usuario A
                            WalletModel walletA = userA.getWallet();
                            int outstandingBalanceA = walletA.getOutstandingBalance() - commissionUserA;
                            walletA.setOutstandingBalance(outstandingBalanceA);
                            // Actualizamos el dinero disponible dependiendo del estado de la transacción
                            int availableBalanceA = walletA.getAvailableBalance();
                            walletA.setAvailableBalance((isTrasactionApproved) ? (availableBalanceA + commissionUserA) : availableBalanceA);
                            availableBalanceA = walletA.getAvailableBalance();
                            // Actualizamos el saldo total
                            walletA.setTotalBalance(outstandingBalanceA + availableBalanceA);
                            updateUsers.add(userA);
                        } // En caso que haya usuario A
                    } // En caso que haya usuario B
                } catch(NoSuchElementException e) {
                    // En caso de haber excepción, identificamos donde se dió la excepción durante la búsqueda del usuario referidor A o B, teniendo en consideración el obj para actualizar los usuarios y entregamos el mensaje informativo
                    int updateUsersSize = updateUsers.size();
                    message = "Ha ocurrido un excepción en la transacción N°" + transactionId + ", el alcance de la comisión es " + String.valueOf(commissionScope) + ", y ";
                    if(updateUsersSize == 1) { // No se pudo encontrar userB
                        message += "no se ha podido encontrar el usuario referidor B";
                        message += (commissionScope == 2) ? message : " y por lo tanto, tampoco el usuario referidor A";
                    } else {  // No se pudo encontrar userA
                        message += "no se ha podido encontrar el usuario referidor A";
                    }
                }
                // Se ajustan data general
                quoterDB.setQuoterStatus(pointOfTransactionStatus);
                quoterDB.setUpdatedDate(currentDateTime);
                transactionDB.setStatus(pointOfTransactionStatus);
                transactionDB.setUpdatedDate(currentDateTime);
                transactionDB.setApprovalDate((isTrasactionApproved) ? currentDateTime : transactionDB.getApprovalDate()); // Se actualiza la fecha de aprobación de la cotización, solo si la transacción es aprobada.
                returnData.put("quoterId", quoterId);
                returnData.put("transactionId", transactionId);
                returnData.put("message", message);
                // Se comprueba que los usuarios referidores fueron encontrados en caso de que la transacción tenga un alcance de comisión mayor a 1
                int updateUsersSize = updateUsers.size();
                if(commissionScope > 1 && updateUsersSize != commissionScope) { // Actualizar solo comisiones de los usuarios que se les actualizo la wallet
                    // Se actualiza el registro que si sabemos que actualizó su wallet (usuario de la transacción)
                    transactionDB.setUserReferringFound(false);
                    transactionDB.setObservation("La comisión ha sido " + pointOfTransactionStatus + ", pero hay usuarios pendientes");
                    String transactionUserId = transactionDB.getUserId();
                    for(TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                        String transactionCommissionUserId = transactionCommission.getUserId();
                        if(transactionUserId.equals(transactionCommissionUserId)) {
                            transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                            break;
                        }
                    }
                    // En caso de que no se encontró usuario referidor A, quiere decir que se encontró el usuario referidor B,
                    // por lo tanto, también se le actualiza su registro en las comisiones, ya que, se logró actualizar su wallet
                    if(commissionScope == 3 && updateUsersSize == 2) { // No se encontró usuario referidor A
                        for(UserModel updateUser : updateUsers) {
                            String userIdFromUpdateUser = updateUser.getUserId();
                            if(!transactionUserId.equals(userIdFromUpdateUser)) {
                                for(TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                                    String transactionCommissionUserId = transactionCommission.getUserId();
                                    if(userIdFromUpdateUser.equals(transactionCommissionUserId)) {
                                        transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                                        break; // Estado de comisión de usuario referidor B actualizado
                                    }
                                }
                                break; // Se encontró usuario referidor B
                            }
                        }
                    }
                    // Se actualizo lo necesario, por lo tanto, se realiza la petición entregando el mensaje informativo
                    userRepository.saveAll(updateUsers);
                    transactionRepository.save(transactionDB);
                    return ResponseHelper.accepted("la transacción se ha actualizado y necesita revisión", DataHelper.buildUser(userC, returnData));
                }
                // Transacción sin usuario referidor, se actualiza y se termina solicitud
                for(TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
                    transactionCommission.setCommissionStatus(pointOfTransactionStatus);
                }
                transactionDB.setObservation("La comisión ha sido " + pointOfTransactionStatus);
                userRepository.saveAll(updateUsers);
                transactionRepository.save(transactionDB);
                return ResponseHelper.ok("la transacción se ha finalizado correctamente", DataHelper.buildUser(userC, returnData));
            } else if(quoterId.equals(quoterIdDB) && (quoterStatusDB.equals("Aprobado") || quoterStatusDB.equals("Rechazado") || quoterStatusDB.equals("Caducado"))) {
                try {
                    transactionId = transactionRepository.findByUserIdAndQuoterId(userC.getUserId(), quoterId).orElseThrow().getTransactionId();
                } catch(NoSuchElementException e) {
                    LOGGER_MESSAGES.info("No es posible identificar id de transacción: " + e.getMessage());
                }
                returnData.put("quoterId", quoterId);
                returnData.put("transactionId", transactionId);
                returnData.put("message", message);
                return ResponseHelper.imUsed("la cotización ya ha sido finalizada y se encuentra: " + quoterStatusDB, DataHelper.buildUser(userC, returnData));
            }
        }
        String errorMessage = "no es posible encontrar la cotización N°" + quoterId + ", del usuario: " + emailAuth;
        return ResponseHelper.failedDependency(errorMessage, "failed dependency");
    }

    // Servicio que genera reporte de pago pendiente de comisiones, con fecha de recolección de comisiones hasta los días 5 del mes y que se pagan los días 10 del mes
    @Override
    @Transactional
    public ResponseEntity<?> commissionReport(CommissionReportRequest commissionReportRequest, HttpServletRequest request) {
        if(!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("Api-Key-MoneyFy"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        // Obtenemos la fecha de corte y la fecha de pago
        // DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate pointOfCurrentDate = LocalDate.now();
        LocalDate currentDateProccess = pointOfCurrentDate;
        int minusMonth = (commissionReportRequest.minusMonth() != null) ? commissionReportRequest.minusMonth() : 0;
        if(minusMonth > 0 && minusMonth < 7) { // Se puede crear un reporte hasta con 6 meses de antiguedad de comisiones
            currentDateProccess = pointOfCurrentDate.minusMonths(minusMonth).with(TemporalAdjusters.lastDayOfMonth());
        }
        // Revisar que el día de corte del mes (5), haya pasado para seguir con la consulta
        if(currentDateProccess.getDayOfMonth() <= commissionCutoffDate) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud, ya que, aún se pueden recolectar comisiones hasta la fecha de corte (día 5 del mes)", Map.of("currentDate", pointOfCurrentDate));
        }
        // Ya paso el día de corte del mes para la recolección de las comisiones, se puede seguir con la validación
        LocalDate pointOfCommissionCollectionDate = currentDateProccess.withDayOfMonth(commissionCutoffDate);
        LocalDate pointOfPaymentDate = currentDateProccess.withDayOfMonth(commissionPaymentDate);
        LocalDateTime limitDateTime = pointOfCommissionCollectionDate.atTime(LocalTime.MAX); // // Esto crea un LocalDateTime: yyyy-MM-ddT23:59:59.999999999
        List<TransactionModel> transactionsFromCutoffDate = transactionRepository.findAllByApprovalDateBeforeAndStatusApproved(limitDateTime);
        // Empezamos a revisar todas las transacciones aprobadas hasta el día de corte y guardar los ids de los usuarios sin repetirlos para luego construir respuesta
        List<ReportUserDto> usersApproved = new ArrayList<>();
        List<ReportUserDto> usersProblem = new ArrayList<>();
        for(TransactionModel transactionFromCutoffDate : transactionsFromCutoffDate) {
            // Tenemos que asegurarnos que por la transacción que estemos pasando, no exista problema de referidos
            String transactionId = transactionFromCutoffDate.getTransactionId();
            String transactionUserId = transactionFromCutoffDate.getUserId();
            Boolean isUserReferringFound = transactionFromCutoffDate.getUserReferringFound();
            if(isUserReferringFound == null || !isUserReferringFound) {
                quoterHelper.checkReportUsersProblem(usersProblem, transactionUserId, "", "", transactionId, "Existe problema de referidos");
                continue;
            }
            for(TransactionComissionModel transactionCommission : transactionFromCutoffDate.getCommissions()) {
                String commissionUserId = transactionCommission.getUserId();
                int commissionOfUser = transactionCommission.getUserCommission();
                quoterHelper.checkReportUsersApproved(usersApproved, commissionUserId, "", "", null, transactionId, String.format("Nueva comisión $%s", commissionOfUser), commissionOfUser);
            }
        }
        // Ya agregamos a todas las transacciones aprobadas y el id del usuario respectivo más su comisión, ahora buscamos al
        // usuario si es posible para actualizar sus datos (en ambos arreglos)
        for(ReportUserDto userApproved : usersApproved) {
            try {
                UserModel userDB = userRepository.findById(new ObjectId(userApproved.getUserId())).orElseThrow();
                UserDataModel userData = userDB.getPersonalData();
                String email = userData.getEmail();
                if(UserHelper.isTestUser(email) || UserHelper.isDefaulUser(email)) {
                    // No se contabiliza usuario porque es el usuario de la aplicación
                    quoterHelper.addUserProblem(usersProblem, userApproved, "Usuario de prueba de la aplicación");
                    usersApproved.remove(userApproved);
                    continue;
                }
                userApproved.setName(userData.getName() + " " + userData.getSurname());
                userApproved.setEmail(email);
                AccountModel userAccount = quoterHelper.checkUserAccount(userDB);
                if(userAccount != null) {
                    userApproved.setAccount(new CommissionAccountDto(userAccount.getAccountId(), userAccount.getHolderName(), userAccount.getEmail(), userAccount.getBank(), userAccount.getAccountType(), userAccount.getAccountNumber()));
                } else {
                    quoterHelper.addUserProblem(usersProblem, userApproved, "No es posible encontrar una cuenta bancaria activa del usuario");
                    usersApproved.remove(userApproved);
                }
            } catch(IllegalArgumentException e) {
                // El ObjectId no es correcto
                quoterHelper.addUserProblem(usersProblem, userApproved, "No es posible encontrar al usuario por su Id");
                usersApproved.remove(userApproved);
            } catch(NoSuchElementException e) {
                // No se encontró el registro
                quoterHelper.addUserProblem(usersProblem, userApproved, "No es posible encontrar al usuario por su Id");
                usersApproved.remove(userApproved);
            }
        }
        // Arreglo con usuarios con problemas
        for(ReportUserDto userProblem : usersProblem) {
            try {
                UserModel userDB = userRepository.findById(new ObjectId(userProblem.getUserId())).orElseThrow();
                UserDataModel userData = userDB.getPersonalData();
                userProblem.setName(userData.getName() + " " + userData.getSurname());
                userProblem.setEmail(userData.getEmail());
            } catch(IllegalArgumentException e) {
                // El ObjectId no es correcto
                userProblem.setName("El ObjectId del usuario no es correcto");
                userProblem.setEmail("El ObjectId del usuario no es correcto");
            } catch(NoSuchElementException e) {
                // No se encontró el registro
                userProblem.setName("El registro del usuario no pudo ser encontrado");
                userProblem.setEmail("El registro del usuario no pudo ser encontrado");
            }
        }
        // Construcción para data de respuesta
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pointOfCurrentDate", pointOfCurrentDate);
        data.put("pointOfCommissionCollectionDate", pointOfCommissionCollectionDate);
        data.put("pointOfPaymentDate", pointOfPaymentDate);
        data.put("usersApproved", usersApproved);
        data.put("usersProblem", usersProblem);
        return ResponseHelper.ok("se ha generado el reporte", data);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public ResponseEntity<?> commissionPayments(CommissionPaymentRequest commissionPaymentRequest) {
        // Revisamos si la llave de la solicitud hace match con la del backend, de otra manera, no puede seguir con la solicitud
        String key = commissionPaymentRequest.key();
        List<CommissionPaymentDto> payments = commissionPaymentRequest.payments();
        if(DataHelper.isNull(key) || !key.equals(apiKeyMF) || payments == null) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }
        // Se buscan los usuarios para actualizar las comisiones que fueron pagadas (TRATAR LUEGO DE LLEVAR LA LÓGICA AL HELPER)
        List<UserModel> updateUsers = new ArrayList<>();
        List<TransactionModel> updateTransactions = new ArrayList<>();
        List<PaymentModel> listUserPayments = new ArrayList<>();
        String lastStatus = "Liberado";
        String confirmationStatus = "Confirmando";
        LocalDateTime currenDateTime = LocalDateTime.now();
        // Buscamos las transacciones, para actualizar sus comisiones, actualizamos la wallet del usuario y creamos la
        // estructura para los pagos de comisiones realizadas
        String errorCommissionPayments = quoterHelper.updateCommissionPayments(payments, updateUsers, updateTransactions,
                listUserPayments, lastStatus, confirmationStatus, currenDateTime, transactionRepository, userRepository);
        if(errorCommissionPayments != null) {
            return ResponseHelper.failedDependency(errorCommissionPayments, "failed dependency");
        }
        // Perfecto, no hubo error, y ahora falta solamente iterar por las transacciones que se deben de actualizar, para
        // saber si todas las comisiones de esa transacción fueron pagadas, si no actualizar los estados de comisiones y
        // el de la transacción
        Map<String, Object> dataUpdated = quoterHelper.confirmingTransactionStatus(updateTransactions, updateUsers, listUserPayments, lastStatus, confirmationStatus, currenDateTime);
        // Se asignan los registros actualizados
        List<String> transactionIds = (List<String>) dataUpdated.get("transactionIds");
        List<String> userIds = (List<String>) dataUpdated.get("userIds");
        List<String> paymentIds = (List<String>) dataUpdated.get("paymentIds");
        // Se actualizan los registros en la base de datos
        transactionRepository.saveAll(updateTransactions);
        userRepository.saveAll(updateUsers);
        paymentRepository.saveAll(listUserPayments);
        return ResponseHelper.ok("las comisiones se han actualizado, juntamente con la información relacionada", Map.of("transactionIds", transactionIds, "userIds", userIds, "paymentIds", paymentIds));
    }

    // SERVICIOS UTILIZADOS PARA REALIZAR PRUEBAS Y LÓGICAS DE LA APLICACIÓN
    @Override
    public ResponseEntity<?> viewTestData() {
        List<QuoterOwnerModel> ownerList = quoterHelper.ownerList();
        List<QuoterCarModel> vehicleList = quoterHelper.vehicleList();
        List<TestPlanDto> planList1 = quoterHelper.planList1();
        List<TestPlanDto> planList2 = quoterHelper.planList2();
        List<TestPlanDto> planList3 = quoterHelper.planList3();
        return ResponseHelper.ok("se han podido recuperar los datos de prueba", Map.of("owners", ownerList, "vehicles", vehicleList, "planList1", planList1, "planList2", planList2, "planList3", planList3));
    }

    @Override
    public String testNovaFunctions() {
        // COMPARACIÓN DE FECHAS
        // LocalDate startDate = LocalDate.of(2024, 5, 20);
        // LocalDate endDate = LocalDate.of(2024, 12, 20);
        // long daysBetween = endDate.toEpochDay() - startDate.toEpochDay();
        // return String.valueOf(daysBetween) + " - " + startDate.getYear();
        // OBTENCIÓN DE RUT SIN GUÍON Y PUNTOS, Y OBTENCIÓN DEL DV
        // return "12.345.678-9".replace(".", "").substring(0, "12.345.678-9".replace(".", "").length()-2);
        // return "12.345.678-9".split("-")[0].replace(".", "");
        String resultado = "12.345.678-9".substring("12.345.678-9".length()-1);
        return resultado;
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
