package com.referidos.app.segurosref.services;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import com.referidos.app.segurosref.dtos.ReferredDto;
import com.referidos.app.segurosref.dtos.UserCommissionDto;
import com.referidos.app.segurosref.dtos.earning.DailyCommissionDto;
import com.referidos.app.segurosref.dtos.earning.DailyDataDto;
import com.referidos.app.segurosref.dtos.earning.LastDaysEarningDto;
import com.referidos.app.segurosref.dtos.earning.LastMonthsEarningDto;
import com.referidos.app.segurosref.dtos.earning.MonthlyDataDto;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.repositories.PaymentRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.validators.UserValidator;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final AuthRepository authRepository;

    private final ReferredRepository referredRepository;

    private final TransactionRepository transactionRepository;

    private final PaymentRepository paymentRepository;

    private final UserValidator userValidator;

    private final PasswordEncoder passwordEncoder;

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
            String address = DataHelper.isNull(user.address()) ? "" : user.address().strip(); // Usamos strip() para
                                                                                              // quitar espacios al
                                                                                              // inicio y final
            LocalDate dateOfBirth = DataHelper.isNull(user.dateOfBirth()) ? DataHelper.deprecatedDate()
                    : LocalDate.parse(user.dateOfBirth());
            byte[] profilePicture = (user.profilePicture() == null) ? new byte[0] : user.profilePicture().getBytes();

            // Campos opcionales
            userData.setPhone(phone);
            userData.setAddress(address);
            userData.setDateOfBirth(dateOfBirth);
            userData.setProfilePicture(profilePicture);

            // Actualizamos el usuario, ya que sus objetos derivados se obtienen por
            // referencia
            userDB = userRepository.save(userDB);
            return ResponseHelper.ok("el usuario ha sido actualizado exitosamente", DataHelper.buildUser(userDB));
        } catch (DateTimeParseException e) {
            return ResponseHelper.failedDependency("no se pudo procesar la fecha de cumpleaños del usuario",
                    e.getMessage());
        } catch (IOException e) {
            return ResponseHelper.failedDependency("la foto de perfil no pudo ser procesada", e.getMessage());
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> changePassword(ChangePwdRequest changePwd, String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        AuthModel authDB = authRepository.findByEmail(emailAuth).orElseThrow();
        if (passwordEncoder.matches(changePwd.oldPwd(), authDB.getPwd())) {
            authDB.setPwd(passwordEncoder.encode(changePwd.newPwd()));
            authRepository.save(authDB);
            return ResponseHelper.ok("la contraseña del usuario ha sido cambiada exitosamente",
                    DataHelper.buildUser(userDB));
        } else {
            return ResponseHelper.locked("la contraseña antigua del usuario no coincide", null);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> hydrationData(String emailAuth) {
        // Endpoint utilizado para refrescar la data de la aplicación.
        // La actualización de credenciales ahora se maneja vía Sliding Session en los
        // headers.
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        return ResponseHelper.ok("la información de hidratación del usuario fue recuperada correctamente",
                DataHelper.buildUser(userDB));
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponse> listReferreds(String emailAuth) {
        UserModel userA = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<ReferredDto> referredsDto = new ArrayList<>(); // Lista de todos los referidos que se van a mostrar.
        List<ReferredModel> referredsB = referredRepository.findAllByUserReferring(emailAuth);
        for (ReferredModel referredB : referredsB) {
            // Recuperamos los referidos del usuario que está haciendo la solicitud (que
            // serían los usuariosB),
            // del cual necesitamos conocer su nombre, apellido, estado actual, y recuperar
            // los referidos del
            // usuario B (que serían los usuariosC), para calcular la ganacia total del
            // usuario que está haciendo
            // la solicitud, contabilizando los usuariosB y los usuariosC
            String userEmailB = referredB.getReferred();
            Optional<UserModel> userBOptional = userRepository.findByPersonalData_Email(userEmailB);
            if (userBOptional.isEmpty()) {
                continue;
            }
            UserModel userB = userBOptional.get();
            UserDataModel userDataB = userB.getPersonalData();
            // Si el usuario aún no confirma su registro, no se agrega como referido
            Optional<AuthModel> authOptionalB = authRepository.findByEmail(userEmailB);
            if (authOptionalB.isEmpty() || !authOptionalB.get().isAccountConfirmed()) {
                continue;
            }
            // Luego de checkear que el registro del usuario se completo, se actualiza info
            // dependiendo del estado
            String userBId = userB.getUserId();
            String statusUserB = userDataB.getStatus();
            String nameUserB = userDataB.getName();
            String surnameUserB = userDataB.getSurname();
            boolean isUserBDeleted = false;
            String showStatusUserB;
            switch (statusUserB) {
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
            // Ya que se estableció la data principal, ahora calcularemos las ganacias
            // totales
            long earnings = 0; // Ganancias totales del usuario B y sus referidos.
            long totalTransactionsB = transactionRepository.countByUserIdAndCommissionScopeGTEAndStatusPassed(userBId,
                    2);
            long userBEarnings = totalTransactionsB * 10000;
            long userCEarnings = 0;
            boolean allUsersCDeleted = true;
            List<ReferredModel> referredsC = referredRepository.findAllByUserReferring(userEmailB);
            for (ReferredModel referredC : referredsC) {
                String userEmailC = referredC.getReferred();
                Optional<UserModel> userCOptional = userRepository.findByPersonalData_Email(userEmailC);
                if (userCOptional.isEmpty()) {
                    continue;
                }
                UserModel userC = userCOptional.get();
                String userCId = userC.getUserId();
                String userCStatus = userC.getPersonalData().getStatus();
                long totalTransactionsC = transactionRepository
                        .countByUserIdAndCommissionScopeGTEAndStatusPassed(userCId, 3);
                // Ajustar variables iterativas
                allUsersCDeleted = (!userCStatus.equals("Activado") && !userCStatus.equals("Desactivado"))
                        ? allUsersCDeleted
                        : false;
                userCEarnings += totalTransactionsC * 5000;
            }
            // Si es un referido eliminado que no está aportando al saldo, no lo agregamos
            if (isUserBDeleted && userBEarnings <= 0 && allUsersCDeleted && userCEarnings <= 0) {
                continue;
            }
            earnings += userBEarnings + userCEarnings;
            referredsDto.add(new ReferredDto((showStatusUserB.equals("Eliminado")) ? "No encontrado" : userEmailB,
                    nameUserB, surnameUserB, showStatusUserB, referredsC.size(), earnings));
        }
        // La actualización de credenciales ahora se maneja vía Sliding Session en los
        // headers.
        return ResponseHelper.ok("se han recuperado los referidos",
                DataHelper.buildUser(userA, "referreds", referredsDto));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponse> obtainCommissions(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        UserDataModel userDataDB = userDB.getPersonalData();
        String userId = userDB.getUserId();
        DateTimeFormatter formatStr = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<UserCommissionDto> userCommissions = new ArrayList<>();
        List<TransactionModel> transactionsDB = transactionRepository
                .findAllByCommissions_UserIdAndStatusPassed(userId);
        // Buscamos por las comisiones de las transacciones, donde el id del usuario de
        // la comisión, sea igual al id del
        // usuario que está realizando la consulta
        for (TransactionModel transactionDB : transactionsDB) {
            String transactionId = transactionDB.getTransactionId();
            String transactionUserId = transactionDB.getUserId();
            String createdDate = transactionDB.getCreatedDate().format(formatStr);
            String observation = transactionDB.getObservation();
            for (TransactionComissionModel commissionData : transactionDB.getCommissions()) {
                if (userId.equals(commissionData.getUserId())) {
                    // Comisión del usuario
                    String seller;
                    String status = commissionData.getCommissionStatus();
                    int userCommission = commissionData.getUserCommission();
                    // Buscamos el vendedor del plan
                    if (userId.equals(transactionUserId)) {
                        seller = userDataDB.getName() + " " + userDataDB.getSurname();
                    } else {
                        try {
                            UserModel userSeller = userRepository.findById(new ObjectId(transactionUserId))
                                    .orElseThrow();
                            UserDataModel userDataSeller = userSeller.getPersonalData();
                            seller = userDataSeller.getName() + " " + userDataSeller.getSurname();
                        } catch (Exception e) {
                            seller = "Sin especificar";
                        }
                    }
                    userCommissions.add(new UserCommissionDto(transactionId, seller, status, userCommission,
                            createdDate, observation));
                    break;
                }
            }
        }
        return ResponseHelper.ok("comisiones recuperadas exitosamente",
                DataHelper.buildUser(userDB, "userCommissions", userCommissions));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponse> obtainPayments(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userId = userDB.getUserId();
        List<PaymentModel> userPayments = paymentRepository.findAllByUserId(userId);
        return ResponseHelper.ok("los pagos del usuario se han recuperado correctamente",
                DataHelper.buildUser(userDB, "userPayments", userPayments));
    }

    // SERVICIO PARA OBTENER LAS GANANCIAS DEL USUARIO EN LOS ÚLTIMOS 7 DÍAS
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponse> weeklyEarnings(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userId = userDB.getUserId();
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // Obtención de fecha de hace 6 días (7 días contando el actual)
        LocalDateTime lastDay = currentDate.minusDays(6)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        // Buscamos todas las transacciones con algún estado aceptado y que la fecha en
        // la que fue aprobada la transacción, haya sea igual o superior a la fecha
        // previamente obtenida.
        List<TransactionModel> transactionsDB = transactionRepository
                .findAllByApprovalDateAfterAndCommissions_UserIdAndStatusPassed(lastDay, userId);
        LastDaysEarningDto earningDto = new LastDaysEarningDto(
                this.addDaysToEarnings(lastDay, formatterDate),
                0, 0, lastDay.format(formatterDate));
        int finalCommissions = 0;
        int finalAmount = 0;
        for (TransactionModel transactionDB : transactionsDB) {
            String transacionId = transactionDB.getTransactionId();
            String approvalDate = transactionDB.getApprovalDate().format(formatterDate);
            for (TransactionComissionModel commissionDB : transactionDB.getCommissions()) {
                String transactionUserId = commissionDB.getUserId();
                int transactionCommission = commissionDB.getUserCommission();
                if (userId.equals(transactionUserId)) {
                    if (!this.addCommissionToDailyEarnings(earningDto, transacionId,
                            approvalDate, transactionCommission)) {
                        return ResponseHelper.locked("no se pudo encontrar el día, al que corresponde la comisión",
                                null);
                    }
                    finalCommissions += transactionCommission;
                    finalAmount += 1;
                    break;
                }
            }
        }
        // Se vuelven a establecer los valores finales
        earningDto.setFinalCommissions(finalCommissions);
        earningDto.setFinalAmount(finalAmount);
        return ResponseHelper.ok("se han recuperado las comisiones aceptadas de los últimos 7 días del usuario",
                DataHelper.buildUser(userDB, "weeklyEarnings", earningDto));
    }

    // SERVICIOS HELPERS PARA OBTENER LAS GANANCIAS DE LOS ÚLTIMOS 7 DÍAS DEL
    // USUARIO
    // Crea la estructura de la data de cada día
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<GeneralResponse> monthlyEarnings(String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        String userId = userDB.getUserId();
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatterMonth = DateTimeFormatter.ofPattern("yyyy-MM-01");
        LocalDateTime firstMonth = currentDate.minusMonths(4)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        List<TransactionModel> transactionsDB = transactionRepository
                .findAllByApprovalDateAfterAndCommissions_UserIdAndStatusPassed(firstMonth, userId);
        LastMonthsEarningDto earningDto = new LastMonthsEarningDto(
                this.addMonthsToEarnings(firstMonth, formatterMonth),
                0, 0, firstMonth.format(formatterMonth));

        int finalCommissions = 0;
        int finalAmount = 0;
        for (TransactionModel transactionDB : transactionsDB) {
            if (transactionDB.getApprovalDate() == null) {
                continue;
            }

            String transactionId = transactionDB.getTransactionId();
            String approvalMonth = transactionDB.getApprovalDate()
                    .withDayOfMonth(1)
                    .toLocalDate()
                    .format(formatterMonth);

            for (TransactionComissionModel commissionDB : transactionDB.getCommissions()) {
                if (!userId.equals(commissionDB.getUserId())) {
                    continue;
                }

                int transactionCommission = commissionDB.getUserCommission();
                if (!this.addCommissionToMonthlyEarnings(earningDto, transactionId,
                        approvalMonth, transactionCommission)) {
                    return ResponseHelper.locked("no se pudo encontrar el mes, al que corresponde la comisiÃ³n",
                            null);
                }
                finalCommissions += transactionCommission;
                finalAmount += 1;
                break;
            }
        }

        earningDto.setFinalCommissions(finalCommissions);
        earningDto.setFinalAmount(finalAmount);
        return ResponseHelper.ok("se han recuperado las comisiones aceptadas de los Ãºltimos 5 meses del usuario",
                DataHelper.buildUser(userDB, "monthlyEarnings", earningDto));
    }

    private List<DailyDataDto> addDaysToEarnings(LocalDateTime lastDay, DateTimeFormatter formatterDate) {
        List<DailyDataDto> list = new ArrayList<>();
        // Agregamos desde el día más reciente hasta el más antiguo o viceversa
        for (int i = 6; i >= 0; i--) {
            String dayStr = lastDay.plusDays(i).toLocalDate().format(formatterDate);
            list.add(new DailyDataDto(dayStr, 0, 0, new ArrayList<>()));
        }
        return list;
    }

    // Asigna una comisión al día correspondiente
    private boolean addCommissionToDailyEarnings(LastDaysEarningDto earningDto, String transacionId,
            String approvalDateStr, int transactionCommission) {
        for (DailyDataDto dayDto : earningDto.getDays()) {
            if (approvalDateStr.equals(dayDto.getDate())) {
                int totalCommissions = dayDto.getTotalCommission() + transactionCommission;
                int totalAmount = dayDto.getTotalAmount() + 1;
                dayDto.setTotalCommission(totalCommissions);
                dayDto.setTotalAmount(totalAmount);
                dayDto.addCommission(new DailyCommissionDto(transacionId, transactionCommission));
                return true;
            }
        }
        return false;
    }

    private List<MonthlyDataDto> addMonthsToEarnings(LocalDateTime firstMonth, DateTimeFormatter formatterMonth) {
        List<MonthlyDataDto> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String monthStr = firstMonth.plusMonths(i).toLocalDate().format(formatterMonth);
            list.add(new MonthlyDataDto(monthStr, 0, 0, new ArrayList<>()));
        }
        return list;
    }

    private boolean addCommissionToMonthlyEarnings(LastMonthsEarningDto earningDto, String transactionId,
            String approvalMonthStr, int transactionCommission) {
        for (MonthlyDataDto monthDto : earningDto.getMonths()) {
            if (approvalMonthStr.equals(monthDto.getMonth())) {
                int totalCommissions = monthDto.getTotalCommission() + transactionCommission;
                int totalAmount = monthDto.getTotalAmount() + 1;
                monthDto.setTotalCommission(totalCommissions);
                monthDto.setTotalAmount(totalAmount);
                monthDto.addCommission(new DailyCommissionDto(transactionId, transactionCommission));
                return true;
            }
        }
        return false;
    }

    // SERVICIOS DE VALIDACIONES DE DATOS
    @Override
    public void validateSimpleUser(UserRegisterRequest user, Errors errors) {
        userValidator.validate(user, errors);
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

}
