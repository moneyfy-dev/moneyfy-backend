package com.referidos.app.segurosref.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;

@Component
public class UserValidator implements Validator {

    @Autowired
    private ValidateInputHelper validateInput;

    @SuppressWarnings("null")
    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(UserRegisterRequest.class) ||
                clazz.isAssignableFrom(UserUpdateRequest.class) ||
                clazz.isAssignableFrom(ChangePwdRequest.class);
    }

    @SuppressWarnings("null")
    @Override
    public void validate(Object target, Errors errors) {
        UserRegisterRequest user = (UserRegisterRequest) target;
        this.validateRegister(user, errors);

        String profileRole = this.validateInput.verifyProfileRole(user.profileRole());
        if(!profileRole.equals("")) {
            errors.rejectValue("profileRole", null, profileRole);
        }
    }

    public void validateRegister(Object target, Errors errors) {
        UserRegisterRequest user = (UserRegisterRequest) target;
        
        String name = this.validateInput.verifyName(user.name()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        String surname = this.validateInput.verifySurname(user.surname()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        String email = this.validateInput.verifyEmail(user.email());
        String pwd = this.validateInput.verifyPwd(user.pwd());
        
        this.verifyData(name, surname, email, pwd, errors);
    }

    public void validateUpdate(Object target, BindingHelper bindingHelper) {
        UserUpdateRequest user = (UserUpdateRequest) target;
        
        String name = this.validateInput.verifyName(user.name()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        String surname = this.validateInput.verifySurname(user.surname()); // CON TRIM() INCLUIDO (No permite saltos en línea)
        String phone = this.validateInput.verifyPhoneOptional(user.phone());
        String address = this.validateInput.verifyAddressOptional(user.address()); // CON TRIM() INCLUIDO (permite saltos de línea) - dato opcional
        String dateOfBirth = this.validateInput.verifyDateOfBirthOptional(user.dateOfBirth());
        String profilePicture = this.validateInput.verifyProfilePictureOptional(user.profilePicture());

        if(!name.equals("")) {
            bindingHelper.addError("name", name);
        }
        if(!surname.equals("")) {
            bindingHelper.addError("surname", surname);
        }
        if(!phone.equals("")) {
            bindingHelper.addError("phone", phone);
        }
        if(!address.equals("")) {
            bindingHelper.addError("address", address);
        }
        if(!dateOfBirth.equals("")) {
            bindingHelper.addError("dateOfBirth", dateOfBirth);
        }
        if(!profilePicture.equals("")) {
            bindingHelper.addError("profilePicture", profilePicture);
        }
        bindingHelper.validateData();
    }

    @SuppressWarnings("null")
    public void validatePasswordChanged(Object target, Errors errors) {
        ChangePwdRequest changePwd = (ChangePwdRequest) target;
        
        // Variables que contienen los mensajes de error
        String oldPwd = (DataHelper.isNull(changePwd.oldPwd())) ? "is required" : "";
        String newPwd = validateInput.verifyPwd(changePwd.newPwd());

        if(!oldPwd.equals("")) {
            errors.rejectValue("oldPwd", null, oldPwd);
        }
        if(!newPwd.equals("")) {
            errors.rejectValue("newPwd", null, newPwd);
        }
        if(newPwd.equals("") && oldPwd.equals("")) {
            // Si ambos campos no tienen mensaje de error, o sea son vacíos, es porque tienen almacenado un valor correcto,
            // pero, ahora, abordamos el caso de que sean iguales, porque no tiene caso cambiar la contraseña, si va a ser
            // la misma.
            if( changePwd.newPwd().equals(changePwd.oldPwd()) ) {
                errors.rejectValue("newPwd", null, "needs to be different from the oldPwd");
            }
        }

    }

    @SuppressWarnings("null")
    private void verifyData(String name, String surname, String email, String pwd, Errors errors) {
        if(!name.equals("")) {
            errors.rejectValue("name", null, name);
        }
        if(!surname.equals("")) {
            errors.rejectValue("surname", null, surname);
        }
        if(!email.equals("")) {
            errors.rejectValue("email", null, email);
        }
        if(!pwd.equals("")) {
            errors.rejectValue("pwd", null, pwd);
        }
    }

}
