package com.referidos.app.segurosref.services;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.requests.AccountRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;

public interface AccountService {

    // SERVICIOS RELACIONADOS CON EL MANEJO DE LAS CUENTAS BANCARIAS DEL USUARIO
    ResponseEntity<GeneralResponses> create(AccountRequest account, String emailAuth);
    ResponseEntity<GeneralResponses> update(AccountRequest account, String emailAuth);
    ResponseEntity<GeneralResponses> delete(String accountId, String emailAuth);
    ResponseEntity<GeneralResponses> select(String accountId, String emailAuth);

    // SERVICIO PARA VALDACION DE DATOS
    void validate(AccountRequest account, BindingResult bindingResult, boolean newAccount);

}
