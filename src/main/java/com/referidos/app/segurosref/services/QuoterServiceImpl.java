package com.referidos.app.segurosref.services;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.referidos.app.segurosref.dtos.commission.CommissionPaymentDto;
import com.referidos.app.segurosref.dtos.commission.CommissionReportDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.QuoterHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.models.InsurerModel;
import com.referidos.app.segurosref.models.LogModel;
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
import com.referidos.app.segurosref.models.UserModel;
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

@Service
public class QuoterServiceImpl implements QuoterService {

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

    @Value(value = "${api.key.moneyfy.seed}")
    private String apiKeyMoneyFy;

    private final int commissionUserC = 35000;
    
    private final int commissionUserB = 10000;
    
    private final int commissionUserA = 5000;

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
        String quoterCurrentStatus = "Cotizando";
        LocalDateTime currentDateTime = LocalDateTime.now();
        if(!quoterId.equals("")) {
            for(QuoterModel quoterDB : quoters) {
                String quoterDBId = quoterDB.getQuoterId();
                if(quoterDBId.equals(quoterId)) {
                    if(!quoterDB.getQuoterStatus().equals("Iniciando") && !quoterDB.getQuoterStatus().equals(quoterCurrentStatus)) {
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
                        quoterDB.setQuoterStatus(quoterCurrentStatus);
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
                if(quoterDB.getQuoterStatus().equals(quoterCurrentStatus) && quoterCarDB.getPpu().equals(ppu) && 
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
                userQuoter = quoterHelper.createQuoteStructure(quoterOwner, quoterCar, quoterPurchaser, quoterCurrentStatus, currentDateTime);
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
        // Los datos del plan seleccionado han sido validados anteriormente
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<QuoterModel> quoters = userDB.getQuoters();
        String quoterId = planSelected.quoterId();
        // Buscamos al cotizador mediante al id y por el estado del flujo anterior o el actual, en caso de que el
        // usuario desee cambiar de plan y datos de inspección
        for(QuoterModel quoterDB : quoters) {
            String quoterStatus = quoterDB.getQuoterStatus();
            String quoterDBId = quoterDB.getQuoterId();
            if((quoterStatus.equals("Cotizando") || quoterStatus.equals("Recopilando")) &&
                    quoterId.equals(quoterDBId)) {
                // Se encontró el cotizador, por lo tanto, se puede actualizar y seguir con el flujo

                // Actualizamos/confirmamos la data del dueño
                QuoterOwnerModel quoterOwner = quoterDB.getQuoterOwnerData();
                quoterOwner.setName(planSelected.ownerName().trim()); // CON TRIM() INCLUIDO (no permite saltos en línea)
                quoterOwner.setPaternalSurname(planSelected.ownerPaternalSur().trim()); // CON TRIM() INCLUIDO (no permite saltos en línea)
                quoterOwner.setMaternalSurname(planSelected.ownerMaternalSur().trim()); // CON TRIM() INCLUIDO (no permite saltos en línea)

                // Actualizamos el plan seleccionado del cotizador
                QuoterPlanModel quoterPlan = quoterDB.getQuoterPlanData();
                quoterPlan.setQuoterPlanId(planSelected.planId());
                quoterPlan.setInsurer(planSelected.insurer().trim()); // CON TRIM() INCLUIDO (permite saltos en línea)
                quoterPlan.setPlanName(planSelected.planName().trim()); // CON TRIM() INCLUIDO (permite saltos en línea)
                quoterPlan.setValueUF(planSelected.valueUF());
                quoterPlan.setGrossPriceUF(planSelected.grossPriceUF());
                quoterPlan.setTotalMonths(planSelected.totalMonths());
                quoterPlan.setMonthlyPriceUF(planSelected.monthlyPriceUF());
                quoterPlan.setMonthlyPrice(planSelected.monthlyPrice());
                quoterPlan.setDeductible(planSelected.deductible());
                quoterPlan.setDiscount(planSelected.discount());

                // Actualizamos la dirección de la cotización
                QuoterAddressModel quoterAddress = quoterDB.getQuoterAddressData();
                quoterAddress.setStreet(planSelected.street().trim());  // CON TRIM() INCLUIDO (permite saltos en línea)
                quoterAddress.setStreetNumber(planSelected.streetNumber().trim()); // CON TRIM() INCLUIDO (permite saltos en línea)
                quoterAddress.setDepartment((!DataHelper.isNull(planSelected.department())) ? planSelected.department().trim() : ""); // CON TRIM() INCLUIDO (permite saltos en línea) - opcional
                
                // Actualizamos el estado del flujo, la fecha de actualización del cotizador y la base de datos.
                quoterDB.setQuoterStatus("Recopilando");
                quoterDB.setUpdatedDate(LocalDateTime.now());
                userDB = userRepository.save(userDB);
                
                return ResponseHelper.ok("se ha seleccionado el plan de la cotización", DataHelper.buildUser(userDB, "quoterId", quoterId));
            }
        }

        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    @Transactional
    @Override
    public ResponseEntity<?> generateTransaction(GenerateTransactionRequest generateTransaction, String emailAuth, String requestEndpoint) {
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String quoterId = (!DataHelper.isNull(generateTransaction.quoterId()) && ObjectId.isValid(generateTransaction.quoterId()))
                ? generateTransaction.quoterId() : "No informado";
        if(!quoterId.equals("No informado")) {
            // El id del cotizador cumple con el formato, para buscar un registro específico
            List<QuoterModel> quoters = userC.getQuoters();
            for(QuoterModel quoterDB : quoters) {
                String quoterDBId = quoterDB.getQuoterId();
                String quoterStatus = quoterDB.getQuoterStatus();
                if(quoterId.equals(quoterDBId) && quoterStatus.equals("Recopilando")) {
                    if(transactionRepository.existsByUserIdAndQuoterId(userC.getUserId(), quoterId)) {
                        return ResponseHelper.gone("transacción existente que está siendo procesada", null);
                    }
                    // Se comienza a generar la transacción con las comisiones debidas
                    String transactionId = new ObjectId().toString(); // Nueva transacción
                    int commissionScope = 1; // El nivel encontrado para entregar comisiones
                    int commissionTotal = commissionUserC; // Comienzo de la comisión total que se lleva la venta
                    String currentStatus = "Pendiente"; // Estado del flujo actual
                    LocalDateTime currentDateTime = LocalDateTime.now();
                    TransactionModel novaTransaction = quoterHelper.generateNovaTransactionStructure(transactionId, userC, quoterDB, commissionTotal, currentStatus, currentDateTime);
                    List<UserModel> users = new ArrayList<>(); // Usuarios que se tienen que actualizar por el ajuste de la wallet
                    // Comenzamos a actualizar la data de la wallet del usuario.
                    WalletModel walletC = userC.getWallet();
                    walletC.setOutstandingBalance(walletC.getOutstandingBalance() + commissionUserC);
                    walletC.setTotalBalance(walletC.getOutstandingBalance() + walletC.getAvailableBalance());
                    walletC.addTransactionId(transactionId);
                    users.add(userC);
                    // Ver si existe el userB y userA, para actualizar sus wallets
                    try {
                        // IMPORTANTE: Se busca un userB que haya referido al userC, para agregar la comisión correspondiente.
                        // Si el usuario que está refiriendo está activado, tiene que haber un registro en la colección de
                        // 'users', si no se encuentra se maneja con una respuesta errada con try/catch.
                        Optional<ReferredModel> referredByUserB = referredRepository.findByReferred(emailAuth);
                        if(referredByUserB.isPresent() && referredByUserB.get().getUserReferringStatus().equals("Activado")) {
                            // Ajustamos valores de transacción
                            commissionScope=2;
                            commissionTotal += commissionUserB;
                            // Buscamos el usuario referidor para actualizar su wallet
                            String emailUserB = referredByUserB.get().getUserReferring();
                            UserModel userB = userRepository.findByPersonalData_Email(emailUserB).orElseThrow();
                            WalletModel walletB = userB.getWallet();
                            walletB.setOutstandingBalance(walletB.getOutstandingBalance() + commissionUserB);
                            walletB.setTotalBalance(walletB.getOutstandingBalance() + walletB.getAvailableBalance());
                            walletB.addTransactionId(transactionId);
                            users.add(userB);
                            // Agregamos nueva comisión
                            novaTransaction.addCommission(new TransactionComissionModel(userB.getUserId(), commissionUserB, currentStatus));
                            // IMPORTANTE: Se busca un userA que haya referido al userB, para agregar la comisión correspondiente.
                            Optional<ReferredModel> referredByUserA = referredRepository.findByReferred(emailUserB);
                            if(referredByUserA.isPresent() && referredByUserA.get().getUserReferringStatus().equals("Activado")) {
                                commissionScope=3;
                                commissionTotal += commissionUserA;
                                // Buscamos el usuario referidor para actualizar su wallet
                                String emailUserA = referredByUserA.get().getUserReferring();
                                UserModel userA = userRepository.findByPersonalData_Email(emailUserA).orElseThrow();
                                WalletModel walletA = userA.getWallet();
                                walletA.setOutstandingBalance(walletA.getOutstandingBalance() + commissionUserA);
                                walletA.setTotalBalance(walletA.getOutstandingBalance() + walletA.getAvailableBalance());
                                walletA.addTransactionId(transactionId);
                                users.add(userA);
                                // Agregamos nueva comisión
                                novaTransaction.addCommission(new TransactionComissionModel(userA.getUserId(), commissionUserA, currentStatus));
                            }
                        }
                    } catch(Exception e) {
                        // Generamos log de error
                        String endpoint = !DataHelper.isNull(requestEndpoint) ? requestEndpoint : "No informado";
                        LogModel logReferredNotFound = new LogModel(null, "ERROR", "Referidor no encontrado al generar la transaccion",
                                endpoint, "Grave", "", transactionId, "", new HashMap<>(), currentDateTime, currentDateTime);
                        logReferredNotFound.addData("commissionScope", commissionScope);
                        logRepository.save(logReferredNotFound);
                        // Actualizamos estado de transacción problemática y devolvemos error
                        novaTransaction.setStatus("Generando");
                        transactionRepository.save(novaTransaction);
                        String conflictMessage = "no se ha podido recuperar la data del referidor que recibe la comisión y la transacción N°" + transactionId + " se encuentra en inspección para ser resuelta.";
                        return ResponseHelper.locked(conflictMessage, null);
                    }
                    // Se actualiza el nivel de comisiones que se alcanzo a entregar la transacción (referidos).
                    novaTransaction.setCommissionScope(commissionScope);
                    novaTransaction.setCommissionTotal(commissionTotal);
                    // Se actualizan el estado y fecha de actualización del cotizador.
                    quoterDB.setQuoterStatus(currentStatus);
                    quoterDB.setUpdatedDate(currentDateTime);

                    // Se envía el detalle del plan que se está cotizando en la aseguradora
                    emailProvider.sendQuoteDetails(userC, quoterDB);

                    // Guardamos en la base de datos
                    userRepository.saveAll(users);
                    transactionRepository.save(novaTransaction);
                    // Retornamos el usuario de la consulta (userC), id del cotizador, y id de la transacción
                    return ResponseHelper.ok("la trasacción se ha realizado correctamente", DataHelper.buildUser(userC, "quoterId", quoterId, "transactionId", transactionId));
                }
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    @Transactional
    @Override
    public ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, String emailAuth, String requestEndpoint) {
        // Obtenemos la data del cuerpo de la solicitud y corroboramos que sea correcta
        String quoterId = finalizeQuote.quoterId();
        String transactionStatus = finalizeQuote.transactionStatus();
        if(DataHelper.isNull(quoterId) || !ObjectId.isValid(quoterId) || DataHelper.isNull(transactionStatus) ||
                (!transactionStatus.equals("Aprobado") && !transactionStatus.equals("Rechazado") &&
                !transactionStatus.equals("Caducado")) ) {
            return ResponseHelper.failedDependency("la data proporcionada no es correcta", null);
        }
        // Buscamos un cotizador del usuario con el mismo id y que tenga el estado del último flujo "Pendiente"
        UserModel userC = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        for(QuoterModel quoterDB : userC.getQuoters()) {
            String quoterDBId = quoterDB.getQuoterId();
            String quoterStatus = quoterDB.getQuoterStatus();
            if(quoterId.equals(quoterDBId) && quoterStatus.equals("Pendiente")) {
                // Se intenta cerrar la venta, dependiendo del estado entregado
                TransactionModel userTransaction = transactionRepository.findByUserIdAndQuoterId(userC.getUserId(), quoterId).orElseThrow();
                String transactionStatusDB = userTransaction.getStatus();
                if(transactionStatusDB.equals("Inspeccionando")) {
                    return ResponseHelper.gone("la transacción está siendo inspeccionada", null);
                } else if(!transactionStatusDB.equals("Pendiente")) {
                    return ResponseHelper.failedDependency("la transacción no se encuentra pendiente", null);
                }
                String transactionId = userTransaction.getTransactionId();
                int commissionScope = userTransaction.getCommissionScope();
                boolean isTrasactionApproved = transactionStatus.equals("Aprobado");
                LocalDateTime currentDateTime = LocalDateTime.now();
                List<UserModel> updateUsers = new ArrayList<>();
                // Obtenemos la wallet del usuario C, para comenzar con la actualización.
                WalletModel walletC = userC.getWallet();
                int outstandingBalanceC = walletC.getOutstandingBalance() - commissionUserC;
                outstandingBalanceC = (outstandingBalanceC >= 0) ? outstandingBalanceC : 0;
                walletC.setOutstandingBalance(outstandingBalanceC);
                // Actualizamos el dinero disponible en caso de que sea aprobada la transacción
                if(isTrasactionApproved) {
                    walletC.setAvailableBalance(walletC.getAvailableBalance() + commissionUserC);
                }
                walletC.setTotalBalance(outstandingBalanceC+walletC.getAvailableBalance());
                updateUsers.add(userC);
                try {
                    // IMPORTANTE: Se busca un userB que haya referido al userC, para actualizar la comisión correspondiente,
                    // siempre y cuando confirmemos con el campo 'commissionScope'
                    if(commissionScope > 1) {
                        // La comisión alcanza a un referido
                        ReferredModel referredByUserB = referredRepository.findByReferred(emailAuth).orElseThrow();
                        String emailUserB = referredByUserB.getUserReferring();
                        UserModel userB = userRepository.findByPersonalData_Email(emailUserB).orElseThrow();
                        // Actualizamos los valores de la wallet del usuario B
                        WalletModel walletB = userB.getWallet();
                        int outstandingBalanceB = walletB.getOutstandingBalance() - commissionUserB;
                        outstandingBalanceB = (outstandingBalanceB >= 0) ? outstandingBalanceB : 0;
                        walletB.setOutstandingBalance(outstandingBalanceB);
                        // Actualizamos el dinero disponible en caso de que sea aprobada la transacción
                        if(isTrasactionApproved) {
                            walletB.setAvailableBalance(walletB.getAvailableBalance() + commissionUserB);
                        }
                        walletB.setTotalBalance(outstandingBalanceB+walletB.getAvailableBalance());
                        updateUsers.add(userB);
                        // IMPORTANTE: Se busca un userA en caso de que el alcance de comisión sea mayor a 2
                        if(commissionScope > 2) {
                            // La comisión alcanzo a otro referido
                            ReferredModel referredByUserA = referredRepository.findByReferred(emailUserB).orElseThrow();
                            String emailUserA = referredByUserA.getUserReferring();
                            UserModel userA = userRepository.findByPersonalData_Email(emailUserA).orElseThrow();
                            WalletModel walletA = userA.getWallet();
                            // Actualizamos los valores de la wallet del usuario A
                            int outstandingBalanceA = walletA.getOutstandingBalance() - commissionUserA;
                            outstandingBalanceA = (outstandingBalanceA >= 0) ? outstandingBalanceA : 0;
                            walletA.setOutstandingBalance(outstandingBalanceA);
                            // Actualizamos el saldo disponible en caso de que sea aprobada la transacción
                            if(isTrasactionApproved) {
                                walletA.setAvailableBalance(walletA.getAvailableBalance() + commissionUserA);
                            }
                            walletA.setTotalBalance(outstandingBalanceA+walletA.getAvailableBalance());
                            updateUsers.add(userA);
                        } // En caso que haya usuario A
                    } // En caso que haya usuario B
                } catch(Exception e) {
                    // Generamos log de error
                    String endpoint = !DataHelper.isNull(requestEndpoint) ? requestEndpoint : "No informado";
                    LogModel logReferredNotFound = new LogModel(null, "ERROR", "Referidor no encontrado al finalizar transaccion",
                            endpoint, "Grave", "", transactionId, "", new HashMap<>(), currentDateTime, currentDateTime);
                    logReferredNotFound.addData("commissionScope", commissionScope).put("transactionStatus", transactionStatus);
                    logRepository.save(logReferredNotFound);
                    // Actualizamos el estado de la transacción y devolvemos el error
                    userTransaction.setStatus("Inspeccionando");
                    userTransaction.setUpdatedDate(currentDateTime);
                    transactionRepository.save(userTransaction);
                    String conflictMessage = "no se ha podido recuperar la data del referidor para actualizar las comisiones y la transacción N° " + transactionId + " se encuentra en inspección para ser resuelta.";
                    return ResponseHelper.locked(conflictMessage, null);
                }
                // Actualizar datos generales de la transacción y del cotizador
                for(TransactionComissionModel transactionComission : userTransaction.getCommissions()) {
                    transactionComission.setCommissionStatus(transactionStatus);
                }
                userTransaction.setObservation("La comisión ha sido " + transactionStatus);
                userTransaction.setStatus(transactionStatus);
                userTransaction.setUpdatedDate(currentDateTime);
                quoterDB.setQuoterStatus(transactionStatus);
                quoterDB.setUpdatedDate(currentDateTime);
                // Se actualiza la fecha de aprobación de la cotización, solo si la transacción es aprobada.
                if(isTrasactionApproved) {
                    userTransaction.setApprovalDate(currentDateTime);
                }
                // Actualizamos en la base de datos
                userRepository.saveAll(updateUsers);
                transactionRepository.save(userTransaction);
                return ResponseHelper.ok("la transacción se ha finalizado correctamente", DataHelper.buildUser(userC, "quoterId", quoterId, "transactionId", transactionId));
            } else if(quoterId.equals(quoterDBId) && (quoterStatus.equals("Aprobado") || quoterStatus.equals("Rechazado") || quoterStatus.equals("Caducado"))) {
                return ResponseHelper.accepted("la cotización ya ha sido finalizada y se encuentra: " + quoterStatus, DataHelper.buildUser(userC));
            }
        }
        String errorMessage = "no es posible finalizar la cotización del usuario " + emailAuth + ", de su cotizador N°" + quoterId;
        return ResponseHelper.failedDependency(errorMessage, null);
    }

    // SERVICIOS QUE FORMAN PARTE DEL FLUJO DEL RETIRO DE DINERO DISPONIBLE DEL USUARIO
    @Override
    @Transactional
    public ResponseEntity<?> commissionReport(CommissionReportRequest commissionReportRequest, String requestEndpoint) {
        // Revisamos si la llave de la solicitud hace match con la del backend, de otra manera, no puede seguir con la solicitud
        String key = commissionReportRequest.key();
        if(DataHelper.isNull(key) || !key.equals(apiKeyMoneyFy)) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Obtenemos la fecha de corte y la fecha de pago
        LocalDateTime currentDateTime = LocalDateTime.now();
        Object[] datesForReport = quoterHelper.getCutOffDateAndPaymentDate(currentDateTime, commissionReportRequest);
        String errorCutOffDateMessage = (String) datesForReport[0];
        if(errorCutOffDateMessage != null) {
            // Hay error al tratar de obtener las fechas para el informe
            return ResponseHelper.failedDependency(errorCutOffDateMessage, null);
        }
        // No existe error y se obtienen los datos
        LocalDateTime cutoffDate = (LocalDateTime) datesForReport[1];
        LocalDateTime paymentDate = (LocalDateTime) datesForReport[2];
        DateTimeFormatter formatterString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String endpoint = !DataHelper.isNull(requestEndpoint) ? requestEndpoint : "No informado";
        // Creamos la estructura de reporte con datos por defectos y vamos rellenando
        CommissionReportDto commissionReport = new CommissionReportDto(0, 0, 0,
                cutoffDate.format(formatterString), paymentDate.format(formatterString));
        // Fecha utilizada para buscar todas las transacciones que se crearon antes de la fecha de corte
        LocalDateTime afterDate = LocalDateTime.of(cutoffDate.getYear(), cutoffDate.getMonth(), cutoffDate.getDayOfMonth()+1, 00, 00, 00);
        // Agregamos la data al reporte de comisión
        quoterHelper.generateCommissionReport(commissionReport, afterDate, endpoint, currentDateTime, transactionRepository,
                userRepository, logRepository);
        // Finalmente devolvemos una respuesta correcta, pero 202, si es que existen conflictos
        if(commissionReport.getConflicts().size() > 0) {
           return ResponseHelper.accepted("se ha generado el reporte, aunque es necesario verificar los conflictos", Map.of("commissionReport", commissionReport));
        }
        return ResponseHelper.ok("se ha generado el reporte", Map.of("commissionReport", commissionReport));
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public ResponseEntity<?> commissionPayments(CommissionPaymentRequest commissionPaymentRequest) {
        // Revisamos si la llave de la solicitud hace match con la del backend, de otra manera, no puede seguir con la solicitud
        String key = commissionPaymentRequest.key();
        List<CommissionPaymentDto> payments = commissionPaymentRequest.payments();
        if(DataHelper.isNull(key) || !key.equals(apiKeyMoneyFy) || payments == null) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
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
            return ResponseHelper.failedDependency(errorCommissionPayments, null);
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
