package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.requests.VehicleBrandRequest;
import com.referidos.app.segurosref.requests.CommissionPaymentRequest;
import com.referidos.app.segurosref.requests.CommissionReportRequest;
import com.referidos.app.segurosref.requests.FinalizeQuoteRequest;
import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.requests.RegisterInsurerRequest;
import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;

public interface QuoterService {

    // SERVICIOS PARA INGRESAR O BUSCAR DATA RELACIONADA A LA MARCA/MODELOS DE UN VEHÍCULO PARA REALIZAR LAS COTIZACIONES
    ResponseEntity<?> registerVehicleBrands(VehicleBrandRequest vehicleBrands);
    ResponseEntity<?> searchVehicleBrands(String emailAuth);

    // SERVICIOS PARA INGRESAR O BUSCAR ASEGURADORAS QUE PROVEEN DE LOS PLANES PARA REALIZAR LAS COTIZACIONES
    ResponseEntity<?> registerInsurer(RegisterInsurerRequest registerInsurer);
    ResponseEntity<?> searchInsurers(String emailAuth, String updateCredential, String device);

    // SERVICIOS QUE FORMAN PARTE DEL FLUJO COMPLETO DE LA COTIZACIÓN
    ResponseEntity<?> searchVehicle(SearchVehicleRequest searchVehicle, String emailAuth);
    ResponseEntity<?> searchPlan(SearchPlanRequest vehicleQuote, String emailAuth);
    ResponseEntity<?> selectPlan(SelectPlanRequest planSelected, String emailAuth);
    ResponseEntity<?> generateTransaction(GenerateTransactionRequest generateTransaction, String emailAuth, String requestEndpoint);
    ResponseEntity<?> finalizeQuote(FinalizeQuoteRequest finalizeQuote, String emailAuth, String requestEndpoint);

    // SERVICIOS QUE FORMAN PARTE DEL FLUJO DEL RETIRO DE DINERO DISPONIBLE DEL USUARIO
    ResponseEntity<?> commissionReport(CommissionReportRequest commissionReportRequest, String requestEndpoint);
    ResponseEntity<?> commissionPayments(CommissionPaymentRequest commissionPaymentRequest);

    // SERVICIOS UTILIZADOS PARA REALIZAR PRUEBAS Y LÓGICAS DE LA APLICACIÓN
    ResponseEntity<?> viewTestData();
    String testNovaFunctions();

    // SERVICIOS DE VALIDACIONES DE DATOS
    void validateVehicleFinder(SearchVehicleRequest searchVehicle, BindingResult bindingResult);
    void validatePlanFinder(SearchPlanRequest searchPlan, BindingResult bindingResult);
    void validateSelectedPlan(SelectPlanRequest selectPlan, BindingResult bindingResult);
    
}
