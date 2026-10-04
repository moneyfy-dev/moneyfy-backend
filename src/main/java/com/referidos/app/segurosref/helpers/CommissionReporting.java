package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.referidos.app.segurosref.dtos.manager.CommissionLedgerEntryDto;
import com.referidos.app.segurosref.dtos.manager.CommissionReconciliationDto;
import com.referidos.app.segurosref.models.QuoterModel;
import com.referidos.app.segurosref.models.TransactionModel;
import com.referidos.app.segurosref.models.UserModel;

/** Read-only reporting based on historical amounts and individual beneficiary states. */
public final class CommissionReporting {
    public static final Set<String> REPORTABLE_STATUSES = Set.of("Pendiente", "Aprobado", "Pagado", "Conflictivo");
    private CommissionReporting() {}

    public static boolean withinPeriod(LocalDateTime date, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (date == null) return false;
        LocalDate day = date.toLocalDate();
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    public static List<CommissionLedgerEntryDto> entries(List<TransactionModel> transactions,
            List<UserModel> users, LocalDate from, LocalDate to) {
        Map<String, UserModel> byUser = new HashMap<>();
        Map<String, QuoterModel> byQuote = new HashMap<>();
        for (UserModel user : users) {
            byUser.put(user.getUserId(), user);
            if (user.getQuoters() != null) {
                for (QuoterModel quote : user.getQuoters()) {
                    if (quote != null) byQuote.put(user.getUserId() + ":" + quote.getQuoterId(), quote);
                }
            }
        }
        List<CommissionLedgerEntryDto> result = new ArrayList<>();
        for (TransactionModel transaction : transactions) {
            if (!withinPeriod(transaction.getCreatedDate(), from, to) || transaction.getCommissions() == null) continue;
            QuoterModel quote = byQuote.get(transaction.getUserId() + ":" + transaction.getQuoterId());
            for (var commission : transaction.getCommissions()) {
                if (commission == null) continue;
                UserModel beneficiary = byUser.get(commission.getUserId());
                result.add(new CommissionLedgerEntryDto(transaction.getTransactionId(), transaction.getQuoterId(),
                        commission.getUserId(), email(beneficiary), fullname(beneficiary),
                        commission.getCommissionStatus(), commission.getUserCommission(), transaction.getCreatedDate(),
                        quote == null ? null : quote.getQuoterStatus(), quote == null));
            }
        }
        result.sort(Comparator.comparing(CommissionLedgerEntryDto::createdDate,
                Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CommissionLedgerEntryDto::transactionId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CommissionLedgerEntryDto::userId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public static long total(List<CommissionLedgerEntryDto> entries, String status) {
        return entries.stream().filter(entry -> status.equals(entry.status())).mapToLong(CommissionLedgerEntryDto::amount).sum();
    }

    public static List<CommissionReconciliationDto> reconcile(List<CommissionLedgerEntryDto> allEntries, List<UserModel> users) {
        Map<String, Long> pendingByUser = new HashMap<>();
        for (var entry : allEntries) {
            if ("Pendiente".equals(entry.status())) pendingByUser.merge(entry.userId(), entry.amount(), Long::sum);
        }
        List<CommissionReconciliationDto> discrepancies = new ArrayList<>();
        for (UserModel user : users) {
            long pending = pendingByUser.getOrDefault(user.getUserId(), 0L);
            pendingByUser.remove(user.getUserId());
            Long walletPending = user.getWallet() == null ? null : (long) user.getWallet().getOutstandingBalance();
            if (walletPending == null || walletPending != pending) {
                discrepancies.add(new CommissionReconciliationDto(user.getUserId(), email(user), fullname(user),
                        walletPending, pending, walletPending == null ? null : walletPending - pending, walletPending == null));
            }
        }
        pendingByUser.forEach((userId, pending) -> discrepancies.add(new CommissionReconciliationDto(
                userId, "", "Usuario no disponible", null, pending, null, true)));
        discrepancies.sort(Comparator.comparing(CommissionReconciliationDto::userId, Comparator.nullsLast(Comparator.naturalOrder())));
        return discrepancies;
    }

    private static String email(UserModel user) {
        return user == null || user.getPersonalData() == null || user.getPersonalData().getEmail() == null
                ? "" : user.getPersonalData().getEmail();
    }

    private static String fullname(UserModel user) {
        if (user == null || user.getPersonalData() == null) return "Usuario no disponible";
        var data = user.getPersonalData();
        String name = ((data.getName() == null ? "" : data.getName()) + " "
                + (data.getSurname() == null ? "" : data.getSurname())).trim();
        return name.isEmpty() ? "Usuario no disponible" : name;
    }
}
