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

import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.dtos.ResultQuoteDto;
import com.referidos.app.segurosref.dtos.TestPlanDto;
import com.referidos.app.segurosref.dtos.VehicleBrandDto;
import com.referidos.app.segurosref.dtos.VehicleModelDto;
import com.referidos.app.segurosref.dtos.commission.CommissionPaymentDto;
import com.referidos.app.segurosref.dtos.commission.CommissionReportDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.QuoterHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
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
import com.referidos.app.segurosref.models.UserDataModel;
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
import com.referidos.app.segurosref.requests.VehicleBrandRequest;
import com.referidos.app.segurosref.requests.CommissionPaymentRequest;
import com.referidos.app.segurosref.requests.CommissionReportRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.requests.RegisterInsurerRequest;
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

    @Value(value = "${quoter.endpoint.keyword}")
    private String quoterEndpointKeyword;

    private final int commissionUserC = 35000;
    
    private final int commissionUserB = 10000;
    
    private final int commissionUserA = 5000;

    // SERVICIOS PARA INGRESAR O BUSCAR DATA RELACIONADA A LA MARCA/MODELO DE UN VEHÍCULO PARA REALIZAR LAS COTIZACIONES
    @Transactional
    @Override
    public ResponseEntity<?> registerVehicleBrands(VehicleBrandRequest vehicleBrands) {
        String key = vehicleBrands.key();
        List<BrandModel> brands = vehicleBrands.brands();
        if(key == null || !key.equals(quoterEndpointKeyword) || brands == null || brands.size() < 1) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Se actualizan la lista de marca/modelos, dependiendo de la lista entregada en el cuerpo de la solicitud
        List<BrandModel> brandsDB = quoterHelper.updateVehicleBrands(brandRepository, brands);
        if(brandsDB == null) {
            return ResponseHelper.failedDependency("data inválida", null);
        }
        brandRepository.saveAll(brandsDB);
        return ResponseHelper.ok("se han actualizado las marcas de los vehículos correctamente", Map.of("brands", brandsDB));
    }

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
        // Por cada registro de marcas de vehículos, generamos un objeto dto
        for(BrandModel brandDB : brandsDB) {
            // Buscamos todos los modelos de la marca primero y los vamos agregando a los modelos del vehículo dto
            List<VehicleModelDto> modelsDto = new ArrayList<>();
            for(BrandDataModel modelDB : brandDB.getModels()) {
                modelsDto.add(new VehicleModelDto(modelDB.getModelId(), modelDB.getModel()));
            }
            brandsDto.add(new VehicleBrandDto(brandDB.getBrandId(), brandDB.getBrand(), modelsDto));
        }
        return ResponseHelper.ok("se ha traido la lista de las marcas de los vehículos disponibles", DataHelper.buildUser(userDB, "brands", brandsDto));
    }

    // SERVICIOS PARA INGRESAR O BUSCAR ASEGURADORAS QUE PROVEEN DE LOS PLANES PARA REALIZAR LAS COTIZACIONES
    @Transactional
    @Override
    public ResponseEntity<?> registerInsurer(RegisterInsurerRequest registerInsurer) {
        String name = registerInsurer.name();
        String alias = registerInsurer.alias();
        String endpoint = registerInsurer.endpoint();
        String darkLogo = registerInsurer.darkLogo();
        String lightLogo = registerInsurer.lightLogo();

        if(DataHelper.isNull(name) || DataHelper.isNull(alias) || DataHelper.isNull(endpoint)
                || DataHelper.isNull(darkLogo) || DataHelper.isNull(lightLogo)) {
            return ResponseHelper.failedDependency("debe ingresar información válida", null);
        }
        // Vemos si ya existe por nombre o alias
        if(insurerRepository.existsByNameOrAlias(name, alias)) {
            return ResponseHelper.locked("aseguradora existente", null);
        }

        // Ingresar palabra clave para registrar la aseguradora
        String key = registerInsurer.key();
        if(!DataHelper.isNull(key) && key.equals(quoterEndpointKeyword)) {
            InsurerModel newInsurer = new InsurerModel(name, alias, endpoint, darkLogo, lightLogo);
            insurerRepository.save(newInsurer);
            return ResponseHelper.created("aseguradora agregada", Map.of("insurer", newInsurer));
        }
        
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    @Transactional
    @Override
    public ResponseEntity<?> searchInsurers(String emailAuth, String updateCredential, String device) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<String> insurers = new ArrayList<>();

        insurerRepository.findAll().forEach(insurerDB -> {
            insurers.add(insurerDB.getAlias());
        });
        
        // Revisar si se tiene que actualizar el refresh token
        if(updateCredential.equals("Dated")) {
            UserDataModel userData = userDB.getPersonalData();
            Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(emailAuth, device);
            if(deviceOptional.isPresent()) {
                // Si se encontró el dispositivo se actualiza el token, porque caducó o no se puede leer porque se actualizó la app
                DeviceModel deviceDB = deviceOptional.get();
                userData.setRefreshToken(JwtConfig.createRefreshToken(emailAuth));
                deviceDB.setRefreshToken(userData.getRefreshToken());
                userDB = userRepository.save(userDB);
                deviceRepository.save(deviceDB);
            }
        }
        
        return ResponseHelper.ok("se ha traido la lista de aseguradoras disponibles", DataHelper.buildUser(userDB, "insurers", insurers));
    }

    // SERVICIOS QUE FORMAN PARTE DEL FLUJO COMPLETO DE LA COTIZACIÓN
    @Transactional
    @Override
    public ResponseEntity<?> searchVehicle(SearchVehicleRequest searchVehicle, String emailAuth) {
        UserModel userDB = this.userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Error en caso de que el usuario aún no tenga cuentas bancarias registradas
        if(!DataHelper.accountAvailable(userDB)) {
            return ResponseHelper.locked("debe asegurarse de tener una cuenta bancaria para recibir las comisiones, antes de cotizar seguros", null);
        }

        // Obtención de datos verificados
        String ppu = searchVehicle.ppu().toUpperCase(); // Patente del vehículo a mayúsculas
        String ownerId = searchVehicle.ownerId().toUpperCase(); // Rut de propietario a mayúsculas por la 'k'

        // SIMULACIÓN DE LÓGICA PARA BÚSQUEDA DE VEHÍCULO
        // Primero buscamos vehículo por el propietario
        List<QuoterCarModel> vehiclesByOwnerId = quoterHelper.vehiclesByOwnerId(ownerId); // Simulación de obtención de vehículos por rut con formato
        QuoterCarModel vehicleFound = null;
        for(QuoterCarModel ownerVehicle : vehiclesByOwnerId) {
            if(ownerVehicle.getPpu().equals(ppu)) {
                vehicleFound = ownerVehicle;
                break;
            }
        }
        // Si no se encontro vehículo, se asigna uno de prueba
        if(vehicleFound == null) {
            vehicleFound = new QuoterCarModel(ppu, "OPEL", "CORSA", "2023", "Negro",
                    "N0V0T3STT4RB0", "N0V0T3STT3ST3R", "Stellantis");
        }

        // Buscamos si existe un cotizador con los datos de la cotización, para asignar el id del cotizador
        List<QuoterModel> quoters = userDB.getQuoters();
        String quoterId = "";
        for(QuoterModel quoterDB : quoters) {
            String quoterStatus = quoterDB.getQuoterStatus();
            String quoterOwnerId = quoterDB.getQuoterOwnerData().getPersonalId();
            QuoterCarModel quoterCar = quoterDB.getQuoterCarData();
            if(quoterStatus.equals("Iniciando") && quoterOwnerId.equals(ownerId) &&
                    quoterCar.getPpu().equals(ppu) && quoterCar.getBrand().equals(vehicleFound.getBrand()) &&
                    quoterCar.getModel().equals(vehicleFound.getModel()) && quoterCar.getYear().equals(vehicleFound.getYear())) {
                quoterId = quoterDB.getQuoterId();
            }
        }

        // Si el id del cotizador sigue vacío, es porque no se encontro un registro y por lo tanto se crea uno nuevo
        if(quoterId.equals("")) {
            QuoterOwnerModel quoterOwner = new QuoterOwnerModel(ownerId, "", "", "");
            QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel("", "", "", "", "", "", "");
            QuoterModel novaQuoter = quoterHelper.createQuoteStructure(quoterOwner, vehicleFound, quoterPurchaser,
                    "Iniciando", LocalDateTime.now());
            quoterId = novaQuoter.getQuoterId();
            userDB.addQuoter(novaQuoter);
            userDB = userRepository.save(userDB);
        }

        return ResponseHelper.created("se ha realizado la cotización exitosamente", DataHelper.buildUser(userDB, "vehicle", vehicleFound, "quoterId", quoterId));
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public ResponseEntity<?> searchPlan(SearchPlanRequest searchPlan, String emailAuth) {
        // Si llega, es porque se validaron los datos, por lo tanto, los recuperamos
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String quoterId = (!DataHelper.isNull(searchPlan.quoterId())) ? searchPlan.quoterId() : "No informado"; // Campo opcional
        String ppu = searchPlan.ppu().toUpperCase();
        String brand = searchPlan.brand();
        String model = searchPlan.model();
        String year = searchPlan.year();
        String insurerAlias = searchPlan.insurerAlias().trim(); // CON TRIM() INCLUIDO (permite saltos en línea)
        
        // SIMULACIÓN DE LÓGICA PARA BÚSQUEDA DE VEHÍCULO
        // Resulta que si se hace manual, tiene que existir la validación de que exista el vehículo para obtener los datos del dueño
        // String requestType = searchPlan.requestType(); // Para validar si la búsqueda de planes se está haciendo "Manual" o "Auto"
        
        String purchaserId = searchPlan.purchaserId(); // id del comprador (rut)
        String purchaserName = searchPlan.purchaserName().trim(); // CON TRIM() INCLUIDO (no permite saltos en línea)
        String purchaserPaternalSur = searchPlan.purchaserPaternalSur().trim(); // CON TRIM() INCLUIDO (no permite saltos en línea)
        String purchaserMaternalSur = searchPlan.purchaserMaternalSur().trim(); // CON TRIM() INCLUIDO (no permite saltos en línea)
        String purchaserEmail = searchPlan.purchaserEmail();
        String purchaserPhone = !DataHelper.isNull(searchPlan.purchaserPhone()) ? searchPlan.purchaserPhone() : ""; // Opcional
        String ownerRelationOption = searchPlan.ownerRelationOption(); // Depende de la aseguradora si se usará el campo - // SIMULACIÓN DE LÓGICA PARA BÚSQUEDA DE VEHÍCULO - Si es la primera opción, el rut del propietario es el mismo que el del comprador de la póliza

        // Se hizo correctamente la solicitud, se guarda la cotización con estado de "Cotizando", ya sea,
        // actualizando el registro si se encuentra el cotizador con el id que se inicio o creando un nuevo registro
        // con este estado, pero verificando que no se haya creado antes.
        List<QuoterModel> quoters = userDB.getQuoters();
        LocalDateTime currentDateTime = LocalDateTime.now();
        String currentStatus = "Cotizando";
        boolean isFound = false;
        String returnQuoterId = "";

        if(!quoterId.equals("No informado")) {
            for(QuoterModel quoterDB : quoters) {
                String quoterDBId = quoterDB.getQuoterId();
                if(quoterDBId.equals(quoterId)) {
                    if(!quoterDB.getQuoterStatus().equals("Iniciando") && !quoterDB.getQuoterStatus().equals(currentStatus)) {
                        return ResponseHelper.locked("El cotización ya ha pasado por este proceso", null);
                    }
                    returnQuoterId = quoterDBId;
                    // Cotización encontrada por id, verificamos si el registro ya se actualizó por el estado del cotizador.
                    // Hacemos la verificación por medio del estado del cotizador, ya que, es un endpoint que se consumirá
                    // tantas veces dependiendo de las aseguradoras.
                    if(quoterDB.getQuoterStatus().equals("Iniciando")) {
                        // Se actualiza la data del vehículo del cotizador
                        QuoterCarModel quoterCarDB = quoterDB.getQuoterCarData();
                        quoterCarDB.setPpu(ppu);
                        quoterCarDB.setBrand(brand);
                        quoterCarDB.setModel(model);
                        quoterCarDB.setYear(year);

                        // Se actualiza la data del comprador de la cotización
                        QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                        quoterPurchaserDB.setPersonalId(purchaserId);
                        quoterPurchaserDB.setName(purchaserName);
                        quoterPurchaserDB.setPaternalSurname(purchaserPaternalSur);
                        quoterPurchaserDB.setMaternalSurname(purchaserMaternalSur);
                        quoterPurchaserDB.setEmail(purchaserEmail);
                        quoterPurchaserDB.setPhone(purchaserPhone);
                        quoterPurchaserDB.setOwnerRelationOption(ownerRelationOption);

                        // Finalmente se actualiza el estado a Cotizando, el campo de actualización de registo y se
                        // actualiza en la base de datos
                        quoterDB.setQuoterStatus(currentStatus);
                        quoterDB.setUpdatedDate(currentDateTime);
                        userDB = userRepository.save(userDB);
                    }

                    isFound = true;
                    break;
                }
            }
        }

        // Si no se encontro registro por el id (puede que sea manual), por lo tanto, se crea un registro de cotizador
        // con los datos de cotización, siempre y cuando no exista antes.
        if(!isFound) {
            boolean exists = false;

            for(QuoterModel quoterDB : quoters) {
                QuoterCarModel quoterCarDB = quoterDB.getQuoterCarData();
                QuoterPurchaserModel quoterPurchaserDB = quoterDB.getQuoterPurchaserData();
                if(quoterDB.getQuoterStatus().equals(currentStatus) && quoterCarDB.getPpu().equals(ppu) && 
                        quoterCarDB.getBrand().equals(brand) && quoterCarDB.getModel().equals(model) &&
                        quoterCarDB.getYear().equals(year) && quoterPurchaserDB.getPersonalId().equals(purchaserId) &&
                        quoterPurchaserDB.getName().equals(purchaserName) && quoterPurchaserDB.getPaternalSurname().equals(purchaserPaternalSur) &&
                        quoterPurchaserDB.getMaternalSurname().equals(purchaserMaternalSur) && quoterPurchaserDB.getEmail().equals(purchaserEmail) &&
                        quoterPurchaserDB.getOwnerRelationOption().equals(ownerRelationOption)) {
                    returnQuoterId = quoterDB.getQuoterId();
                    exists = true;
                    break;
                }
            }

            // Si no existe creamos el registro
            if(!exists) {
                // Se debería encontrar alguna data del dueño del vehículo
                QuoterOwnerModel quoterOwner = new QuoterOwnerModel("", "", "", "");
                QuoterCarModel quoterCar = new QuoterCarModel(ppu, brand, model, year, "", "", "", "");
                QuoterPurchaserModel quoterPurchaser = new QuoterPurchaserModel(purchaserId, purchaserName,
                        purchaserPaternalSur, purchaserMaternalSur, purchaserEmail, purchaserPhone, ownerRelationOption);
                QuoterModel novaQuoter = quoterHelper.createQuoteStructure(quoterOwner, quoterCar, quoterPurchaser, currentStatus, currentDateTime);
                returnQuoterId = novaQuoter.getQuoterId();
                // Guardamos el nuevo cotizante en la bd, ya que, no se pudo hallar y puede ser por que la cotización se
                // hizo de manera manual.
                userDB.addQuoter(novaQuoter);
                userDB = userRepository.save(userDB);
            }
        }

        // Ahora entregaremos los planes, dependiendo de la aseguradora, enviando los datos del vehículo verificado.
        List<TestPlanDto> planList = new ArrayList<>();
        InsurerModel returnInsurerDB = new InsurerModel("", "", "", "", "");
        returnInsurerDB.setInsurerId(new ObjectId()); // Para que tenga un dato por defecto en el id

        Optional<InsurerModel> insurerOptional = insurerRepository.findByAlias(insurerAlias);
        String errorPlanFinder = "1"; // Error no se encontró una aseguradora para la búsqueda de planes
        String errorMessage = "No se encontro la aseguradora con el alias '" + insurerAlias + "'";
        String requestBody = "";
        String responseStr = "";
        if(insurerOptional.isPresent()) {
            // Obtener lista de vehículos con/sin delay, dependiendo de la aseguradora
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
                        Thread.sleep(7000);
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
                        Thread.sleep(4000);
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
                    if(errorPlanFinder.equals("0")) { // No existe error, se encontró el id de marca y modelo de la aseguradora, y ahora se realiza la solicitud
                        Map<String, Object> dataFromInsurer4 = apiBciProvider.getPlansFromBCI(purchaserId, brandAndModelId[2], brandAndModelId[3], Integer.parseInt(year), logRepository, currentDateTime);
                        errorPlanFinder = (String) dataFromInsurer4.get("errorPlanFinder");
                        errorMessage = (String) dataFromInsurer4.get("errorMessage");
                        requestBody = (String) dataFromInsurer4.get("requestBody");
                        responseStr = (String) dataFromInsurer4.get("responseStr");
                        if(errorPlanFinder.equals("0")) {
                            planList = (List<TestPlanDto>) dataFromInsurer4.get("plans");
                        }
                    }
                    
                }
            }
        }

        // ACTUALIZAR ESTRUCTURA DEL PLAN, PARA SER GUARDADO EN LA BASE DE DATOS
        // Buscar planes en la base de datos => anteriormente en la captura de los planes dependiendo de la aseguradora
        // estos deben de ser procesados y validados, por lo cual, aquí solo recojemos los datos
        for(TestPlanDto insurerPlan : planList) {
            // Buscamos si existe o no un registro de plan, mediante Id del plan
            String planId = insurerPlan.getPlanId();
            Optional<PlanModel> optionalPlan = planRepository.findById(planId);
            // Si no se encuentra, se crea el plan
            if(optionalPlan.isEmpty()) {
                // Guardamos en la base de datos
                PlanModel novaPlan = new PlanModel(planId, insurerPlan.getInsurer(), insurerPlan.getPlanName(),
                        insurerPlan.getDeductible(), insurerPlan.getStolenVehicle(), insurerPlan.getTotalLoss(),
                        insurerPlan.getDamageThirdParty(), insurerPlan.getWorkshopType(), insurerPlan.getDetails(),
                        currentDateTime, currentDateTime);
                planRepository.save(novaPlan);
            }
        }

        ResultQuoteDto resultQuote = new ResultQuoteDto(returnQuoterId, errorPlanFinder, errorMessage, requestBody, responseStr, returnInsurerDB, planList);
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
        if(DataHelper.isNull(key) || !key.equals(quoterEndpointKeyword)) {
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
        if(DataHelper.isNull(key) || !key.equals(quoterEndpointKeyword) || payments == null) {
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
