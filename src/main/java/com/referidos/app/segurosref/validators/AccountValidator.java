package com.referidos.app.segurosref.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.requests.AccountRequest;

@Component
public class AccountValidator implements Validator {

    @Autowired
    private ValidateInputHelper validateInputHelper;

    @SuppressWarnings("null")
    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(AccountRequest.class);
    }

    @SuppressWarnings("null")
    @Override
    public void validate(Object target, Errors errors) {
        AccountRequest account = (AccountRequest) target;

        String personalId = this.validateInputHelper.verifyPersonalId(account.personalId());
        String holderName = this.validateInputHelper.verifyHolderName(account.holderName()); // CON TRIM() INCLUIDO (permite saltos en línea)
        String alias = this.validateInputHelper.verifyAliasOptional(account.alias()); // CON TRIM() INCLUIDO (permite saltos en línea) - dato opcional
        String email = this.validateInputHelper.verifyEmail(account.email());
        String bank = this.validateInputHelper.verifyBank(account.bank());
        String accountType = this.validateInputHelper.verifyAccountType(account.accountType());
        String accountNumber = this.validateInputHelper.verifyAccountNumber(account.accountNumber()); // CON TRIM() INCLUIDO (no permite saltos en línea)

        this.verifyData(personalId, holderName, alias, email, bank, accountType, accountNumber, errors);
    }

    @SuppressWarnings("null")
    public void validateUpdate(Object target, Errors errors) {
        AccountRequest account = (AccountRequest) target;
        this.validate(account, errors);
        
        String accountId = this.validateInputHelper.verifyAccountId(account.accountId());
        if(!accountId.equals("")) {
            errors.rejectValue("accountId", null, accountId);
        }
    }

    @SuppressWarnings("null")
    private void verifyData(String personalId, String holderName, String alias, String email, String bank,
            String accountType, String accountNumber, Errors errors) {
        if(!personalId.equals("")) {
            errors.rejectValue("personalId", null, personalId);
        }
        if(!holderName.equals("")) {
            errors.rejectValue("holderName", null, holderName);
        }
        if(!alias.equals("")) {
            errors.rejectValue("alias", null, alias);
        }
        if(!email.equals("")) {
            errors.rejectValue("email", null, email);
        }
        if(!bank.equals("")) {
            errors.rejectValue("bank", null, bank);
        }
        if(!accountType.equals("")) {
            errors.rejectValue("accountType", null, accountType);
        }
        if(!accountNumber.equals("")) {
            errors.rejectValue("accountNumber", null, accountNumber);
        }
    }

}
