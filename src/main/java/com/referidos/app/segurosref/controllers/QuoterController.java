package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.requests.VehicleBrandRequest;
import com.referidos.app.segurosref.requests.CommissionPaymentRequest;
import com.referidos.app.segurosref.requests.CommissionReportRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.requests.RegisterInsurerRequest;
import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.QuoterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping(value="/quoter")
@PreAuthorize(value = "denyAll()")
@Tag (
    name = "Quoter Controller",
    description = "Controller to search the insurance that adjust your car the most"
)
public class QuoterController {

    @Autowired
    private QuoterService quoterService;

    // ENDPOINTS PARA INGRESAR O BUSCAR DATA RELACIONADA A LA MARCA/MODELO DE UN VEHÍCULO PARA REALIZAR LAS COTIZACIONES
    @PostMapping(value = "/register/vehicle/brands")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Register the vehicle brands to quote your vehicle",
        description = "Register the vehicle brands to quote your vehicle",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the array of the vehicle brands to register",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = VehicleBrandRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The vehicle brands were created successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> registerVehicleBrands(@RequestBody VehicleBrandRequest vehicleBrands) {
        return quoterService.registerVehicleBrands(vehicleBrands);
    }

    @GetMapping(value = "/search/vehicle/brands")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Search the available brands",
        description = "Search the available brands to quote",
        tags = {"Quoter Controller"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Available brands found",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> searchVehicleBrands(Authentication auth) {
        return quoterService.searchVehicleBrands(auth.getPrincipal().toString());
    }

    // ENDPOINTS PARA INGRESAR O BUSCAR ASEGURADORAS QUE PROVEEN DE LOS PLANES PARA REALIZAR LAS COTIZACIONES
    @PostMapping(value = "/register/insurer")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Register a insurer",
        description = "Register a insurer that provides the insurances",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the data to register the insurer",
            required = true,
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = @Schema(implementation = RegisterInsurerRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The insurer was created successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> registerInsurer(@ModelAttribute RegisterInsurerRequest registerInsurer) {
        return quoterService.registerInsurer(registerInsurer);
    }

    @GetMapping(value = "/search/insurers")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Search the available insurers",
        description = "Search the available insurers to start the quote",
        tags = {"Quoter Controller"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Available insurers found",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> searchInsurers(Authentication auth, HttpServletRequest request) {
        return quoterService.searchInsurers(auth.getPrincipal().toString(), auth.getCredentials().toString(), request.getHeader("User-Agent"));
    }

    // ENDPOINTS QUE FORMAN PARTE DEL FLUJO COMPLETO DE LA COTIZACIÓN
    @PostMapping(value = "/search/vehicle")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Search a vehicle",
        description = "Search a vehicle by its license plate and owner id (rut)",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the license plate and the owner id (rut)",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = SearchVehicleRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The vehicle was encountered successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> searchVehicle(@RequestBody SearchVehicleRequest searchVehicle, BindingResult bindingResult,
            Authentication auth) {
        quoterService.validateVehicleFinder(searchVehicle, bindingResult);
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return quoterService.searchVehicle(searchVehicle, auth.getPrincipal().toString());
    }

    @PostMapping(value = "/search/plan")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Search a plan to quote a vehicle",
        description = "Search a plan to quote a vehicle and find the best insurance",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the vehicle data and the purchaser id and if he's the owner",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = SearchPlanRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "There were plans encountered for the vehicle",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> searchPlan(@RequestBody SearchPlanRequest searchPlan, BindingResult bindingResult,
            Authentication auth) {
        quoterService.validatePlanFinder(searchPlan, bindingResult); // VALIDACIÓN ACTUALIZADA
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return quoterService.searchPlan(searchPlan, auth.getPrincipal().toString());
    }

    @PutMapping(value = "/select/plan")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Select a plan provided",
        description = "Select a plan provided for the vehicle quote",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the plan and address data for the quote",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = SelectPlanRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The quote was updated successfully with the plan provided",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> selectPlan(@RequestBody SelectPlanRequest selectPlan, BindingResult bindingResult,
            Authentication auth) {
        quoterService.validateSelectedPlan(selectPlan, bindingResult);
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return quoterService.selectPlan(selectPlan, auth.getPrincipal().toString());
    }

    @PutMapping(value = "/generate/transaction")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Confirm payment for the user quoter",
        description = "Confirm payment for the user quoter and update the wallet",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the data to generate the transaction",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = GenerateTransactionRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The transaction was generated successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> generateTransaction(@RequestBody GenerateTransactionRequest generateTransaction, Authentication auth, HttpServletRequest request) {
        return quoterService.generateTransaction(generateTransaction, auth.getPrincipal().toString(), request.getRequestURI());
    }

    @PutMapping(value = "/finalize/quote")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Finalize a quote that is Pending",
        description = "Finalize a quote that is Pending and it needs to be ended",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the data to finalize the quote",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = FinalizeQuoteRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The user's quote was finalized successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<?> finalizeQuote(@RequestBody FinalizeQuoteRequest finalizeQuote, Authentication auth, HttpServletRequest request) {
        return quoterService.finalizeQuote(finalizeQuote, auth.getPrincipal().toString(), request.getRequestURI());
    }

    // ENDPOINTS QUE FORMAN PARTE DEL FLUJO DEL RETIRO DE DINERO DISPONIBLE DEL USUARIO
    @PostMapping(value = "/commission/report")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Generate the commission report",
        description = "Generate the commission report for the users that have available money",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the data to authorize the commission report",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = CommissionReportRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The commission report was generated successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> commissionReport(@RequestBody CommissionReportRequest commissionReportRequest, HttpServletRequest request) {
        return quoterService.commissionReport(commissionReportRequest, request.getRequestURI());
    }

    @PostMapping(value = "/commission/payments")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Update the commissions that were paid",
        description = "Update the commissions that were paid",
        tags = {"Quoter Controller"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Provide the data to update the commissions that were paid",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = CommissionPaymentRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The commissions were updated successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> commissionPayments(@RequestBody CommissionPaymentRequest commissionPaymentRequest) {
        return quoterService.commissionPayments(commissionPaymentRequest);
    }

    // ENDPOINTS UTILIZADOS PARA REALIZAR PRUEBAS Y LÓGICAS DE LA APLICACIÓN
    @GetMapping(value = "/view/test/data")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "View the test data",
        description = "View the test data to make the differents tests",
        tags = {"Quoter Controller"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "There was test data encountered",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<?> viewTestData() {
        return quoterService.viewTestData();
    }

    @PostMapping(value = "/test/nova/functions")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Test nova functions",
        description = "Test nova functions",
        tags = {"Quoter Controller"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The function has finished successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public String testNovaFunctions() {
        return quoterService.testNovaFunctions();
    }

}
