package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.requests.AccountRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;

public interface AccountService {

    // SERVICIOS RELACIONADOS CON EL MANEJO DE LAS CUENTAS BANCARIAS DEL USUARIO
    ResponseEntity<GeneralResponse> create(AccountRequest account, String emailAuth);
    ResponseEntity<GeneralResponse> update(AccountRequest account, String emailAuth);
    ResponseEntity<GeneralResponse> delete(String accountId, String emailAuth);
    ResponseEntity<GeneralResponse> select(String accountId, String emailAuth);

    // SERVICIO PARA VALDACION DE DATOS
    void validate(AccountRequest account, BindingResult bindingResult, boolean newAccount);

}
