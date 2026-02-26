package com.referidos.app.segurosref.requests;

import java.util.List;

import com.referidos.app.segurosref.dtos.commission.CommissionPaymentDto;

public record CommissionPaymentRequest(
    String key,
    List<CommissionPaymentDto> payments
) {

}
