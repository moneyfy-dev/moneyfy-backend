package com.referidos.app.segurosref.dtos.manager;

public record CommissionReconciliationDto(String userId, String userEmail, String userFullname,
        Long walletPendingApproval, long ledgerPendingApproval, Long difference, boolean walletMissing) {
}
