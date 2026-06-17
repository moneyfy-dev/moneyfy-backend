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
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import com.referidos.app.segurosref.dtos.manager.FailedPaymentDto;
import com.referidos.app.segurosref.dtos.manager.PayQuotesRequest;
import com.referidos.app.segurosref.dtos.manager.UserQuotePaymentDto;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.PaymentModel;
import com.referidos.app.segurosref.models.TransactionComissionModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.PaymentRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

import org.springframework.stereotype.Service;

import com.referidos.app.segurosref.dtos.manager.DashboardQuoteDto;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.repositories.TransactionRepository;
import com.referidos.app.segurosref.services.ManagerService;
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
                                    && !comm.getPaymentDate().equals(DataHelper.deprecatedDate())) {
                                paymentDateStr = comm.getPaymentDate().format(formatter);
                            } else if (trans.getPaymentDate() != null
                                    && !trans.getPaymentDate().equals(DataHelper.deprecatedDate())) {
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

            // Validar que todas las transacciones existan y esten en Aprobado
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

                if (tx == null || !"Aprobado".equals(tx.getStatus())) {
                    validTransactions = false;
                    break;
                }
                userTransactions.add(tx);
            }

            if (!validTransactions || userTransactions.size() != transactionIds.size()) {
                failedPayments.add(new FailedPaymentDto(userId, transactionIds,
                        "Una o más transacciones no existen o no están en estado Aprobado (Todo o Nada)"));
                continue; // Todo o Nada
            }

            // Actualizar Wallet
            WalletModel wallet = user.getWallet();
            int currentAvailable = wallet.getAvailableBalance();
            wallet.setAvailableBalance(currentAvailable - userQuote.getUserPayment());
            wallet.setTotalBalance(wallet.getAvailableBalance() + wallet.getOutstandingBalance());
            wallet.setPaymentBalance(wallet.getPaymentBalance() + userQuote.getUserPayment());

            // Crear Payment
            ObjectId newPaymentId = new ObjectId();
            PaymentModel payment = new PaymentModel(newPaymentId, userId, userQuote.getUserAccount(),
                    userQuote.getUserPayment(),
                    userQuote.getUserVoucher(), transactionIds, now, now);
            paymentsToSave.add(payment);

            wallet.addPaymentId(newPaymentId.toString());

            // Actualizar Transacciones y Comisiones
            for (TransactionModel tx : userTransactions) {
                if (tx.getCommissions() != null) {
                    for (TransactionComissionModel comm : tx.getCommissions()) {
                        if (comm.getUserId().equals(userId)) {
                            comm.setCommissionStatus("Pagado");
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
        // todas las comisiones estan Pagadas
        for (TransactionModel tx : transactionsToSave.values()) {
            boolean allPaid = true;
            if (tx.getCommissions() != null && !tx.getCommissions().isEmpty()) {
                for (TransactionComissionModel comm : tx.getCommissions()) {
                    if (!"Pagado".equals(comm.getCommissionStatus())) {
                        allPaid = false;
                        break;
                    }
                }
            } else {
                allPaid = false;
            }

            if (allPaid) {
                tx.setStatus("Pagado");
                tx.setPaymentDate(now);

                // Buscar al dueño del quoter y actualizar el quoterStatus a Pagado
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
                                q.setQuoterStatus("Pagado");
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

}
