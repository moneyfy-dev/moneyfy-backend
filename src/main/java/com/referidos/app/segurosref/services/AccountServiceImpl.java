package com.referidos.app.segurosref.services;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.AccountRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.validators.AccountValidator;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountValidator validator;

    // SERVICIOS RELACIONADOS CON EL MANEJO DE LAS CUENTAS BANCARIAS DEL USUARIO
    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> create(AccountRequest account, String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<AccountModel> accounts = userDB.getAccounts();

        // Verificar que no exista una cuenta bancaria con el mismo rut, banco, tipo de cuenta y número de cuenta.
        // En caso de no existir, quitar la selección de la cuenta principal (solo se actualiza, si no existe error).
        String personalId = account.personalId();
        String holderName = account.holderName().trim(); // CON TRIM() INCLUIDO (permite saltos en línea)
        String alias = DataHelper.isNull(account.alias()) ? "" : account.alias().trim(); // CON TRIM() INCLUIDO (permite saltos en línea) - dato opcional
        String email = account.email();
        String bank = account.bank();
        String accountType = account.accountType();
        String accountNumber = account.accountNumber().trim(); // CON TRIM() INCLUIDO
        for(AccountModel accountDB : accounts) {
            if(accountDB.getPersonalId().equals(personalId) && accountDB.getBank().equals(bank) &&
                    accountDB.getAccountType().equals(accountType) && accountDB.getAccountNumber().equals(accountNumber)) {
                return ResponseHelper.locked("cuenta bancaria del usuario existente", null);
            }
            accountDB.setSelected(false);
        }

        // Nueva cuenta seleccionada por defecto
        LocalDateTime currenTime = LocalDateTime.now();
        AccountModel newAccount = new AccountModel(new ObjectId(), personalId, holderName, alias, email, bank,
                accountType, accountNumber, true, currenTime, currenTime);
        userDB.addAccount(newAccount);
        // (IMPORTANTE: se obtienen los valores por referencia, asi que solo hay que actualizar el usuario)
        userDB = userRepository.save(userDB);

        return ResponseHelper.created(
                "la cuenta bancaria del usuario fue creada exitosamente",
                DataHelper.buildUser(userDB));
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> update(AccountRequest account, String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<AccountModel> accounts = userDB.getAccounts();
        for(AccountModel accountDB : accounts) {
            if(account.accountId().equals(accountDB.getAccountId())) {
                accountDB.setPersonalId(account.personalId());
                accountDB.setHolderName(account.holderName().trim()); // CON TRIM() INCLUIDO (permite saltos en línea)
                accountDB.setEmail(account.email());
                accountDB.setBank(account.bank());
                accountDB.setAccountType(account.accountType());
                accountDB.setAccountNumber(account.accountNumber().trim()); // CON TRIM() INCLUIDO (no permite saltos en línea)

                String alias = DataHelper.isNull(account.alias()) ? "" : account.alias().trim(); // CON TRIM() INCLUIDO (permite saltos en línea) - dato opcional
                accountDB.setAlias(alias);
                accountDB.setUpdatedDate(LocalDateTime.now());
                userDB = userRepository.save(userDB);

                return ResponseHelper.ok(
                        "la cuenta bancaria del usuario fue actualizada exitosamente",
                        DataHelper.buildUser(userDB));
            }
        }
        return ResponseHelper.failedDependency("no se ha podido identificar el recurso", null);
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> delete(String accountId, String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<AccountModel> accounts = userDB.getAccounts();
        for(int i=0; i<accounts.size(); i++) {
            AccountModel accountDB = accounts.get(i);
            if(accountId.equals(accountDB.getAccountId())) {
                if(accountDB.isSelected()) {
                    return ResponseHelper.locked(
                            "no se puede eliminar la cuenta que está seleccionada para recibir las comisiones",
                            null);
                }
                accounts.remove(i);
                userDB = userRepository.save(userDB);
                return ResponseHelper.ok(
                        "la cuenta bancaria del usuario fue eliminada exitosamente",
                        DataHelper.buildUser(userDB));
            }
        }
        return ResponseHelper.failedDependency("no se ha podido identificar el recurso", null);
    }

    @Transactional
    @Override
    public ResponseEntity<GeneralResponses> select(String accountId, String emailAuth) {
        UserModel userDB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        List<AccountModel> accounts = userDB.getAccounts();
        boolean found = false;

        for(AccountModel accountDB : accounts) {
            if(accountDB.getAccountId().equals(accountId)) {
                if(accountDB.isSelected()) {
                    return ResponseHelper.imUsed(
                            "la cuenta bancaria del usuario ya se encuentra seleccionada",
                            DataHelper.buildUser(userDB));
                }
                accountDB.setSelected(true);
                accountDB.setUpdatedDate(LocalDateTime.now());
                found = true; // No sale del bucle, ya que tiene que dejar las otras cuentas en => false
            } else {
                accountDB.setSelected(false);
                accountDB.setUpdatedDate(LocalDateTime.now());
            }
        }

        if(found) {
            userDB = userRepository.save(userDB);
            return ResponseHelper.ok(
                    "la cuenta bancaria del usuario ha sido seleccionada exitosamente",
                    DataHelper.buildUser(userDB));
        }

        return ResponseHelper.failedDependency("no se ha podido identificar el recurso", null);
    }

    // SERVICIO PARA VALDACION DE DATOS
    @Override
    public void validate(AccountRequest account, BindingResult bindingResult, boolean newAccount) {
        if(newAccount) {
            validator.validate(account, bindingResult);
        } else {
            validator.validateUpdate(account, bindingResult);
        }
    }

}
