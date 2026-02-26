package com.referidos.app.segurosref.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.requests.SearchVehicleRequest;
import com.referidos.app.segurosref.requests.SelectPlanRequest;
import com.referidos.app.segurosref.requests.SearchPlanRequest;

@Component
public class QuoterValidator implements Validator {

    @Autowired
    private ValidateInputHelper validateInput;

    @SuppressWarnings("null")
    @Override
    public boolean supports(Class<?> clazz) {
        // Lo que se debería colocar es un objeto que se va utilizar para las validaciones, no específicamente el modelo
        return  clazz.isAssignableFrom(SearchVehicleRequest.class) ||
                clazz.isAssignableFrom(SearchPlanRequest.class) ||
                clazz.isAssignableFrom(SelectPlanRequest.class);
    }

    @SuppressWarnings("null")
    @Override
    public void validate(Object target, Errors errors) {
        SearchVehicleRequest searchVehicle = (SearchVehicleRequest) target;
        
        String ppu = this.validateInput.verifyPpu(searchVehicle.ppu());
        String ownerId = this.validateInput.verifyPersonalId(searchVehicle.ownerId());

        if(!ppu.equals("")) {
            errors.rejectValue("ppu", null, ppu);
        }
        if(!ownerId.equals("")) {
            errors.rejectValue("ownerId", null, ownerId);
        }
    }

    public void validatePlanFinder(SearchPlanRequest searchPlan, BindingResult bindingResult) {
        String quoterId = this.validateInput.verifyQuoterIdOptional(searchPlan.quoterId());
        String ppu = this.validateInput.verifyPpu(searchPlan.ppu());
        String brand = this.validateInput.verifyBrand(searchPlan.brand());
        String model = this.validateInput.verifyModel(searchPlan.model());
        String year = this.validateInput.verifyYear(searchPlan.year());
        String insurerAlias = this.validateInput.verifyInsurerAlias(searchPlan.insurerAlias()); // CON TRIM() INCLUIDO (permite saltos en línea)
        String requestType = this.validateInput.verifyRequestTypeForSearchPlan(searchPlan.requestType());
        String purchaserId = this.validateInput.verifyPersonalId(searchPlan.purchaserId());
        String purchaserName = this.validateInput.verifyName(searchPlan.purchaserName()); // CON TRIM() INCLUIDO (no permite saltos en línea)
        String purchaserPaternalSur = this.validateInput.verifySurname(searchPlan.purchaserPaternalSur()); // CON TRIM() INCLUIDO (no permite saltos en línea)
        String purchaserMaternalSur = this.validateInput.verifySurname(searchPlan.purchaserMaternalSur()); // CON TRIM() INCLUIDO (no permite saltos en línea)
        String purchaserEmail = this.validateInput.verifyEmail(searchPlan.purchaserEmail());
        String purchaserPhone = this.validateInput.verifyPhoneOptional(searchPlan.purchaserPhone()); // Opcional
        String ownerRelationOption = this.validateInput.verifyOwnerOption(searchPlan.ownerRelationOption());
        
        this.verifyPlanFinderData(quoterId, ppu, brand, model, year, insurerAlias, requestType, purchaserId, purchaserName,
                purchaserPaternalSur, purchaserMaternalSur, purchaserEmail, purchaserPhone, ownerRelationOption, bindingResult);
    }

    public void validateSelectedPlan(SelectPlanRequest selectPlan, BindingResult bindingResult) {
        // Datos del plan
        String quoterId = this.validateInput.verifyQuoterId(selectPlan.quoterId());
        String planId = this.validateInput.verifyPlanId(selectPlan.planId());
        String insurer = this.validateInput.verifyInsurer(selectPlan.insurer()); // CON TRIM() INCLUIDO (permite saltos en línea)
        String planName = this.validateInput.verifyPlanName(selectPlan.planName()); // CON TRIM() INCLUIDO (permite saltos en línea)
        String valueUF = this.validateInput.verifyNumberValue(selectPlan.valueUF());
        String grossPriceUF = this.validateInput.verifyNumberValue(selectPlan.grossPriceUF());
        String totalMonths = this.validateInput.verifyNumberValue(selectPlan.totalMonths());
        String monthlyPriceUF = this.validateInput.verifyNumberValue(selectPlan.monthlyPriceUF());
        String monthlyPrice = this.validateInput.verifyNumberValue(selectPlan.monthlyPrice());
        String deductible = this.validateInput.verifyNumberValue(selectPlan.deductible());
        String discount = this.validateInput.verifyNumberValue(selectPlan.discount());
        // Datos del dueño del vehículo
        String ownerName = this.validateInput.verifyName(selectPlan.ownerName()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        String ownerPaternalSur = this.validateInput.verifySurname(selectPlan.ownerPaternalSur()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        String ownerMaternalSur = this.validateInput.verifySurname(selectPlan.ownerMaternalSur()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        // Datos para la inspección
        String street = this.validateInput.verifyStreet(selectPlan.street()); // CON TRIM() INCLUIDO (permite saltos en línea)
        String streetNumber = this.validateInput.verifyStreetNumber(selectPlan.streetNumber()); // CON TRIM() INCLUIDO (permite saltos en línea)
        String department = this.validateInput.verifyDepartment(selectPlan.department()); // CON TRIM() INCLUIDO (permite saltos en línea) - opcional
        // Validar siguientes campos y verificar si tienen error los demás campos
        this.validatePlanData(quoterId, planId, insurer, planName, valueUF, grossPriceUF, totalMonths, monthlyPriceUF,
                monthlyPrice, deductible, discount, ownerName, ownerPaternalSur, ownerMaternalSur, street, streetNumber,
                department, bindingResult);
    }

    // Verificar si existen errores en caso de haberlos, se asignan al objeto vinculante
    @SuppressWarnings("null")
    private void verifyPlanFinderData(String quoterId, String ppu, String brand, String model, String year, String insurerAlias,
            String requestType, String purchaserId, String purchaserName, String purchaserPaternalSur,
            String purchaserMaternalSur, String purchaserEmail, String purchaserPhone, String ownerRelationOption,
            Errors errors) {
        if(!quoterId.equals("")) {
            errors.rejectValue("quoterId", null, quoterId);
        }
        if(!ppu.equals("")) {
            errors.rejectValue("ppu", null, ppu);
        }
        if(!brand.equals("")) {
            errors.rejectValue("brand", null, brand);
        }
        if(!model.equals("")) {
            errors.rejectValue("model", null, model);
        }
        if(!year.equals("")) {
            errors.rejectValue("year", null, year);
        }
        if(!insurerAlias.equals("")) {
            errors.rejectValue("insurerAlias", null, insurerAlias);
        }
        if(!requestType.equals("")) {
            errors.rejectValue("requestType", null, requestType);
        }
        if(!purchaserId.equals("")) {
            errors.rejectValue("purchaserId", null, purchaserId);
        }
        if(!purchaserName.equals("")) {
            errors.rejectValue("purchaserName", null, purchaserName);
        }
        if(!purchaserPaternalSur.equals("")) {
            errors.rejectValue("purchaserPaternalSur", null, purchaserPaternalSur);
        }
        if(!purchaserMaternalSur.equals("")) {
            errors.rejectValue("purchaserMaternalSur", null, purchaserMaternalSur);
        }
        if(!purchaserEmail.equals("")) {
            errors.rejectValue("purchaserEmail", null, purchaserEmail);
        }
        if(!purchaserPhone.equals("")) {
            errors.rejectValue("purchaserPhone", null, purchaserPhone);
        }
        if(!ownerRelationOption.equals("")) {
            errors.rejectValue("ownerRelationOption", null, ownerRelationOption);
        }
    }
    
    @SuppressWarnings("null")
    private void validatePlanData(String quoterId, String planId, String insurer, String planName, String valueUF,
            String grossPriceUF, String totalMonths, String monthlyPriceUF, String monthlyPrice, String deductible,
            String discount, String ownerName, String ownerPaternalSur, String ownerMaternalSur, String street,
            String streetNumber, String department, BindingResult errors) {
        if(!quoterId.equals("")) {
            errors.rejectValue("quoterId", null, quoterId);
        }
        if(!planId.equals("")) {
            errors.rejectValue("planId", null, planId);
        }
        if(!insurer.equals("")) {
            errors.rejectValue("insurer", null, insurer);
        }
        if(!planName.equals("")) {
            errors.rejectValue("planName", null, planName);
        }
        if(!valueUF.equals("")) {
            errors.rejectValue("valueUF", null, valueUF);
        }
        if(!grossPriceUF.equals("")) {
            errors.rejectValue("grossPriceUF", null, grossPriceUF);
        }
        if(!totalMonths.equals("")) {
            errors.rejectValue("totalMonths", null, totalMonths);
        }
        if(!monthlyPriceUF.equals("")) {
            errors.rejectValue("monthlyPriceUF", null, monthlyPriceUF);
        }
        if(!monthlyPrice.equals("")) {
            errors.rejectValue("monthlyPrice", null, monthlyPrice);
        }
        if(!deductible.equals("")) {
            errors.rejectValue("deductible", null, deductible);
        }
        if(!discount.equals("")) {
            errors.rejectValue("discount", null, discount);
        }
        if(!ownerName.equals("")) {
            errors.rejectValue("ownerName", null, ownerName);
        }
        if(!ownerPaternalSur.equals("")) {
            errors.rejectValue("ownerPaternalSur", null, ownerPaternalSur);
        }
        if(!ownerMaternalSur.equals("")) {
            errors.rejectValue("ownerMaternalSur", null, ownerMaternalSur);
        }
        if(!street.equals("")) {
            errors.rejectValue("street", null, street);
        }
        if(!streetNumber.equals("")) {
            errors.rejectValue("streetNumber", null, streetNumber);
        }
        if(!department.equals("")) {
            errors.rejectValue("department", null, department);
        }
    }

}
