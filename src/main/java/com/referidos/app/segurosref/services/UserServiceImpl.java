package com.referidos.app.segurosref.services;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import com.referidos.app.segurosref.dtos.ReferredDto;
import com.referidos.app.segurosref.dtos.UserCommissionDto;
import com.referidos.app.segurosref.dtos.commission.CommissionDataDto;
import com.referidos.app.segurosref.dtos.earnings.MonthlyDataDto;
import com.referidos.app.segurosref.dtos.earnings.MonthlyEarningDto;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.DeviceModel;
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
import com.referidos.app.segurosref.requests.ChangePwdRequest;
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
    private UserValidator userValidator;

    @Autowired
    private UserHelper userHelper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // SERVICIOS PARA FLUJOS RELACIONADOS AL USUARIO
    @Transactional
    @Override
    public ResponseEntity<?> update(UserUpdateRequest user, String emailAuth) {
        try {
            UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
            UserDataModel userData = userDB.getPersonalData();

            // Actualizamos los datos del usuario
            userData.setName(user.name().strip()); // Usamos strip() para quitar espacios al inicio y final
            userData.setSurname(user.surname().strip()); // Usamos strip() para quitar espacios al inicio y final

            String phone = DataHelper.isNull(user.phone()) ? "" : user.phone();
            String address = DataHelper.isNull(user.address()) ? "" : user.address().strip(); // Usamos strip() para quitar espacios al inicio y final
            LocalDate dateOfBirth = DataHelper.isNull(user.dateOfBirth()) ? DataHelper.deprecatedDate() : LocalDate.parse(user.dateOfBirth());
            byte[] profilePicture = (user.profilePicture() == null) ? new byte[0] : user.profilePicture().getBytes();

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
            return ResponseHelper.locked("la contraseña antigua del usuario no coincide", null);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> hydrationData(String emailAuth, String updateCredential, String device) {
        // Endpoint utilizado para refrescar la data de la aplicación, por lo tanto, un buen lugar para
        // actualizar el refresh token, en caso de ser necesario.
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        // Revisar si se tiene que actualizar el refresh token
        if(updateCredential.equals("Dated")) {
            Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(emailAuth, device);
            if(deviceOptional.isPresent()) {
                userHelper.updateRefreshToken(userRepository, userDB, deviceOptional.get(), deviceRepository);
            }
        }
        return ResponseHelper.ok("la información de hidratación del usuario fue recuperada correctamente", DataHelper.buildUser(userDB));
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> listReferreds(String emailAuth, String updateCredential, String device) {
        UserModel userA = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<ReferredDto> referredsDto = new ArrayList<>(); // Lista de todos los referidos que se van a mostrar.
        List<ReferredModel> referredsB = referredRepository.findAllByUserReferring(emailAuth);
        for(ReferredModel referredB : referredsB) {
            // Recuperamos los referidos del usuario que está haciendo la solicitud (que serían los usuariosB),
            // del cual necesitamos conocer su nombre, apellido, estado actual, y recuperar los referidos del
            // usuario B (que serían los usuariosC), para calcular la ganacia total del usuario que está haciendo
            // la solicitud, contabilizando los usuariosB y los usuariosC
            String userEmailB = referredB.getReferred();
            UserModel userB = userRepository.findByPersonalData_Email(userEmailB).orElseThrow();
            UserDataModel userDataB = userB.getPersonalData();
            // Si el usuario aún no confirma su registro, no se agrega como referido
            if(userDataB.getRefreshToken().equals("") || userDataB.getSessionToken().equals("")) {
               continue; 
            }
            // Luego de checkear que el registro del usuario se completo, se actualiza info dependiendo del estado
            String userBId = userB.getUserId();
            String statusUserB = userDataB.getStatus();
            String nameUserB = userDataB.getName();
            String surnameUserB = userDataB.getSurname();
            boolean isUserBDeleted = false;
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
                    isUserBDeleted = true;
                    break;
                }
            }
            // Ya que se estableció la data principal, ahora calcularemos las ganacias totales
            long earnings = 0; // Ganancias totales del usuario B y sus referidos.
            long totalTransactionsB = transactionRepository.countByUserIdAndCommissionScopeGTEAndStatusPassed(userBId, 2);
            long userBEarnings = totalTransactionsB * 10000;
            long userCEarnings = 0;
            boolean allUsersCDeleted = true;
            List<ReferredModel> referredsC = referredRepository.findAllByUserReferring(userEmailB);
            for(ReferredModel referredC : referredsC) {
                String userEmailC = referredC.getReferred();
                UserModel userC = userRepository.findByPersonalData_Email(userEmailC).orElseThrow();
                String userCId = userC.getUserId();
                String userCStatus = userC.getPersonalData().getStatus();
                long totalTransactionsC = transactionRepository.countByUserIdAndCommissionScopeGTEAndStatusPassed(userCId, 3);
                // Ajustar variables iterativas
                allUsersCDeleted = (!userCStatus.equals("Activado") && !userCStatus.equals("Desactivado")) ? allUsersCDeleted : false;
                userCEarnings += totalTransactionsC * 5000;
            }
            // Si es un referido eliminado que no está aportando al saldo, no lo agregamos
            if(isUserBDeleted && userBEarnings <= 0 && allUsersCDeleted && userCEarnings <= 0) {
                continue;
            } 
            earnings += userBEarnings + userCEarnings;
            referredsDto.add(new ReferredDto(userEmailB, nameUserB, surnameUserB, showStatusUserB, referredsC.size(), earnings));
        }
        // Revisamos si se tiene que actualizar el refresh token
        if(updateCredential.equals("Dated")) {
            Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(emailAuth, device);
            if(deviceOptional.isPresent()) {
                userHelper.updateRefreshToken(userRepository, userA, deviceOptional.get(), deviceRepository);
            }
        }
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
            for(TransactionComissionModel commissionData : transactionDB.getCommissions()) {
                if(userId.equals(commissionData.getUserId())) {
                    // Comisión del usuario
                    String seller;
                    String status = commissionData.getCommissionStatus();
                    int userCommission = commissionData.getUserCommission();
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
                    break;
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
        // Obtención de fecha, del último mes, de los últimos 5 meses
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
                    finalCommissions += transactionCommission;
                    finalAmount += 1;
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

    // SERVICIOS HELPERS PARA OBTENER LAS GANANCIAS DE LOS ÚLTIMOS 5 MESES DEL USUARIO
    // Crea la estructura de la data de cada mes
    private List<MonthlyDataDto> addMonthsToMonthlyEarnings(LocalDateTime lastMonth, DateTimeFormatter formatterDate) {
        // Se toman los últimos cinco meses de las comisiones, y agregamos las fechas teniendo como
        // referencia el cálculo del último mes.
        List<MonthlyDataDto> list = new ArrayList<>();
        // Agregamos los meses
        String monthStr1 = lastMonth.plusMonths(4).toLocalDate().format(formatterDate);
        MonthlyDataDto month1 = new MonthlyDataDto(monthStr1, 0, 0, new ArrayList<>());
        String monthStr2 = lastMonth.plusMonths(3).toLocalDate().format(formatterDate);
        MonthlyDataDto month2 = new MonthlyDataDto(monthStr2, 0, 0, new ArrayList<>());
        String monthStr3 = lastMonth.plusMonths(2).toLocalDate().format(formatterDate);
        MonthlyDataDto month3 = new MonthlyDataDto(monthStr3, 0, 0, new ArrayList<>());
        String monthStr4 = lastMonth.plusMonths(1).toLocalDate().format(formatterDate);
        MonthlyDataDto month4 = new MonthlyDataDto(monthStr4, 0, 0, new ArrayList<>());
        String monthStr5 = lastMonth.toLocalDate().format(formatterDate);
        MonthlyDataDto month5 = new MonthlyDataDto(monthStr5, 0, 0, new ArrayList<>());
        // Se agrega a la lista la estructura de la ganancia de los últimos 5 meses
        list.add(month1);
        list.add(month2);
        list.add(month3);
        list.add(month4);
        list.add(month5);
        return list;
    }
    // Asigna una comisión al mes correspondiente
    private boolean addCommissionToMonthlyEarnings(MonthlyEarningDto monthlyEarningDto, String transacionId,
            String approvalDateYearMonth, int transactionCommission) {
        for(MonthlyDataDto monthDto : monthlyEarningDto.getMonths()) {
            String yearMonth = monthDto.getMonth().substring(0, 7);
            if(approvalDateYearMonth.equals(yearMonth)) {
                int totalCommissions = monthDto.getTotalCommission() + transactionCommission;
                int totalAmount = monthDto.getTotalAmount() + 1;
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
    // @Transactional(readOnly=true)
    // @Override
    // public List<UserSimpleDto> findAll() {
    //     List<UserSimpleDto> users = new ArrayList<>();
        
    //     userRepository.findAll().forEach(userDB -> {
    //         users.add(DataHelper.buildSimpleUser(userDB));
    //     });
        
    //     return users;
    // }
    // @SuppressWarnings("null")
    // @Transactional(readOnly=true)
    // @Override
    // public ResponseEntity<?> findById(ObjectId userId) {
    //     UserModel userDB = userRepository.findById(userId).orElseThrow();
    //     return ResponseHelper.ok(
    //             "el usuario ha sido encontrado exitosamente",
    //             Map.of("user", DataHelper.buildSimpleUser(userDB)));
    // }

}
