package com.referidos.app.segurosref.services.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            if (quoterDoc != null && quoterDoc.getString("quoterId") != null) {
                currentQuoterIds.add(quoterDoc.getString("quoterId"));
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
                dto.setCommissionStatus(trans.getCommissions() != null && !trans.getCommissions().isEmpty()
                        ? trans.getCommissions().get(0).getCommissionStatus()
                        : "N/A");
                dto.setApprovalDate(
                        trans.getApprovalDate() != null ? trans.getApprovalDate().format(formatter) : "N/A");
                dto.setTransactionTotalCommission(trans.getCommissionTotal());
                dto.setTransactionTotalScope(trans.getCommissionScope());
            } else {
                dto.setTransactionId("N/A");
                dto.setTransactionStatus("N/A");
                dto.setCommissionStatus("N/A");
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
}
