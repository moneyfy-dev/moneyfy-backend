package com.referidos.app.segurosref.services;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.dtos.ReferredDto;
import com.referidos.app.segurosref.dtos.UserCommissionDto;
import com.referidos.app.segurosref.dtos.UserSimpleDto;
import com.referidos.app.segurosref.dtos.commission.CommissionDataDto;
import com.referidos.app.segurosref.dtos.earnings.MonthlyDataDto;
import com.referidos.app.segurosref.dtos.earnings.MonthlyEarningDto;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.NotificationDataModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.PaymentRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.repositories.WhiteListRepository;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.SeedDefaultRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.seeder.RunUserSeeder;
import com.referidos.app.segurosref.validators.UserValidator;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WhiteListRepository whiteListRepository;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private UserHelper userHelper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value(value = "${user.endpoint.keyword}")
    private String userEndpointKeyword;

    // SERVICIOS PARA FLUJOS RELACIONADOS AL USUARIO
    @Transactional
    @Override
    public ResponseEntity<?> update(UserUpdateRequest user, String emailAuth) {
        try {
            UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
            UserDataModel userData = userDB.getPersonalData();

            // Actualizamos los datos del usuario
            userData.setName(user.name().trim()); // CON TRIM() INCLUIDO (No permite saltos en línea)
            userData.setSurname(user.surname().trim()); // CON TRIM() INCLUIDO (No permite saltos en línea)

            String phone = DataHelper.isNull(user.phone()) ? "" : user.phone();
            String address = DataHelper.isNull(user.address()) ? "" : user.address().trim(); // CON TRIM() INCLUIDO (permite saltos de línea) - dato opcional
            LocalDate dateOfBirth = DataHelper.isNull(user.dateOfBirth()) ? DataHelper.deprecatedDate() : LocalDate.parse(user.dateOfBirth());
            byte[] profilePicture = (user.profilePicture() == null) ? new byte[0] : user.profilePicture().getBytes() ;

            // Campos opcionales
            userData.setPhone(phone);
            userData.setAddress(address);
            userData.setDateOfBirth(dateOfBirth);
            userData.setProfilePicture(profilePicture);
            
            // Actualizamos el usuario, ya que sus objetos derivados se obtienen por referencia
            userDB = userRepository.save(userDB);
            return ResponseHelper.ok("el usuario ha sido actualizado exitosamente", DataHelper.buildUser(userDB));
        } catch (DateTimeParseException e) {   
            return ResponseHelper.failedDependency("no se pudo procesar la fecha de cumpleaños del usuario", e.getMessage());
        }catch (IOException e) {
            return ResponseHelper.failedDependency("la foto de perfil no pudo ser procesada", e.getMessage());
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> changePassword(ChangePwdRequest changePwd, String emailAuth) {
        // Verificamos primero si es un usuario de prueba
        if(RunUserSeeder.isTestUser(emailAuth)) {
            return ResponseHelper.failedDependency("el usuario de prueba, no puede cambiar su contraseña", null);
        }
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        if(passwordEncoder.matches(changePwd.oldPwd(), userData.getPwd())) {
            userData.setPwd(passwordEncoder.encode(changePwd.newPwd()));
            userDB = userRepository.save(userDB);
            return ResponseHelper.ok("la contraseña del usuario ha sido cambiada exitosamente", DataHelper.buildUser(userDB));
        } else {
            return ResponseHelper.locked("la contraseña del usuario antigua del usuario no coincide", null);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> hydrationData(String emailAuth, String updateCredential, String device) {
        // Enpoint utilizado para refrescar la data de la aplicación, por lo tanto, un buen lugar para actualizar
        // el refresh token, en caso de ser necesario
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Recuperación de notificaciones
        NotificationModel notifPreference = userDB.getNotifPreference();
        List<NotificationDataModel> notifs = new ArrayList<>();
        boolean updateNotifs = false;
        for(NotificationDataModel notifDB : notifPreference.getData()) {
            if(notifDB.getStatus().equals("Sin notificar")) {
                notifDB.setStatus("Notificado");
                updateNotifs = true; // Se tiene que actualizar las notificaciones, porque se cambia el estado
                // Buscamos el tipo de notificación y notificar si es el caso.
                switch (notifDB.getType()) {
                    case "Usuario referido":
                        if(notifPreference.isReferredRegistered()) {
                            notifs.add(notifDB); // Se agrega la notificación, ya que, el usuario tiene activado las notifcaciones para saber cuando se refiere un nuevo usuario con su código
                        }
                        break;
                    default:
                        notifs.add(notifDB); // Casos no manejados, se agregan a los notificaciones del usuario mientras.
                        break;
                }
                
            }
        }
        if(updateNotifs) {
            userRepository.save(userDB);
        }
        // Revisar si se tiene que actualizar el refresh token
        if(updateCredential.equals("Dated")) {
            UserDataModel userData = userDB.getPersonalData();
            Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(emailAuth, device);
            if(deviceOptional.isPresent()) {
                DeviceModel deviceDB = deviceOptional.get();
                userData.setRefreshToken(JwtConfig.createRefreshToken(emailAuth));
                deviceDB.setRefreshToken(userData.getRefreshToken());
                userDB = userRepository.save(userDB);
                deviceRepository.save(deviceDB);
            }
        }
        return ResponseHelper.ok("la información de hidratación del usuario fue recuperada correctamente", DataHelper.buildUser(userDB, "notifs", notifs));
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> listReferreds(String emailAuth, String updateCredential, String device) {
        // Obtenemos los referidos del usuario que está haciendo la solicitud
        UserModel userA = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<ReferredDto> referredsDto = new ArrayList<>(); // Lista de todos los referidos que se van a mostrar.
        List<ReferredModel> referredsB = referredRepository.findAllByUserReferring(emailAuth);
        for(ReferredModel referredB : referredsB) {
            // Recuperamos la data de los referidos del usuario A (o sea los usuarios B), del cual necesitamos conocer su
            // nombre, apellido, estado actual, además de saber cuantos referidos tiene el usuario B (o sea los usuarios C)
            // y cuanto es el monto de ganancia que se lleva el usuario A, entre sus referidos (usuarios B) y los referidos
            // del usuario B (usuarios C)
            String userEmailB = referredB.getReferred();
            UserModel userB = userRepository.findByPersonalData_Email(userEmailB).orElseThrow();
            String userBId = userB.getUserId();
            UserDataModel userDataB = userB.getPersonalData();
            // Si el usuario aún no confirma su registro, no se agrega como referido
            if(userDataB.getRefreshToken().equals("") || userDataB.getSessionToken().equals("")) {
               continue; 
            }
            String statusUserB = userDataB.getStatus();
            // Una vez encontrado el usuario B, se obtiene su información, y dependiendo del estado se omite info
            String nameUserB = userDataB.getName();
            String surnameUserB = userDataB.getSurname();
            String showStatusUserB;
            switch(statusUserB) {
                case "Activado" -> {
                    showStatusUserB = "Activo";
                    break;
                }
                case "Desactivado" -> {
                    showStatusUserB = "Pausado";
                    break;
                }
                default -> {
                    nameUserB = "Sin especificar";
                    surnameUserB = "Sin especificar";
                    showStatusUserB = "Eliminado";
                    break;
                }
            }
            // Ya que se estableció la data principal, ahora calcularemos las ganacias totales
            long earnings=0; // Ganancias totales del usuario B y sus referidos.
            long totalTransactionsB = transactionRepository.countByUserIdAndCommissionScopeGTEAndStatusPassed(userBId, 2);
            earnings += totalTransactionsB * 10000;
            // Obtener ganancias totales de los referidos del usuario B.
            List<ReferredModel> referredsC = referredRepository.findAllByUserReferring(userEmailB);
            for(ReferredModel referredC : referredsC) {
                // Se obtienen las transacciones que se realizaron, independiente del estado del usuario C
                String userEmailC = referredC.getReferred();
                String userCId = userRepository.findByPersonalData_Email(userEmailC).orElseThrow().getUserId();
                long totalTransactionsC = transactionRepository.countByUserIdAndCommissionScopeGTEAndStatusPassed(userCId, 3);
                earnings += totalTransactionsC * 5000;
            }
            referredsDto.add(new ReferredDto(userEmailB, nameUserB, surnameUserB, showStatusUserB, referredsC.size(), earnings));
        }
        // Revisamos si se tiene que actualizar el refresh token
        if(updateCredential.equals("Dated")) {
            UserDataModel userAData = userA.getPersonalData();
            Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(emailAuth, device);
            if(deviceOptional.isPresent()) {
                DeviceModel deviceUserA = deviceOptional.get();
                userAData.setRefreshToken(JwtConfig.createRefreshToken(emailAuth));
                deviceUserA.setRefreshToken(userAData.getRefreshToken());
                userA = userRepository.save(userA);
                deviceRepository.save(deviceUserA);
            }
        }
        // Finalmente retornamos la lista de los referidos del usuario y los detalles de estos, además del los datos
        // propios del usuario que hace la solicitud.
        return ResponseHelper.ok("se han recuperado los referidos", DataHelper.buildUser(userA, "referreds", referredsDto));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponses> obtainCommissions(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        UserDataModel userDataDB = userDB.getPersonalData();
        String userId = userDB.getUserId();
        DateTimeFormatter formatStr = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<UserCommissionDto> userCommissions = new ArrayList<>();
        List<TransactionModel> transactionsDB = transactionRepository.findAll();
        // Buscamos por las comisiones de las transacciones, donde el id del usuario de la comisión, sea igual al id del
        // usuario que está realizando la consulta
        for(TransactionModel transactionDB : transactionsDB) {
            String transactionId = transactionDB.getTransactionId();
            String transactionUserId = transactionDB.getUserId();
            String createdDate = transactionDB.getCreatedDate().format(formatStr);
            String observation = transactionDB.getObservation();
            for(TransactionComissionModel commission : transactionDB.getCommissions()) {
                if(userId.equals(commission.getUserId())) {
                    // Comisión del usuario
                    String seller;
                    String status = commission.getCommissionStatus();
                    int userCommission = commission.getUserCommission();
                    // Buscamos el vendedor del plan
                    if(userId.equals(transactionUserId)) {
                        seller = userDataDB.getName() + " " + userDataDB.getSurname();
                    } else {
                        try {
                            UserModel userSeller = userRepository.findById(new ObjectId(transactionUserId)).orElseThrow();
                            UserDataModel userDataSeller = userSeller.getPersonalData();
                            seller = userDataSeller.getName() + " " + userDataSeller.getSurname();
                        } catch (Exception e) {
                            seller = "Sin especificar";
                        }
                    }
                    // Revisamos si la comisión se encuentra en el estado "Confirmando"
                    if(status.equals("Confirmando")) {
                        observation = "Debe crear o seleccionar una cuenta bancaria predeterminada, para recibir su comisión";
                    }
                    userCommissions.add(new UserCommissionDto(transactionId, seller, status, userCommission, createdDate, observation));
                }
            }
        }
        return ResponseHelper.ok("comisiones recuperadas exitosamente", DataHelper.buildUser(userDB, "userCommissions", userCommissions));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponses> obtainPayments(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userId = userDB.getUserId();
        List<PaymentModel> userPayments = paymentRepository.findAllByUserId(userId);
        return ResponseHelper.ok("los pagos del usuario se han recuperado correctamente", DataHelper.buildUser(userDB, "userPayments", userPayments));
    }

    // SERVICIO PARA OBTENER LAS GANANCIAS DEL USUARIO EN LOS ÚLTIMOS 5 MESES
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponses> monthlyEarnings(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userId = userDB.getUserId();
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // Obtención de fecha del último mes, de los últimos 5 meses
        LocalDateTime lastMonth = currentDate.minusMonths(4)
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        // Buscamos todas las transacciones con algún estado aceptado y que la fecha en la que fue aprobada la
        // transacción, haya sea igual o superior a la fecha previamente obtenida.
        List<TransactionModel> transactionsDB = transactionRepository.findAllByApprovalDateAfterAndStatusAccepted(lastMonth);
        MonthlyEarningDto monthlyEarningDto = new MonthlyEarningDto(this.addMonthsToMonthlyEarnings(lastMonth, formatterDate),
                0, 0, lastMonth.format(formatterDate));
        int finalCommissions = 0;
        int finalAmount = 0;
        for(TransactionModel transactionDB : transactionsDB) {
            String transacionId = transactionDB.getTransactionId();
            String approvalDate = transactionDB.getApprovalDate().format(formatterDate);
            for(TransactionComissionModel commissionDB : transactionDB.getCommissions()) {
                String transactionUserId = commissionDB.getUserId();
                int transactionCommission = commissionDB.getUserCommission();
                if(userId.equals(transactionUserId)) {
                    if(!this.addCommissionToMonthlyEarnings(monthlyEarningDto, transacionId, approvalDate.substring(0, 7), transactionCommission)) {
                        return ResponseHelper.locked("no se pudo encontrar el mes, al que corresponde la comisión", null);
                    }
                    finalCommissions += 1;
                    finalAmount += transactionCommission;
                    break;
                }
            }
        }
        // Se vuelven a establecer los valores finales
        monthlyEarningDto.setFinalCommissions(finalCommissions);
        monthlyEarningDto.setFinalAmount(finalAmount);
        return ResponseHelper.ok("se han recuperado las comisiones aceptadas de los últimos 5 meses del usuario",
                DataHelper.buildUser(userDB, "monthlyEarnings", monthlyEarningDto));
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> seedDefault(SeedDefaultRequest seedDefault) {
        // Verificamos que la llave sea la correcta
        String key = seedDefault.key();
        if(key == null || !key.equals(userEndpointKeyword)) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
        }
        // Registramos primero los usuarios de prueba
        String message = userHelper.seedTestUsers(userRepository, referredRepository, deviceRepository, whiteListRepository, passwordEncoder);
        if(message == null) {
            return ResponseHelper.failedDependency("los usuarios de pruebas son incorrectos", null);
        }
        return ResponseHelper.ok(message, Map.of("info", "ok"));
    }

    // SERVICIOS HELPERS PARA OBTENER LAS GANANCIAS DE LOS ÚLTIMOS 5 MESES DEL USUARIO
    private List<MonthlyDataDto> addMonthsToMonthlyEarnings(LocalDateTime lastMonth, DateTimeFormatter formatterDate) {
        // Se toman los últimos cinco meses de las comisiones, y agregamos las fechas teniendo como referencia el último mes
        List<MonthlyDataDto> list = new ArrayList<>();

        String monthStr1 = lastMonth.toLocalDate().format(formatterDate);
        MonthlyDataDto month1 = new MonthlyDataDto(monthStr1, 0, 0, new ArrayList<>());
        
        String monthStr2 = lastMonth.plusMonths(1).toLocalDate().format(formatterDate);
        MonthlyDataDto month2 = new MonthlyDataDto(monthStr2, 0, 0, new ArrayList<>());
        
        String monthStr3 = lastMonth.plusMonths(2).toLocalDate().format(formatterDate);
        MonthlyDataDto month3 = new MonthlyDataDto(monthStr3, 0, 0, new ArrayList<>());

        String monthStr4 = lastMonth.plusMonths(3).toLocalDate().format(formatterDate);
        MonthlyDataDto month4 = new MonthlyDataDto(monthStr4, 0, 0, new ArrayList<>());

        String monthStr5 = lastMonth.plusMonths(4).toLocalDate().format(formatterDate);
        MonthlyDataDto month5 = new MonthlyDataDto(monthStr5, 0, 0, new ArrayList<>());

        list.add(month1);
        list.add(month2);
        list.add(month3);
        list.add(month4);
        list.add(month5);

        return list;
    }

    private boolean addCommissionToMonthlyEarnings(MonthlyEarningDto monthlyEarningDto, String transacionId,
            String approvalDateYearMonth, int transactionCommission) {
        for(MonthlyDataDto monthDto : monthlyEarningDto.getMonths()) {
            String yearMonth = monthDto.getMonth().substring(0, 7);
            if(approvalDateYearMonth.equals(yearMonth)) {
                int totalCommissions = monthDto.getTotalCommission() + 1;
                int totalAmount = monthDto.getTotalAmount() + transactionCommission;
                monthDto.setTotalCommission(totalCommissions);
                monthDto.setTotalAmount(totalAmount);
                monthDto.addCommission(new CommissionDataDto(transacionId, transactionCommission));
                return true;
            }
        }
        return false;
    }

    // SERVICIOS DE VALIDACIONES DE DATOS
    @Override
    public void validateRegister(UserRegisterRequest user, Errors errors) {
        userValidator.validateRegister(user, errors);
    }

    @Override
    public void validateUpdate(UserUpdateRequest user, BindingHelper bindingHelper) {
        userValidator.validateUpdate(user, bindingHelper);
    }

    @Override
    public void validateSave(UserRegisterRequest user, Errors errors) {
        userValidator.validate(user, errors);
    }

    @Override
    public void validatePasswordChanged(ChangePwdRequest changePwd, Errors errors) {
        userValidator.validatePasswordChanged(changePwd, errors);
    }

    // SERVICIOS SUPUESTOS PARA ADMINISTRADORES QUE NO SE ESTÁN UTILIZANDO AÚN
    @Transactional(readOnly=true)
    @Override
    public List<UserSimpleDto> findAll() {
        List<UserSimpleDto> users = new ArrayList<>();
        
        userRepository.findAll().forEach(userDB -> {
            users.add(DataHelper.buildSimpleUser(userDB));
        });
        
        return users;
    }
    
    @Transactional(readOnly=true)
    @Override
    public ResponseEntity<?> findById(ObjectId userId) {
        UserModel userDB = userRepository.findById(userId).orElseThrow();
        return ResponseHelper.ok(
                "el usuario ha sido encontrado exitosamente",
                Map.of("user", DataHelper.buildSimpleUser(userDB)));
    }

}
