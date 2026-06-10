package com.referidos.app.segurosref.dtos.manager;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    private String message;
    private int status;
    private List<DashboardQuoteDto> data;
}
