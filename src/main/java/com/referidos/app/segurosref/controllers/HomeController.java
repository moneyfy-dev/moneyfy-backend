package com.referidos.app.segurosref.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "Home",
    description = "Controller to know the server state"
)
public class HomeController {

    // ENDPOINT PARA MOSTRAR RÁPIDAMENTE SI EL PROYECTO SE ESTÁ EJECUTANDO
    @GetMapping(value="/")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Server data",
        description = "Have a quick report about the server state",
        tags = {"Home"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Server working"
            )
        }
    )
    public ResponseEntity<String> init() {
        StringBuilder sb = new StringBuilder("<h2 style=\"font-size: 30px; margin: 20px 10px;\"><strong>Project Started...</strong></h2>");
        int status = HttpStatus.OK.value();
        sb.append("<div style=\"padding: 0px 10px\"><p style=\"margin: 0px 0px 5px 0px\"><b>Status code: </b>").append(status).append("</p>");
        sb.append("<p style=\"margin: 0px 0px 5px 0px\"><b>Common endpoints: </b></p>");
        sb.append("<div><p style=\"margin: 0px 0px 5px 0px\">url=\"https://api.moneyfy.cl/\", method=GET</p>");
        sb.append("<p style=\"margin: 0px 0px 5px 0px\">url=\"https://api.moneyfy.cl/docs\", method=GET</p>");
        sb.append("<p style=\"margin: 0px 0px 5px 0px\">url=\"https://api.moneyfy.cl/login\", method=POST</p>");
        sb.append("<p style=\"margin: 0px 0px 5px 0px\">url=\"https://api.moneyfy.cl/create\", method=POST</p></div></div>");
        String html = sb.toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(html, headers, status);
    }

}
