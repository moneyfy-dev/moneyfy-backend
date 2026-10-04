package com.referidos.app.segurosref.services.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.referidos.app.segurosref.dtos.manager.DashboardSummaryDto;
import com.referidos.app.segurosref.dtos.manager.CommissionLedgerPageDto;
import com.referidos.app.segurosref.dtos.manager.CommissionReconciliationDto;
import com.referidos.app.segurosref.dtos.manager.MoneyfyersResponseDto;
import com.referidos.app.segurosref.integrations.email.providers.EmailAppProvider;
import com.referidos.app.segurosref.models.*;
import com.referidos.app.segurosref.repositories.*;
import com.referidos.app.segurosref.responses.GeneralResponse;
import org.springframework.data.mongodb.core.MongoTemplate;

class ManagerCommissionReportingTest {
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ManagerRepository managers = mock(ManagerRepository.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ManagerServiceImpl service = new ManagerServiceImpl(transactions, users,
            mock(PaymentRepository.class), mongo, mock(EmailAppProvider.class), managers);

    private void authorize() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager@example.invalid", "unused"));
        when(managers.findByEmail("manager@example.invalid")).thenReturn(java.util.Optional.of(mock(ManagerModel.class)));
        when(users.findAll()).thenReturn(List.of());
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private TransactionModel transaction(String status, TransactionComissionModel... commissions) {
        var now = LocalDateTime.now();
        var tx = new TransactionModel("tx", "plan", "owner", "quote", status, 999999, 1, false, "",
                now, now, now, now);
        tx.setCommissions(List.of(commissions));
        return tx;
    }

    private TransactionComissionModel commission(String userId, int amount, String status) {
        return new TransactionComissionModel(userId, amount, status, "", LocalDateTime.now());
    }

    private DashboardSummaryDto summary() {
        var body = (GeneralResponse) service.getDashboardSummary().getBody();
        return (DashboardSummaryDto) ((Map<?, ?>) body.data()).get("summary");
    }

    @Test void approvalPendingIsNotMoneyOwedForPayment() {
        authorize();
        when(transactions.findAll()).thenReturn(List.of(
                transaction("Pendiente", commission("owner", 245000, "Pendiente")),
                transaction("Aprobado", commission("owner", 35000, "Aprobado"))));
        assertEquals(35000, summary().getPendingCommissions());
    }

    @Test void partialPaymentsUseEachBeneficiaryStatusAndHistoricalAmount() {
        authorize();
        when(transactions.findAll()).thenReturn(List.of(transaction("Aprobado",
                commission("owner", 35000, "Pagado"), commission("referrer", 10000, "Aprobado"))));
        var result = summary();
        assertEquals(35000, result.getPaidCommissions());
        assertEquals(10000, result.getPendingCommissions());
    }

    @Test void unauthenticatedManagerDoesNotLoadFinancialRecords() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("absent@example.invalid", "unused"));
        assertEquals(401, service.getDashboardSummary().getStatusCode().value());
        verifyNoInteractions(transactions, users);
    }

    private CommissionLedgerPageDto details(String status, LocalDate from, LocalDate to, int page, int size, String userId) {
        return (CommissionLedgerPageDto) ((GeneralResponse) service
                .getDashboardCommissionLedger(status, from, to, page, size, userId).getBody()).data();
    }

    @Test void pendingApprovalAndConflictAreSeparateFromPayableAndPaid() {
        authorize();
        when(transactions.findAll()).thenReturn(List.of(transaction("Conflictivo",
                commission("owner", 245000, "Pendiente"), commission("b", 10000, "Aprobado"),
                commission("c", 5000, "Conflictivo"), commission("d", 35000, "Pagado"),
                commission("e", 90000, "Rechazado"), commission("f", 1000, "Caducado"))));
        var result = summary();
        assertEquals(245000, result.getPendingApprovalCommissions());
        assertEquals(10000, result.getPendingCommissions());
        assertEquals(5000, result.getConflictCommissions());
        assertEquals(35000, result.getPaidCommissions());
    }

    @Test void summaryAndDetailsUseTheSameInclusiveCreationPeriod() {
        authorize();
        var first = transaction("Aprobado", commission("a", 100, "Aprobado"));
        first.setCreatedDate(LocalDateTime.of(2026, 8, 4, 0, 0));
        var last = transaction("Aprobado", commission("b", 200, "Aprobado"));
        last.setCreatedDate(LocalDateTime.of(2026, 10, 4, 23, 59, 59));
        var old = transaction("Aprobado", commission("c", 500, "Aprobado"));
        old.setCreatedDate(LocalDateTime.of(2026, 8, 3, 23, 59));
        var unknownDate = transaction("Aprobado", commission("d", 50, "Aprobado"));
        unknownDate.setCreatedDate(null);
        when(transactions.findAll()).thenReturn(List.of(first, last, old, unknownDate));
        var from = LocalDate.of(2026, 8, 4);
        var to = LocalDate.of(2026, 10, 4);
        var result = (DashboardSummaryDto) ((Map<?, ?>) ((GeneralResponse)
                service.getDashboardSummary(from, to).getBody()).data()).get("summary");
        var detail = details("Aprobado", from, to, 0, 10, null);
        assertEquals(300, result.getPendingCommissions());
        assertEquals(result.getPendingCommissions(), detail.totalAmount());
        assertEquals(2, detail.totalElements());
        assertEquals(850, details("Aprobado", null, null, 0, 10, null).totalAmount());
    }

    @Test void ledgerShowsTransactionsEvenWhenQuotationIsMissing() {
        authorize();
        when(transactions.findAll()).thenReturn(List.of(transaction("Pendiente", commission("owner", 245000, "Pendiente"))));
        var result = details("Pendiente", null, null, 0, 10, null);
        assertEquals(245000, result.totalAmount());
        assertEquals("quote", result.content().getFirst().quoteId());
        assertTrue(result.content().getFirst().quoteMissing());
    }

    @Test void totalsAreForAllMatchingRowsNotJustThePage() {
        authorize();
        when(transactions.findAll()).thenReturn(List.of(transaction("Aprobado", commission("a", 10, "Aprobado"),
                commission("b", 20, "Aprobado"), commission("c", 30, "Aprobado"))));
        var result = details("Aprobado", null, null, 1, 2, null);
        assertEquals(1, result.content().size());
        assertEquals(3, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(60, result.totalAmount());
        assertEquals(20, details("Aprobado", null, null, 0, 10, "b").totalAmount());
        assertTrue(details("Aprobado", null, null, Integer.MAX_VALUE, 100, null).content().isEmpty());
    }

    @Test void totalUsesLongToAvoidOverflow() {
        authorize();
        when(transactions.findAll()).thenReturn(List.of(transaction("Aprobado", commission("a", 2000000000, "Aprobado"),
                commission("b", 2000000000, "Aprobado"))));
        assertEquals(4000000000L, summary().getPendingCommissions());
        assertEquals(4000000000L, details("Aprobado", null, null, 0, 10, null).totalAmount());
    }

    @Test void invalidFiltersAreRejectedBeforeLoadingFinancialRecords() {
        authorize();
        var from = LocalDate.of(2026, 10, 4);
        var to = LocalDate.of(2026, 8, 4);
        assertEquals(400, service.getDashboardSummary(from, to).getStatusCode().value());
        assertEquals(400, service.getDashboardCommissionLedger("Pendiente", from, to, 0, 10, null).getStatusCode().value());
        assertEquals(400, service.getDashboardCommissionLedger("Cotizando", null, null, 0, 10, null).getStatusCode().value());
        assertEquals(400, service.getDashboardCommissionLedger("Pagado", null, null, -1, 10, null).getStatusCode().value());
        assertEquals(400, service.getDashboardCommissionLedger("Pagado", null, null, 0, 101, null).getStatusCode().value());
        verifyNoInteractions(transactions);
    }

    @Test void allNewReportsRejectUnknownManagers() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("absent@example.invalid", "unused"));
        assertEquals(401, service.getDashboardCommissionLedger("Pagado", null, null, 0, 10, null).getStatusCode().value());
        assertEquals(401, service.getCommissionReconciliation().getStatusCode().value());
        verifyNoInteractions(transactions, users);
    }

    @Test void reconciliationReportsWalletDriftWithoutWritingOrReclassifyingIt() {
        authorize();
        var user = new UserModel("", null,
                new UserDataModel("Alejandro", "", "person@example.invalid", "", "", null, "", null),
                new WalletModel(245000, 245000, 0, 0), null);
        user.setUserId(new org.bson.types.ObjectId());
        when(users.findAll()).thenReturn(List.of(user));
        when(transactions.findAll()).thenReturn(List.of(transaction("Pendiente", commission(user.getUserId(), 35000, "Pendiente"))));
        var body = (Map<?, ?>) ((GeneralResponse) service.getCommissionReconciliation().getBody()).data();
        var rows = (List<CommissionReconciliationDto>) body.get("content");
        assertEquals(1, body.get("discrepancyCount"));
        assertEquals(245000L, rows.getFirst().walletPendingApproval());
        assertEquals(35000, rows.getFirst().ledgerPendingApproval());
        assertEquals(210000L, rows.getFirst().difference());
        assertEquals(245000, user.getWallet().getOutstandingBalance());
        verify(users, never()).save(any());
        verify(users, never()).saveAll(any());
        verify(transactions, never()).save(any());
    }

    @Test void reconciliationDoesNotFlagBalancedWallets() {
        authorize();
        var user = new UserModel("", null, null, new WalletModel(35000, 35000, 0, 0), null);
        user.setUserId(new org.bson.types.ObjectId());
        when(users.findAll()).thenReturn(List.of(user));
        when(transactions.findAll()).thenReturn(List.of(transaction("Pendiente", commission(user.getUserId(), 35000, "Pendiente"))));
        var body = (Map<?, ?>) ((GeneralResponse) service.getCommissionReconciliation().getBody()).data();
        assertEquals(0, body.get("discrepancyCount"));
    }

    @Test void consolidatedMoneyfyersAcceptMongoLongTotalsWithoutOverflow() {
        authorize();
        var user = new UserModel("", null, null, new WalletModel(0, 0, 0, 0), null);
        user.setUserId(new org.bson.types.ObjectId());
        when(users.findAll()).thenReturn(List.of(user));
        var metrics = new org.bson.Document("_id", user.getUserId())
                .append("realizedCommissions", 12).append("pendingPayments", 3000000000L)
                .append("ownCommissions", 3000000000L).append("referredCommissions", 2000000000L)
                .append("paidCommissions", 1000000000L).append("pendingApprovalCommissions", 4000000000L)
                .append("conflictCommissions", 1000000000L);
        when(mongo.aggregate(any(org.springframework.data.mongodb.core.aggregation.Aggregation.class),
                eq("transactions"), eq(org.bson.Document.class)))
                .thenReturn(new org.springframework.data.mongodb.core.aggregation.AggregationResults<>(
                        List.of(metrics), new org.bson.Document()));
        var result = ((MoneyfyersResponseDto) service.getMoneyfyersDashboard().getBody()).getData().getFirst();
        assertEquals(5000000000L, result.getTotalCommissions());
        assertEquals(3000000000L, result.getPendingPayments());
        assertEquals(4000000000L, result.getPendingApprovalCommissions());
    }
}
