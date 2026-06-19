package com.referidos.app.segurosref.services.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.dtos.manager.FailedPaymentDto;
import com.referidos.app.segurosref.dtos.manager.PayQuotesReportResponse;
import com.referidos.app.segurosref.dtos.manager.PayQuotesRequest;
import com.referidos.app.segurosref.dtos.manager.UserQuotePaymentDto;
import com.referidos.app.segurosref.dtos.manager.DashboardMetricPointDto;
import com.referidos.app.segurosref.dtos.manager.DashboardSummaryDto;
import com.referidos.app.segurosref.dtos.report.ReportAccountDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.PaymentRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.integrations.email.providers.EmailAppProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;
import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.dtos.manager.DashboardQuoteDto;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.services.ManagerService;
import com.referidos.app.segurosref.dtos.manager.BankPayrollDto;
import com.referidos.app.segurosref.dtos.manager.ConflictDto;
import com.referidos.app.segurosref.dtos.manager.DashboardPaginatedResponseDto;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;

@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final MongoTemplate mongoTemplate;
    private final ReferredRepository referredRepository;
    private final EmailAppProvider emailAppProvider;

    @Value("${moneyfy.api-key}")
    private String apiKeyMF;

    @Value("${moneyfy.commissions.level1}")
    private int commissionUserC;

    @Value("${moneyfy.commissions.level2}")
    private int commissionUserB;

    @Value("${moneyfy.commissions.level3}")
    private int commissionUserA;

    @Override
    public DashboardPaginatedResponseDto getQuotesDashboard(int page, int size, String userId, String quoteStatus) {
        // 1. Construir el pipeline de agregación
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(Aggregation.unwind("quoters"));

        if (userId != null && !userId.isBlank()) {
            operations.add(Aggregation.match(Criteria.where("_id").is(new ObjectId(userId))));
        }

        if (quoteStatus != null && !quoteStatus.isBlank()) {
            operations.add(Aggregation.match(Criteria.where("quoters.quoterStatus").is(quoteStatus)));
        }

        operations.add(Aggregation.sort(Sort.Direction.DESC, "quoters.createdDate"));

        FacetOperation facet = Aggregation.facet()
                .and(Aggregation.count().as("totalElements")).as("countFacet")
                .and(Aggregation.skip((long) page * size), Aggregation.limit(size)).as("dataFacet");
        operations.add(facet);

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<Document> results = mongoTemplate
                .aggregate(aggregation, "users", Document.class);
        Document resultDoc = results.getUniqueMappedResult();

        long totalElements = 0;
        List<Document> dataFacet = new ArrayList<>();

        if (resultDoc != null) {
            List<Document> countFacetList = resultDoc.getList("countFacet", Document.class);
            if (countFacetList != null && !countFacetList.isEmpty()) {
                totalElements = countFacetList.get(0).getInteger("totalElements", 0);
            }
            dataFacet = resultDoc.getList("dataFacet", Document.class);
            if (dataFacet == null) {
                dataFacet = new ArrayList<>();
            }
        }

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        List<DashboardQuoteDto> dashboardQuotes = new ArrayList<>();

        if (dataFacet.isEmpty()) {
            DashboardPaginatedResponseDto.PaginatedData paginatedData = new DashboardPaginatedResponseDto.PaginatedData(
                    dashboardQuotes, page, size, totalElements, totalPages);
            return new DashboardPaginatedResponseDto("Cotizaciones recuperadas exitosamente", 200, paginatedData);
        }

        // 2. Extraer todos los IDs de cotizaciones para buscar transacciones
        List<String> currentQuoterIds = new ArrayList<>();
        for (Document doc : dataFacet) {
            Document quoterDoc = doc.get("quoters", Document.class);
            if (quoterDoc != null && quoterDoc.get("quoterId") != null) {
                currentQuoterIds.add(quoterDoc.get("quoterId").toString());
            }
        }

        // 3. Consultar transacciones masivamente y pasarlas a Diccionario (O(1))
        Map<String, TransactionModel> transactionMap = transactionRepository.findByQuoterIdIn(currentQuoterIds)
                .stream()
                .collect(Collectors.toMap(TransactionModel::getQuoterId, t -> t, (t1, t2) -> t1));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        // 4. Mapeo a DTO controlando nulos
        for (Document doc : dataFacet) {
            String idUser = doc.getObjectId("_id").toString();
            Document personalDataDoc = doc.get("personalData", Document.class);
            com.referidos.app.segurosref.models.UserDataModel personalData = personalDataDoc != null
                    ? mongoTemplate.getConverter().read(com.referidos.app.segurosref.models.UserDataModel.class,
                            personalDataDoc)
                    : null;

            Document quoterDoc = doc.get("quoters", Document.class);
            QuoterModel quoter = quoterDoc != null
                    ? mongoTemplate.getConverter().read(QuoterModel.class, quoterDoc)
                    : null;

            if (quoter == null)
                continue;

            DashboardQuoteDto dto = new DashboardQuoteDto();

            // Datos obligatorios
            dto.setIdUser(idUser);
            dto.setUserFullname(
                    personalData != null ? personalData.getName() + " " + personalData.getSurname() : "N/A");
            dto.setUserEmail(personalData != null ? personalData.getEmail() : "N/A");
            dto.setQuoteId(quoter.getQuoterId());
            dto.setQuoteStatus(quoter.getQuoterStatus() != null ? quoter.getQuoterStatus() : "N/A");
            dto.setQuoterCarPpu(quoter.getQuoterCarData() != null && quoter.getQuoterCarData().getPpu() != null
                    ? quoter.getQuoterCarData().getPpu()
                    : "N/A");
            dto.setInicialDate(quoter.getCreatedDate() != null ? quoter.getCreatedDate().format(formatter) : "N/A");

            // Datos opcionales: Owner
            if (quoter.getQuoterOwnerData() != null) {
                dto.setQuoterOwnerPersonalId(quoter.getQuoterOwnerData().getPersonalId() != null
                        ? quoter.getQuoterOwnerData().getPersonalId()
                        : "N/A");
                dto.setQuoterOwnerFullname(quoter.getQuoterOwnerData().getName() + " "
                        + quoter.getQuoterOwnerData().getPaternalSurname());
            } else {
                dto.setQuoterOwnerPersonalId("N/A");
                dto.setQuoterOwnerFullname("N/A");
            }

            // Datos opcionales: Car
            if (quoter.getQuoterCarData() != null) {
                dto.setQuoterCarBrand(
                        quoter.getQuoterCarData().getBrand() != null ? quoter.getQuoterCarData().getBrand() : "N/A");
                dto.setQuoterCarModel(
                        quoter.getQuoterCarData().getModel() != null ? quoter.getQuoterCarData().getModel() : "N/A");
                dto.setQuoterCarYear(
                        quoter.getQuoterCarData().getYear() != null ? quoter.getQuoterCarData().getYear() : "N/A");
                dto.setQuoterCarType(
                        quoter.getQuoterCarData().getType() != null ? quoter.getQuoterCarData().getType() : "N/A");
            } else {
                dto.setQuoterCarBrand("N/A");
                dto.setQuoterCarModel("N/A");
                dto.setQuoterCarYear("N/A");
                dto.setQuoterCarType("N/A");
            }

            // Datos opcionales: Buyer
            if (quoter.getQuoterPurchaserData() != null) {
                dto.setQuoterBuyerPersonalId(quoter.getQuoterPurchaserData().getPersonalId() != null
                        ? quoter.getQuoterPurchaserData().getPersonalId()
                        : "N/A");
                dto.setQuoterBuyerFullname(quoter.getQuoterPurchaserData().getName() + " "
                        + quoter.getQuoterPurchaserData().getPaternalSurname());
                dto.setQuoterBuyerEmail(quoter.getQuoterPurchaserData().getEmail() != null
                        ? quoter.getQuoterPurchaserData().getEmail()
                        : "N/A");
                dto.setQuoterBuyerPhone(quoter.getQuoterPurchaserData().getPhone() != null
                        ? quoter.getQuoterPurchaserData().getPhone()
                        : "N/A");
            } else {
                dto.setQuoterBuyerPersonalId("N/A");
                dto.setQuoterBuyerFullname("N/A");
                dto.setQuoterBuyerEmail("N/A");
                dto.setQuoterBuyerPhone("N/A");
            }

            // Datos opcionales: Plan
            if (quoter.getQuoterPlanData() != null) {
                dto.setQuoterPlanInsurer(
                        quoter.getQuoterPlanData().getInsurer() != null ? quoter.getQuoterPlanData().getInsurer()
                                : "N/A");
                dto.setQuoterPlanName(
                        quoter.getQuoterPlanData().getPlanName() != null ? quoter.getQuoterPlanData().getPlanName()
                                : "N/A");
                dto.setQuoterPlanUf(quoter.getQuoterPlanData().getValueUF() != null
                        ? quoter.getQuoterPlanData().getValueUF().toString()
                        : "0.0");
                dto.setQuoterPlanMonthlyPriceUF(quoter.getQuoterPlanData().getMonthlyPriceUF() != null
                        ? quoter.getQuoterPlanData().getMonthlyPriceUF()
                        : BigDecimal.ZERO);
                dto.setQuoterPlanMonthlyPrice(quoter.getQuoterPlanData().getMonthlyPrice() != null
                        ? quoter.getQuoterPlanData().getMonthlyPrice()
                        : BigDecimal.ZERO);
                dto.setQuoterPlanMonths(12);
            } else {
                dto.setQuoterPlanInsurer("N/A");
                dto.setQuoterPlanName("N/A");
                dto.setQuoterPlanUf("0.0");
                dto.setQuoterPlanMonthlyPriceUF(BigDecimal.ZERO);
                dto.setQuoterPlanMonthlyPrice(BigDecimal.ZERO);
                dto.setQuoterPlanMonths(0);
            }

            // Datos opcionales: Address
            if (quoter.getQuoterAddressData() != null) {
                dto.setQuoterAddressStreet(
                        quoter.getQuoterAddressData().getStreet() != null ? quoter.getQuoterAddressData().getStreet()
                                : "N/A");
                dto.setQuoterAddressStreetNumber(quoter.getQuoterAddressData().getStreetNumber() != null
                        ? quoter.getQuoterAddressData().getStreetNumber()
                        : "N/A");
            } else {
                dto.setQuoterAddressStreet("N/A");
                dto.setQuoterAddressStreetNumber("N/A");
            }

            // Datos opcionales: Transaction
            TransactionModel trans = transactionMap.get(quoter.getQuoterId());
            if (trans != null) {
                dto.setTransactionId(trans.getTransactionId());
                dto.setTransactionStatus(trans.getStatus() != null ? trans.getStatus() : "N/A");
                String commStatus = "N/A";
                String paymentDateStr = "N/A";
                if (trans.getCommissions() != null) {
                    for (TransactionComissionModel comm : trans.getCommissions()) {
                        if (comm.getUserId().equals(idUser)) {
                            commStatus = comm.getCommissionStatus();
                            if (comm.getPaymentDate() != null
                                    && !comm.getPaymentDate().equals(DataHelper.deprecatedDateTime())) {
                                paymentDateStr = comm.getPaymentDate().format(formatter);
                            } else if (trans.getPaymentDate() != null
                                    && !trans.getPaymentDate().equals(DataHelper.deprecatedDateTime())) {
                                paymentDateStr = trans.getPaymentDate().format(formatter);
                            }
                            break;
                        }
                    }
                }
                dto.setCommissionStatus(commStatus);
                dto.setPaymentDate(paymentDateStr);
                dto.setApprovalDate(
                        trans.getApprovalDate() != null ? trans.getApprovalDate().format(formatter) : "N/A");
                dto.setTransactionTotalCommission(trans.getCommissionTotal());
                dto.setTransactionTotalScope(trans.getCommissionScope());
            } else {
                dto.setTransactionId("N/A");
                dto.setTransactionStatus("N/A");
                dto.setCommissionStatus("N/A");
                dto.setPaymentDate("N/A");
                dto.setApprovalDate("N/A");
                dto.setTransactionTotalCommission(0);
                dto.setTransactionTotalScope(0);
            }

            dashboardQuotes.add(dto);
        }

        DashboardPaginatedResponseDto.PaginatedData paginatedData = new DashboardPaginatedResponseDto.PaginatedData(
                dashboardQuotes, page, size, totalElements, totalPages);
        return new DashboardPaginatedResponseDto("Cotizaciones recuperadas exitosamente", 200, paginatedData);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardSummary() {
        List<UserModel> users = userRepository.findAll();
        List<TransactionModel> transactions = transactionRepository.findAll();

        LocalDateTime todayStart = LocalDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime firstDay = todayStart.minusDays(6);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");

        List<DashboardMetricPointDto> weeklyMetrics = new ArrayList<>();
        Map<String, DashboardMetricPointDto> bucketByDate = new HashMap<>();
        Map<String, Set<String>> usersByDay = new HashMap<>();

        for (int i = 0; i < 7; i++) {
            LocalDateTime bucketDate = firstDay.plusDays(i);
            String dateKey = bucketDate.toLocalDate().format(dateFormatter);
            DashboardMetricPointDto point = new DashboardMetricPointDto(
                    dateKey,
                    bucketDate.toLocalDate().format(labelFormatter),
                    0,
                    0,
                    0);
            weeklyMetrics.add(point);
            bucketByDate.put(dateKey, point);
            usersByDay.put(dateKey, new java.util.HashSet<>());
        }

        int activeUsers = 0;
        for (UserModel user : users) {
            if (user.getQuoters() == null || user.getQuoters().isEmpty()) {
                continue;
            }

            activeUsers += 1;
            for (QuoterModel quoter : user.getQuoters()) {
                if (quoter.getCreatedDate() == null || quoter.getCreatedDate().isBefore(firstDay)) {
                    continue;
                }

                String dateKey = quoter.getCreatedDate().toLocalDate().format(dateFormatter);
                Set<String> bucketUsers = usersByDay.get(dateKey);
                if (bucketUsers != null) {
                    bucketUsers.add(user.getUserId());
                }
            }
        }

        int paidCommissions = 0;
        int pendingCommissions = 0;

        for (TransactionModel transaction : transactions) {
            String status = transaction.getStatus();
            int amount = transaction.getCommissionTotal();

            if ("Pagado".equals(status)) {
                paidCommissions += amount;
            }

            if ("Pendiente".equals(status) || "Aprobado".equals(status)) {
                pendingCommissions += amount;
            }

            if (transaction.getApprovalDate() == null || transaction.getApprovalDate().isBefore(firstDay)) {
                continue;
            }

            if (!"Aprobado".equals(status) && !"Pagado".equals(status)) {
                continue;
            }

            String dateKey = transaction.getApprovalDate().toLocalDate().format(dateFormatter);
            DashboardMetricPointDto point = bucketByDate.get(dateKey);
            if (point == null) {
                continue;
            }

            point.setCommissions(point.getCommissions() + amount);
            point.setSales(point.getSales() + 1);
        }

        weeklyMetrics.forEach((point) -> {
            Set<String> bucketUsers = usersByDay.get(point.getDate());
            point.setUsers(bucketUsers != null ? bucketUsers.size() : 0);
        });

        DashboardSummaryDto summary = new DashboardSummaryDto(
                activeUsers,
                paidCommissions,
                pendingCommissions,
                weeklyMetrics);

        return ResponseHelper.response("Solicitud realizada: Resumen dashboard generado", 200, summary);
    }

    @SuppressWarnings("null")
    @Transactional
    @Override
    public ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, HttpServletRequest request) {
        if (!ValidateInputHelper.checkApiKeyMF(apiKeyMF, request.getHeader("X-Moneyfy-Api-Key"))) {
            return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
        }

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

    @SuppressWarnings("null")
    @Override
    public ResponseEntity<?> payQuotes(PayQuotesRequest request) {
        if (request == null || request.getUsersQuotes() == null || request.getUsersQuotes().isEmpty()) {
            return ResponseHelper.failedDependency("La solicitud no contiene usuarios a procesar", "failed dependency");
        }

        List<FailedPaymentDto> failedPayments = new ArrayList<>();
        Map<String, UserModel> usersToSave = new HashMap<>();
        Map<String, TransactionModel> transactionsToSave = new HashMap<>();
        List<PaymentModel> paymentsToSave = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (UserQuotePaymentDto userQuote : request.getUsersQuotes()) {
            String userId = userQuote.getUserId();
            Set<String> transactionIds = userQuote.getTransactions();

            if (userId == null || !ObjectId.isValid(userId) || transactionIds == null || transactionIds.isEmpty()) {
                failedPayments
                        .add(new FailedPaymentDto(userId, transactionIds, "ID de usuario o transacciones no válidos"));
                continue;
            }

            Optional<UserModel> userOpt = userRepository.findById(new ObjectId(userId));
            if (userOpt.isEmpty()) {
                failedPayments.add(new FailedPaymentDto(userId, transactionIds, "Usuario no encontrado"));
                continue;
            }

            UserModel user = userOpt.get();
            if (usersToSave.containsKey(userId)) {
                user = usersToSave.get(userId);
            }

            // Validar que todas las transacciones existan y su comisión asociada al usuario esten en Aprobado
            boolean validTransactions = true;
            List<TransactionModel> userTransactions = new ArrayList<>();
            for (String txId : transactionIds) {
                TransactionModel tx = null;
                if (transactionsToSave.containsKey(txId)) {
                    tx = transactionsToSave.get(txId);
                } else {
                    Optional<TransactionModel> txOpt = transactionRepository.findById(txId);
                    if (txOpt.isPresent()) {
                        tx = txOpt.get();
                    }
                }

                if (tx == null) {
                    validTransactions = false;
                    break;
                }

                boolean userCommissionApproved = false;
                if (tx.getCommissions() != null) {
                    for (TransactionComissionModel comm : tx.getCommissions()) {
                        if (comm.getUserId().equals(userId) && "Aprobado".equals(comm.getCommissionStatus())) {
                            userCommissionApproved = true;
                            break;
                        }
                    }
                }

                if (!userCommissionApproved) {
                    validTransactions = false;
                    break;
                }
                userTransactions.add(tx);
            }

            if (!validTransactions || userTransactions.size() != transactionIds.size()) {
                failedPayments.add(new FailedPaymentDto(userId, transactionIds,
                        "Una o más transacciones no existen o no están en estado Aprobado para este usuario (Todo o Nada)"));
                continue; // Todo o Nada
            }

            String transactionStatus = userQuote.getUserTransactionStatus();
            if (transactionStatus == null || (!transactionStatus.equals("Pagado") && !transactionStatus.equals("Conflictivo"))) {
                failedPayments.add(new FailedPaymentDto(userId, transactionIds, "Estado de transacción inválido"));
                continue;
            }

            String userNote = userQuote.getUserNote();
            if (transactionStatus.equals("Conflictivo") && DataHelper.isNull(userNote)) {
                userNote = "No se proporcionaron datos suficientes, verifique que los datos de su cuenta bancaria esten actualizados";
            } else if (DataHelper.isNull(userNote)) {
                userNote = "";
            }

            String userVoucher = userQuote.getUserVoucher();
            if (DataHelper.isNull(userVoucher)) {
                userVoucher = "";
            }

            WalletModel wallet = user.getWallet();

            // Actualizar Wallet si es Pagado
            if ("Pagado".equals(transactionStatus)) {
                int currentAvailable = wallet.getAvailableBalance();
                wallet.setAvailableBalance(currentAvailable - userQuote.getUserPayment());
                wallet.setTotalBalance(wallet.getAvailableBalance() + wallet.getOutstandingBalance());
                wallet.setPaymentBalance(wallet.getPaymentBalance() + userQuote.getUserPayment());
            } else {
                // Enviar correo si es Conflictivo
                String userEmail = user.getPersonalData() != null ? user.getPersonalData().getEmail() : null;
                if (userEmail != null) {
                    emailAppProvider.notifyConflictivePayment(userEmail, userNote);
                }
            }

            // Crear Payment obligatoriamente
            ObjectId newPaymentId = new ObjectId();
            PaymentModel payment = new PaymentModel(newPaymentId, userId, userQuote.getUserAccount(),
                    userQuote.getUserPayment(),
                    userVoucher, transactionStatus, userNote, transactionIds, now, now);
            paymentsToSave.add(payment);

            wallet.addPaymentId(newPaymentId.toString());

            // Actualizar Transacciones y Comisiones
            for (TransactionModel tx : userTransactions) {
                if (tx.getCommissions() != null) {
                    for (TransactionComissionModel comm : tx.getCommissions()) {
                        if (comm.getUserId().equals(userId)) {
                            comm.setCommissionStatus(transactionStatus);
                            comm.setObservation(userNote);
                            comm.setPaymentDate(now);
                            break;
                        }
                    }
                }
                tx.setUpdatedDate(now);
                transactionsToSave.put(tx.getTransactionId(), tx);
            }

            usersToSave.put(userId, user);
        }

        // Revisión posterior: Actualizar transacciones generales y cotizaciones si
        // las comisiones están Pagadas o en Conflicto
        for (TransactionModel tx : transactionsToSave.values()) {
            boolean hasConflict = false;
            boolean allPaid = true;
            String ownerCommissionStatus = null;

            if (tx.getCommissions() != null && !tx.getCommissions().isEmpty()) {
                for (TransactionComissionModel comm : tx.getCommissions()) {
                    if ("Conflictivo".equals(comm.getCommissionStatus())) {
                        hasConflict = true;
                        allPaid = false;
                    } else if (!"Pagado".equals(comm.getCommissionStatus())) {
                        allPaid = false;
                    }
                    if (comm.getUserId().equals(tx.getUserId())) {
                        ownerCommissionStatus = comm.getCommissionStatus();
                    }
                }
            } else {
                allPaid = false;
            }

            if (hasConflict) {
                tx.setStatus("Conflictivo");
                tx.setPaymentDate(now);
            } else if (allPaid) {
                tx.setStatus("Pagado");
                tx.setPaymentDate(now);
            }

            // Actualizar el estado del Quoter basado SOLAMENTE en el estado de la comisión del dueño principal
            if ("Pagado".equals(ownerCommissionStatus) || "Conflictivo".equals(ownerCommissionStatus)) {
                String ownerId = tx.getUserId();
                if (ownerId != null && ObjectId.isValid(ownerId)) {
                    UserModel owner;
                    if (usersToSave.containsKey(ownerId)) {
                        owner = usersToSave.get(ownerId);
                    } else {
                        Optional<UserModel> ownerOpt = userRepository.findById(new ObjectId(ownerId));
                        if (ownerOpt.isPresent()) {
                            owner = ownerOpt.get();
                            usersToSave.put(ownerId, owner);
                        } else {
                            continue; // Owner not found
                        }
                    }

                    if (owner.getQuoters() != null) {
                        for (com.referidos.app.segurosref.models.QuoterModel q : owner.getQuoters()) {
                            if (q.getQuoterId().equals(tx.getQuoterId())) {
                                q.setQuoterStatus(ownerCommissionStatus);
                                q.setUpdatedDate(now);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Guardado Batch
        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave.values());
        }
        if (!transactionsToSave.isEmpty()) {
            transactionRepository.saveAll(transactionsToSave.values());
        }
        if (!paymentsToSave.isEmpty()) {
            paymentRepository.saveAll(paymentsToSave);
        }

        return ResponseHelper.ok("Proceso de pagos completado", Map.of("failedPayments", failedPayments));
    }

    @SuppressWarnings("null")
    @Override
    public ResponseEntity<?> generatePayQuotesReport(
            com.referidos.app.segurosref.dtos.manager.PayQuotesReportRequest request) {
        if (request == null || request.getDateFrom() == null || request.getDateTo() == null) {
            return ResponseHelper.failedDependency("La solicitud no contiene el rango de fechas válido",
                    "failed dependency");
        }

        LocalDateTime dateFrom = request.getDateFrom().atStartOfDay();
        LocalDateTime dateTo = request.getDateTo().atTime(23, 59, 59);

        List<TransactionModel> approvedTransactions = transactionRepository
                .findAllByApprovalDateBetweenAndStatus(dateFrom, dateTo, "Aprobado");

        Map<String, List<TransactionModel>> transactionsByUser = new HashMap<>();

        // Agrupar transacciones por userId a partir de las comisiones en estado
        // Aprobado
        // También detectar si el usuario tiene transacciones en estado Conflictivo globalmente
        Set<String> usersWithConflicts = new HashSet<>();
        List<TransactionModel> conflictiveTransactions = transactionRepository.findAllByStatus("Conflictivo");
        for (TransactionModel ctx : conflictiveTransactions) {
            if (ctx.getCommissions() != null) {
                for (TransactionComissionModel ccomm : ctx.getCommissions()) {
                    if ("Conflictivo".equals(ccomm.getCommissionStatus())) {
                        usersWithConflicts.add(ccomm.getUserId());
                    }
                }
            }
        }

        for (TransactionModel tx : approvedTransactions) {
            if (tx.getCommissions() != null) {
                for (TransactionComissionModel comm : tx.getCommissions()) {
                    if ("Aprobado".equals(comm.getCommissionStatus())) {
                        String userId = comm.getUserId();
                        transactionsByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(tx);
                    }
                }
            }
        }

        List<BankPayrollDto> bankPayroll = new ArrayList<>();
        List<UserQuotePaymentDto> backendPayload = new ArrayList<>();
        List<ConflictDto> conflicts = new ArrayList<>();

        if (transactionsByUser.isEmpty()) {
            return ResponseHelper.response("Solicitud realizada: Reporte generado", 200,
                    new PayQuotesReportResponse(bankPayroll, backendPayload,
                            conflicts));
        }

        List<ObjectId> userIds = transactionsByUser.keySet().stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        List<UserModel> users = userRepository.findAllById(userIds);
        Map<String, UserModel> usersMap = users.stream().collect(Collectors.toMap(UserModel::getUserId, u -> u));

        for (Map.Entry<String, List<TransactionModel>> entry : transactionsByUser.entrySet()) {
            String userId = entry.getKey();
            List<TransactionModel> userTransactions = entry.getValue();

            UserModel user = usersMap.get(userId);
            if (user == null) {
                conflicts.add(new ConflictDto(userId, "N/A",
                        "Usuario no encontrado en la base de datos"));
                continue;
            }

            String userName = user.getPersonalData() != null
                    ? user.getPersonalData().getName() + " " + user.getPersonalData().getSurname()
                    : "N/A";

            if (usersWithConflicts.contains(userId)) {
                conflicts.add(new ConflictDto(userId, userName,
                        "Advertencia: El usuario tiene otras comisiones en estado Conflictivo pendientes de revisión. Sin embargo, sus comisiones aprobadas están en la nómina."));
            }

            AccountModel selectedAccount = null;
            if (user.getAccounts() != null) {
                selectedAccount = user.getAccounts().stream()
                        .filter(AccountModel::isSelected).findFirst().orElse(null);
            }

            if (selectedAccount == null) {
                conflicts.add(new ConflictDto(userId, userName,
                        "Usuario no tiene cuenta bancaria confirmada/seleccionada"));
                continue;
            }

            int calculatedTotal = 0;
            Set<String> transactionIds = new HashSet<>();

            for (TransactionModel tx : userTransactions) {
                transactionIds.add(tx.getTransactionId());
                for (TransactionComissionModel comm : tx.getCommissions()) {
                    if (comm.getUserId().equals(userId) && "Aprobado".equals(comm.getCommissionStatus())) {
                        calculatedTotal += comm.getUserCommission();
                    }
                }
            }

            if (calculatedTotal <= 0) {
                conflicts.add(new ConflictDto(userId, userName,
                        "Inconsistencia matemática: El monto total a pagar calculado es <= 0"));
                continue;
            }

            ReportAccountDto reportAccount = new ReportAccountDto(
                    selectedAccount.getPersonalId(),
                    selectedAccount.getHolderName(),
                    selectedAccount.getEmail(),
                    selectedAccount.getBank(),
                    selectedAccount.getAccountType(),
                    selectedAccount.getAccountNumber());

            bankPayroll.add(new BankPayrollDto(userId, reportAccount,
                    calculatedTotal));

            backendPayload.add(new UserQuotePaymentDto(
                    userId,
                    "",
                    "",
                    transactionIds,
                    reportAccount,
                    calculatedTotal,
                    ""));
        }

        return ResponseHelper.response("Solicitud realizada: Reporte generado", 200,
                new PayQuotesReportResponse(bankPayroll, backendPayload,
                        conflicts));
    }

}

