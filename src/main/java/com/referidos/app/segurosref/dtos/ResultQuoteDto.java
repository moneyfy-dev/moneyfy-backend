package com.referidos.app.segurosref.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.referidos.app.segurosref.models.InsurerModel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder(value = {"quoterId", "error", "errorMessage", "requestBody", "response", "insurer", "plans"})
public class ResultQuoteDto {

    private String quoterId;
    private String error;
    private String errorMessage;
    private String requestBody;
    private String response;
    private InsurerModel insurer;
    private List<TestPlanDto> plans;

}
