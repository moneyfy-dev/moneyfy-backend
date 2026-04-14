package com.referidos.app.segurosref.requests;

import java.util.List;

import com.referidos.app.segurosref.dtos.report.ReportUserDto;

public record CommissionPaymentRequest(
    List<ReportUserDto> updateUsers
) {

}
