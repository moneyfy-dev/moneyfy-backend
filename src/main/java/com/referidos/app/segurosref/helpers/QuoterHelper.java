package com.referidos.app.segurosref.helpers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.referidos.app.segurosref.dtos.TestPlanDto;
import com.referidos.app.segurosref.dtos.TransactionCommissionDto;
import com.referidos.app.segurosref.dtos.TransactionDto;
import com.referidos.app.segurosref.dtos.report.ReportAccountDto;
import com.referidos.app.segurosref.dtos.report.ReportTransactionDataDto;
import com.referidos.app.segurosref.dtos.report.ReportUserDto;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.QuoterAddressModel;
import com.referidos.app.segurosref.models.QuoterCarModel;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.QuoterOwnerModel;
import com.referidos.app.segurosref.models.QuoterPaymentModel;
import com.referidos.app.segurosref.models.QuoterPlanModel;
import com.referidos.app.segurosref.models.QuoterPurchaserModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

// Se inyecta como repositorio en el servicio de "Quoter", pero, realizando funcionalidades de servicio
@Component 
public class QuoterHelper {

    @Value(value="${report.commission.cutoff-date}")
    private int commissionCutoffDate;

    @Value(value="${report.commission.payment-date}")
    private int commissionPaymentDate;

    // Funciones de apoyo con data de prueba
    public List<QuoterCarModel> vehicleList() {
        List<QuoterCarModel> list = new ArrayList<>();
        QuoterCarModel car1 = new QuoterCarModel("11AA22", "CHANGAN", "CS35", "2022", "SUV", "Plateado", "AA1234BB5678", "FAEBDC892354A1B3C6", "SAIC-GM-Wuling");
        QuoterCarModel car2 = new QuoterCarModel("AB1234", "AUDI", "S4", "2018", "SEDAN", "Blanco", "123ABC456DEF", "789GHI012JKL", "Toyota Motor Corporation");
        QuoterCarModel car3 = new QuoterCarModel("DE5678", "CHEVROLET", "AVEO", "2012", "HATCHBACK", "Azul", "456DEF789GHI", "012JKL345MNO", "BMW AG");
        QuoterCarModel car4 = new QuoterCarModel("GH9012", "CITROEN", "C-5", "2017", "SEDAN", "Gris", "789GHI012JKL", "345MNO678PQR", "Ford Motor Company");
        QuoterCarModel car5 = new QuoterCarModel("JK34DL", "TOYOTA", "RUSH", "2021", "STATION WAGON", "Blanco", "012JKL345MNO", "678PQR901STU", "Mercedes-Benz AG");
        list.add(car1);
        list.add(car2);
        list.add(car3);
        list.add(car4);
        list.add(car5);
        return list;
    }
    public QuoterCarModel buildDefaultVehicle(boolean update, String ppu, String brand, String model, String year) {
        return update ? (new QuoterCarModel(ppu, brand, model, year, "", "Negro", "N0V0T3STT4RB0", "N0V0T3STT3ST3R", "Stellantis")) : (new QuoterCarModel(ppu, "AUDI", "A3", "2021", "HATCHBACK", "Negro", "N0V0T3STT4RB0", "N0V0T3STT3ST3R", "Stellantis"));
    }

    public List<QuoterOwnerModel> ownerList() {
        List<QuoterOwnerModel> list = new ArrayList<>();
        QuoterOwnerModel owner1 = new QuoterOwnerModel("11.111.111-1", "Pepe", "Rodriguez", "Fuentes");
        QuoterOwnerModel owner2 = new QuoterOwnerModel("22.222.222-2", "Maria", "Fuentes", "Silva");
        QuoterOwnerModel owner3 = new QuoterOwnerModel("33.333.333-3", "Camila", "Avellaneda", "González");
        QuoterOwnerModel owner4 = new QuoterOwnerModel("44.444.444-4", "Octaquio", "Alfonso", "Riquelme");
        QuoterOwnerModel owner5 = new QuoterOwnerModel("55.555.555-5", "Valentina", "Carrasco", "Zamora");
        list.add(owner1);
        list.add(owner2);
        list.add(owner3);
        list.add(owner4);
        list.add(owner5);
        return list;
    }

    public List<TestPlanDto> planList1() {
        List<TestPlanDto> list = new ArrayList<>();
        double valueUF = 37000.00;
        String stolenCar = "Valor comercial";
        String workshopType = "Oficial de la marca";
        // Creamos planes de prueba
        TestPlanDto plan1 = new TestPlanDto("TRACTOR045678987", "Tractor Seguros Automotriz",
                "Plan protector de auto", valueUF, 24.86, 11, 24.86/11,
                (24.86/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan1, "70%", "800 UF", "90", "3");
        TestPlanDto plan2 = new TestPlanDto("TRACTOR123456789", "Tractor Seguros Automotriz",
                "Seguro auto completo", valueUF, 22.72, 11, 22.72/11,
                (22.72/11)*valueUF, 5, "Deducible 5 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan2, "80%", "1200 UF", "120", "4");
        TestPlanDto plan3 = new TestPlanDto("TRACTOR987654321", "Tractor Seguros Automotriz",
                "Plan seguro auto asegurado", valueUF, 27.81, 11, 27.81/11,
                (27.81/11)*valueUF, 0, "Deducible 0 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan3, "60%", "1500 UF", "90", "4");
        TestPlanDto plan4 = new TestPlanDto("TRACTOR12975678953", "Tractor Seguros Automotriz",
                "Seguro auto premium", valueUF, 20.12, 11, 20.12/11,
                (20.12/11)*valueUF, 10, "Deducible 10 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan4, "75%", "900 UF", "120", "3");
        list.add(plan1);
        list.add(plan2);
        list.add(plan3);
        list.add(plan4);
        return list;
    }

    public List<TestPlanDto> planList2() {
        List<TestPlanDto> list = new ArrayList<>();
        double valueUF = 37000.00;
        String stolenCar = "Valor comercial";
        String workshopType = "Oficial de la marca";
        // Creamos planes de prueba
        TestPlanDto plan1 = new TestPlanDto("SEGUROSALAMEDA045678987", "Seguros Alameda",
                "Asistencia en viaje", valueUF, 23.55, 11, 23.55/11,
                (23.55/11)*valueUF, 5, "Deducible 5 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan1, "70%", "1200 UF", "90", "3");
        TestPlanDto plan2 = new TestPlanDto("SEGUROSALAMEDA123456789", "Seguros Alameda",
                "Tu trasporte asegurado", valueUF, 27.01, 11, 27.01/11,
                (27.01/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan2, "80%", "800 UF", "120", "4");
        list.add(plan1);
        list.add(plan2);
        return list;
    }

    public List<TestPlanDto> planList3() {
        List<TestPlanDto> list = new ArrayList<>();
        double valueUF = 37000.00;
        String stolenCar = "Valor comercial";
        String workshopType = "Oficial de la marca";
        // Creamos planes de prueba
        TestPlanDto plan1 = new TestPlanDto("LOSALAMOS045678987", "Los Alamos Seguros Automotriz",
                "Proteción ultra automóvil", valueUF, 22.03, 11, 22.03/11,
                (22.03/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan1, "65%", "1500 uf", "180", "3");
        TestPlanDto plan2 = new TestPlanDto("LOSALAMOS123456789", "Los Alamos Seguros Automotriz",
                "Plan de automóvil asegurado", valueUF, 21.41, 11, 21.41/11,
                (21.41/11)*valueUF, 3, "Deducible 3 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan2, "75%", "1000 UF", "120", "4");
        TestPlanDto plan3 = new TestPlanDto("LOSALAMOS987654321", "Los Alamos Seguros Automotriz",
                "Seguro MAX automóvil", valueUF, 23.38, 11, 23.38/11,
                (23.38/11)*valueUF, 5, "Deducible 5 UF", 0.0, stolenCar, "",
                "", workshopType);
        this.adjustTestPlan(plan3, "75%", "1200 UF", "90", "3");
        list.add(plan1);
        list.add(plan2);
        list.add(plan3);
        return list;
    }

    public void adjustTestPlan(TestPlanDto testPlan, String lossPercentage, String thirdPartyUF, String daysReplacement,
            String yearsRenewal) {
        // Adjust data
        String totalLoss = "Valor comercial en caso de daños mayores al " + lossPercentage + " del valor";
        String damageThirdParty = "Hasta " + thirdPartyUF + " entre daños emergentes, morales y lucro cesante";
        String detailReplacement = "Limitado hasta " + daysReplacement + " días hábiles, para el reemplazo del vehículo";
        String detailRenewal = "Luego de " + yearsRenewal + " año/s de haber comprado, se habilita la renovación del vehículo";
        // Update plan
        testPlan.setTotalLoss(totalLoss);
        testPlan.setDamageThirdParty(damageThirdParty);
        testPlan.addDetail(detailReplacement).add(detailRenewal);
    }

    // Creación de un cotizador para los flujos: "Iniciando" o "Cotizando"
    public QuoterModel createQuoteStructure(QuoterOwnerModel quoterOwner, QuoterCarModel quoterCar,
            QuoterPurchaserModel quoterPurchaser, String quoterStatus, LocalDateTime currentDateTime) {
        // Estructura de los otros objetos del cotizador (vacíos por el momento)
        QuoterPlanModel quoterPlan = new QuoterPlanModel("", "", "", 0.0, 0.0, 0, 0.0, 0.0, "", 0.0);
        QuoterAddressModel quoterAddress = new QuoterAddressModel("", "", "");
        QuoterPaymentModel quoterPayment = new QuoterPaymentModel("", "", "", "");
        return new QuoterModel(new ObjectId(), quoterStatus, quoterOwner, quoterCar, quoterPurchaser, quoterPlan,
                quoterAddress, quoterPayment, currentDateTime, currentDateTime);
    }

    // Generamos el cuerpo de una nueva transacción
    public TransactionModel generateNovaTransactionStructure(String transactionId, String userCId, QuoterModel quoterDB, String status, int commissionTotal, int commissionScope, String observation, LocalDateTime currentDateTime) {
        String planId = quoterDB.getQuoterPlanData().getQuoterPlanId();
        String quoterId = quoterDB.getQuoterId();
        TransactionModel novaTransaction = new TransactionModel(transactionId, planId, userCId, quoterId, status, commissionTotal,
                commissionScope, true, observation, currentDateTime, currentDateTime, DataHelper.deprecatedDateTime());
        novaTransaction.addCommission(new TransactionComissionModel(userCId, commissionTotal, status));
        return novaTransaction;
    }

    // Checkeamos si existe el usuario con problemas en el arreglo para crearlo o actualizarlo si es el caso
    public void checkReportUsersProblem(List<ReportUserDto> usersProblem, String userId, String userName, String userEmail, String transactionId, int commission, String transactionMessage) {
        for(ReportUserDto userProblem : usersProblem) {
            if(userId.equals(userProblem.getUserId())) {
                userProblem.setName((!userName.equals("")) ? userName : userProblem.getName());
                userProblem.setEmail((!userEmail.equals("")) ? userEmail : userProblem.getEmail());
                userProblem.addTransactionData(new ReportTransactionDataDto(transactionId, commission, transactionMessage));
                return;
            }
        }
        // No está el usuario se agrega en el arreglo
        usersProblem.add(new ReportUserDto(userId, userName, userEmail, commission, "", "Usuario con detalles", null).addTransactionData(new ReportTransactionDataDto(transactionId, commission, transactionMessage)));
    }

    // Checkeamos si existe el usuario con aprobado+ en el arreglo para crearlo o actualizarlo si es el caso
    public void checkReportUsersApproved(List<ReportUserDto> usersApproved, String userId, String userName, String userEmail, ReportAccountDto accountDto, String transactionId, int commission, String transactionMessage) {
        for(ReportUserDto userApproved : usersApproved) {
            if(userId.equals(userApproved.getUserId())) {
                userApproved.setName((!userName.equals("")) ? userName : userApproved.getName());
                userApproved.setEmail((!userEmail.equals("")) ? userEmail : userApproved.getEmail());
                userApproved.setTotalCommission(userApproved.getTotalCommission() + commission);
                userApproved.setAccount((accountDto != null) ? accountDto : userApproved.getAccount());
                userApproved.addTransactionData(new ReportTransactionDataDto(transactionId, commission, transactionMessage));
                return;
            }
        }
        // No está el usuario se agrega en el arreglo
        usersApproved.add(new ReportUserDto(userId, userName, userEmail, commission, "", "Usuario aprobado", accountDto).addTransactionData(new ReportTransactionDataDto(transactionId, commission, transactionMessage)));
    }

    // Agregamos un usuario al arreglo de los usuarios con problemas por una excepción y con mensaje explicativo
    public void addUserProblem(List<ReportUserDto> usersProblem, ReportUserDto userApproved, String generalMessage) {
        String userId = userApproved.getUserId();
        for(ReportUserDto userProblem : usersProblem) {
            if(userId.equals(userProblem.getUserId())) {
                userProblem.setGeneralMessage(userProblem.getGeneralMessage() + " - " + generalMessage);
                userProblem.setName(userApproved.getName());
                userProblem.setEmail(userApproved.getEmail());
                userProblem.setTotalCommission(userProblem.getTotalCommission() + userApproved.getTotalCommission());
                // Agregamos las transacciones que tenía el usuario aprobado
                for(ReportTransactionDataDto transactionData : userApproved.getTransactionData()) {
                    userProblem.addTransactionData(transactionData);
                }
                return;
            }
        }
        // No se encontró el usuario con detalles, y ahora se agrega
        usersProblem.add(new ReportUserDto(userId, userApproved.getName(), userApproved.getEmail(), userApproved.getTotalCommission(), userApproved.getVoucher(), generalMessage, null).setTransactionData(userApproved.getTransactionData()));
    }

    // Revisar si el usuario tiene una cuenta bancaria activa para poder pagale las comisiones
    public AccountModel checkUserAccount(UserModel userDB) {
        for(AccountModel userAccountDB : userDB.getAccounts()) {
            if(userAccountDB.isSelected()) {
                return userAccountDB;
            }
        }
        return null;
    }

    // Buscamos las transacciones para actualizar sus comisiones
    public String manageTransactionsForCommission(List<ReportUserDto> usersRequest, List<TransactionModel> updateTransactionsInDB, TransactionRepository transactionRepository, LocalDateTime currentTime) {
        String message = "";
        String pointOfLastStatus = "Liberado";
        // Primero recuperamos todas las transacciones en objeto que nos permita procesarla como única y con uno o más usuarios que se beneficiaron de la transacción
        List<TransactionDto> transactionsDto = new ArrayList<>();
        for(ReportUserDto userRequest : usersRequest) {
            String userIdRequest = userRequest.getUserId();
            String userEmailRequest = userRequest.getEmail();
            if(DataHelper.isNull(userIdRequest) || DataHelper.isNull(userEmailRequest)) {
                message = "Se encontró valor sin declarar en el usuario con email: " + userEmailRequest;
                return message;
            }
            for(ReportTransactionDataDto reportTransaction : userRequest.getTransactionData()) {
                String reportTransactionId = reportTransaction.transactionId();
                if(DataHelper.isNull(reportTransactionId)) {
                    message = "No se encontró la transacción N°" + reportTransactionId + ", del usuario con email: " + userEmailRequest;
                    return message;
                }
                this.buildTransactionDto(transactionsDto, reportTransactionId, userIdRequest, userEmailRequest);
            }
        }
        // Ahora que tenemos las transacciones como si fuera única (con las comisiones de los usuarios), se puede procesar
        // y si todo sale bien, se puede agregar a la lista de las transacciones que se tienen que actualizar en la db y
        // no se va a pisar la data
        for(TransactionDto transactionDto : transactionsDto) {
            String transactionIdDto = transactionDto.getTransactionId();
            @SuppressWarnings("null")
            Optional<TransactionModel> optionalTransaction = transactionRepository.findById(transactionIdDto);
            if(optionalTransaction.isEmpty()) {
                message = "La transacción con N°" + transactionIdDto + ", no fue encontrada para actualizar las comisiones de los usuarios";
                return message;
            }
            // Se encontró la transacción ahora se verifica que se encontró todas las comisiones de la transacción
            TransactionModel transactionDB = optionalTransaction.get();
            for(TransactionCommissionDto transactionCommissionDto : transactionDto.getCommissions()) {
                String userIdCommissionDto = transactionCommissionDto.getUserId();
                String userEmailCommissionDto = transactionCommissionDto.getUserEmail();
                boolean isUserCommission = false;
                for(TransactionComissionModel transactionCommissionDB : transactionDB.getCommissions()) {
                    if(userIdCommissionDto.equals(transactionCommissionDB.getUserId())) {
                        isUserCommission = true;
                        transactionCommissionDB.setCommissionStatus(pointOfLastStatus);
                        break;
                    }
                }
                if(!isUserCommission) {
                    message = "No se ha podido encontrar usuario en la transacción N°" + transactionIdDto + ", con el email: " + userEmailCommissionDto;
                    return message;
                }
            }
            transactionDB.setUpdatedDate(currentTime);
            // Verificar si se debe actualizar el estado general de la transacción y agregar transacción a transacciones que se deben actualizar
            this.checkTransactionLastStatus(transactionDB, pointOfLastStatus, currentTime);
            updateTransactionsInDB.add(transactionDB);
        }
        return message;
    }

    // Ayuda a construir el objeto de transacciones que beneficia a un usuario o a varios usuarios
    private void buildTransactionDto(List<TransactionDto> transactionsDto, String transactionId, String userId, String userEmail) {
        boolean isTransactionDto = false;
        for(TransactionDto transactionDto : transactionsDto) {
            String transactionIdDto = transactionDto.getTransactionId();
            if(transactionId.equals(transactionIdDto)) {
                isTransactionDto = true;
                transactionDto.addTransactionCommissionDto(new TransactionCommissionDto(userId, userEmail));
                break;
            }
        }
        if(!isTransactionDto) {
            transactionsDto.add(new TransactionDto(transactionId).addTransactionCommissionDto(new TransactionCommissionDto(userId, userEmail)));
        }
    }

    // Actualizar el último estado de la transacción en caso de que todas las comisiones estén con el último estado
    private void checkTransactionLastStatus(TransactionModel transactionDB, String pointOfLastStatus, LocalDateTime currentTime) {
        boolean isLastStatus = true;
        for(TransactionComissionModel transactionCommission : transactionDB.getCommissions()) {
            String commissionStatus = transactionCommission.getCommissionStatus();
            if(!pointOfLastStatus.equals(commissionStatus)) {
                isLastStatus = false;
                break;
            }
        }
        if(isLastStatus) {
            transactionDB.setStatus(pointOfLastStatus);
            transactionDB.setUpdatedDate(currentTime);
        }
    }

    // Buscar los usuarios para actualizar sus datos y crear los objetos de pagos
    public String manageUsersAndPaymentsForCommission(List<ReportUserDto> usersRequest, List<UserModel> updateUsersInDB,
            List<PaymentModel> updatePaymentsInDB, UserRepository userRepository, LocalDateTime currentTime) {
        String message = "";
        String paymentDate = "";
        try {
            DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            paymentDate = currentTime.format(formatterDate);
        } catch (Exception e) {
            LOGGER_MESSAGES.info("No ha sido posible crear el formato para la fecha de pago");
        }
        for(ReportUserDto userRequest : usersRequest) {
            // Primero buscamos usuario en la DB
            String userEmailRequest = userRequest.getEmail();
            Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmailRequest);
            if(userOptional.isEmpty()) {
                message = "No fue posible encontrar el usuario con email: " + userEmailRequest;
                return message;
            }
            UserModel userDB = userOptional.get();
            String userIdRequest = userRequest.getUserId();
            String userIdDB = userDB.getUserId();
            if(userIdRequest == null || userIdDB == null || !userIdRequest.equals(userIdDB)) {
                message = "El id del usuario de la solicitud no es el mismo al id del usuario encontrado con email: " + userEmailRequest;
                return message;
            }
            // Comparamos el total de la comisión cancelada con todas las comisiones asociadas al usuario, además de ir guardando todas las transacciones del usuario
            int expectedCommissions = userRequest.getTotalCommission();
            int calculatedCommissions = 0;
            Set<String> transactionIds = new HashSet<>();
            for(ReportTransactionDataDto reportTransaction : userRequest.getTransactionData()) {
                calculatedCommissions += reportTransaction.commission();
                transactionIds.add(reportTransaction.transactionId());
            }
            if(expectedCommissions != calculatedCommissions) {
                message = "La comisión calculada : $" + calculatedCommissions + ", no es la misma a la comisión esperada: $" + expectedCommissions + ", del usuario con email: " + userEmailRequest;
                return message;
            }
            // Esta bien el usuario y su comisión, ahora se revisa su cuenta a la que se deposito
            ReportAccountDto userAccount = userRequest.getAccount();
            if(userAccount == null) {
                message = "No se ha podido identificar la cuenta bancaria del usuario con email: " + userEmailRequest;
                return message;
            }
            // Esta todo correcto se crea objeto de pago, se actualiza wallet de usuario y se agregan a los objetos para actualizar en DB
            PaymentModel novaPayment = new PaymentModel(new ObjectId(), userIdRequest, userAccount, expectedCommissions, userRequest.getVoucher(), paymentDate, transactionIds, currentTime, currentTime);
            updatePaymentsInDB.add(novaPayment);
            WalletModel wallet = userDB.getWallet();
            wallet.setAvailableBalance(wallet.getAvailableBalance() - expectedCommissions);
            wallet.setTotalBalance(wallet.getAvailableBalance() + wallet.getOutstandingBalance());
            wallet.setPaymentBalance(wallet.getPaymentBalance() + expectedCommissions);
            wallet.addPaymentId(novaPayment.getPaymentId());
            updateUsersInDB.add(userDB);
        } 
        return message;
    }

}
