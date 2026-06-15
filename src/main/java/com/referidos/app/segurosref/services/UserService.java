package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;

import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;

public interface UserService {

    // SERVICIOS PARA FLUJOS RELACIONADOS AL USUARIO
    ResponseEntity<?> update(UserUpdateRequest user, String emailAuth);

    ResponseEntity<?> changePassword(ChangePwdRequest changePwd, String emailAuth);

    ResponseEntity<GeneralResponse> hydrationData(String emailAuth, String updateCredential, String device);

    ResponseEntity<GeneralResponse> listReferreds(String emailAuth, String updateCredential, String device);

    ResponseEntity<GeneralResponse> obtainCommissions(String emailAuth);

    ResponseEntity<GeneralResponse> obtainPayments(String emailAuth);

    ResponseEntity<GeneralResponse> monthlyEarnings(String emailAuth);

    // SERVICIOS DE VALIDACIONES DE DATOS
    void validateSimpleUser(UserRegisterRequest user, Errors errors);

    void validateSave(UserRegisterRequest user, Errors errors);

    void validateUpdate(UserUpdateRequest user, BindingHelper bindingHelper);

    void validatePasswordChanged(ChangePwdRequest changePwd, Errors errors);

}
