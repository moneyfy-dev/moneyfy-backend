package com.referidos.app.segurosref.services;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;

import com.referidos.app.segurosref.dtos.UserSimpleDto;
import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.SeedDefaultRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;

public interface UserService {

    // SERVICIOS PARA FLUJOS RELACIONADOS AL USUARIO
    ResponseEntity<?> update(UserUpdateRequest user, String emailAuth);
    ResponseEntity<?> changePassword(ChangePwdRequest changePwd, String emailAuth);
    ResponseEntity<GeneralResponses> hydrationData(String emailAuth, String updateCredential, String device);
    ResponseEntity<GeneralResponses> listReferreds(String emailAuth, String updateCredential, String device);
    ResponseEntity<GeneralResponses> obtainCommissions(String emailAuth);
    ResponseEntity<GeneralResponses> obtainPayments(String emailAuth);
    ResponseEntity<GeneralResponses> monthlyEarnings(String emailAuth);
    
    // SERVICIO PARA ALMACENAR O ACTUALIZAR LA DATA POR DEFECTO
    ResponseEntity<GeneralResponses> seedDefault(SeedDefaultRequest seedDefault);
    
    // SERVICIOS DE VALIDACIONES DE DATOS
    void validateRegister(UserRegisterRequest user, Errors errors);
    void validateSave(UserRegisterRequest user, Errors errors);
    void validateUpdate(UserUpdateRequest user, BindingHelper bindingHelper);
    void validatePasswordChanged(ChangePwdRequest changePwd, Errors errors);

    // SERVICIOS SUPUESTOS PARA ADMINISTRADORES QUE NO SE ESTÁN UTILIZANDO AÚN
    List<UserSimpleDto> findAll();
    ResponseEntity<?> findById(ObjectId userId);

}
