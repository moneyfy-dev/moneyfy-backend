package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.requests.GenerateTransactionRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;

import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;

public interface QuoterService {

    // Servicio para buscar marcas/modelos registrados
    ResponseEntity<?> searchVehicleBrands(String emailAuth);

    // Servicio para buscar aseguradoras registrados
    ResponseEntity<?> searchInsurers(String emailAuth);

    // Servicios que forman parte del flujo completo de la cotización
    ResponseEntity<?> searchVehicle(SearchVehicleRequest searchVehicle, String emailAuth);

    ResponseEntity<?> searchPlan(SearchPlanRequest vehicleQuote, String emailAuth);

    ResponseEntity<?> selectPlan(SelectPlanRequest planSelected, String emailAuth);

    ResponseEntity<?> generateTransaction(GenerateTransactionRequest generateTransaction, String emailAuth);

    // Servicios para validaciones de datos
    void validateVehicleFinder(SearchVehicleRequest searchVehicle, BindingResult bindingResult);

    void validatePlanFinder(SearchPlanRequest searchPlan, BindingResult bindingResult);

    void validateSelectedPlan(SelectPlanRequest selectPlan, BindingResult bindingResult);

}
