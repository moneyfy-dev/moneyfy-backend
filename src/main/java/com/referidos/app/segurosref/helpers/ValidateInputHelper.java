package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// Clase para validar la lógica de todos los inputs de la aplicación
@Component
public class ValidateInputHelper {

    @Autowired
    private Environment env;

    // Validación del nombre del usuario - Obligatorio
    public String verifyName(String name) {
        if(DataHelper.isNull(name)) {
            return env.getProperty("message.field.null");
        }
        final int NAME_LENGTH = name.trim().length(); // CON TRIM() INCLUIDO (No permite saltos en línea)
        final String NAME_REGEX = "^[a-zA-ZáéíóúñçýÁÉÍÓÚÑÇÝ]+$";
        if(NAME_LENGTH < 2) {
            return env.getProperty("message.field.min.characters.2");
        } else if(NAME_LENGTH > 40) {
            return env.getProperty("message.field.max.characters.40");
        } else if(!name.trim().matches(NAME_REGEX)) { // CON TRIM() INCLUIDO (No permite saltos en línea)
            return env.getProperty("message.field.non.allow.characters");
        }
        return "";
    }

    // Validación del apellido del usuario - Obligatorio
    public String verifySurname(String surname) {
        if(DataHelper.isNull(surname)) {
            return env.getProperty("message.field.null");
        }
        final int SURNAME_LENGTH = surname.trim().length(); // CON TRIM() INCLUIDO (No permite saltos en línea)
        final String SURNAME_REGEX = "^[a-zA-ZáéíóúñçýÁÉÍÓÚÑÇÝ]+$";
        if(SURNAME_LENGTH < 2) {
            return env.getProperty("message.field.min.characters.2");
        } else if(SURNAME_LENGTH > 40) {
            return env.getProperty("message.field.max.characters.40");
        } else if(!surname.trim().matches(SURNAME_REGEX)) { // CON TRIM() INCLUIDO (No permite saltos en línea)
            return env.getProperty("message.field.non.allow.characters");
        }
        return "";
    }

    // Validación de email - Obligatorio
    public String verifyEmail(String email) {
        if(DataHelper.isNull(email)) {
            return env.getProperty("message.field.null");
        }
        final int EMAIL_LENGTH = email.length();
        final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@.]+\\.[^\\s@.]{2,}$";
        if (EMAIL_LENGTH > 50) {
            return env.getProperty("message.field.max.characters.50");
        } else if(!email.matches(EMAIL_REGEX)) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }
    
    // Validación de la contraseña del usuario - Obligatorio
    public String verifyPwd(String pwd) {
        if(DataHelper.isNull(pwd)) {
            return env.getProperty("message.field.null");
        }
        final int PASS_LENGTH = pwd.length();
        boolean pwdHasNumber = this.pwdHasNumber(pwd);
        boolean pwdHasLowerCase = this.pwdHasLowerCase(pwd);
        boolean pwdHasUpperCase = this.pwdHasUpperCase(pwd);
        // boolean pwdHasSpecialChar = this.pwdHasSpecialChar(pwd);
        if(PASS_LENGTH < 8) {
            return env.getProperty("message.field.min.characters.8");
        } else if(PASS_LENGTH > 40) {
            return env.getProperty("message.field.max.characters.40");
        // } else if(!pwdHasNumber || !pwdHasLowerCase || !pwdHasUpperCase || !pwdHasSpecialChar) {
        } else if(!pwdHasNumber || !pwdHasLowerCase || !pwdHasUpperCase) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }
    private boolean pwdHasNumber(String pwd) {
        String[] numbers = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        for(int i=0; i<pwd.length(); i++) {
            String character = Character.toString(pwd.charAt(i));
            for(String number : numbers) {
                if(character.equals(number)) {
                    return true;
                }
            }
        }        
        return false;
    }
    private boolean pwdHasLowerCase(String pwd) {
        String[] lowerCases = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
                "s", "t", "u", "v", "w", "x", "y", "z", "á", "é", "í", "ó", "ú", "ñ", "ç", "ý"};
        for(int i=0; i<pwd.length(); i++) {
            String character = Character.toString(pwd.charAt(i));
            for(String lowerCase : lowerCases) {
                if(character.equals(lowerCase)) {
                    return true;
                }
            }
        }  
        return false;
    }
    private boolean pwdHasUpperCase(String pwd) {
        String[] upperCases = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X", "Y", "Z", "Á", "É", "Í", "Ó", "Ú", "Ñ", "Ç", "Ý"};
        for(int i=0; i<pwd.length(); i++) {
            String character = Character.toString(pwd.charAt(i));
            for(String upperCase : upperCases) {
                if(character.equals(upperCase)) {
                    return true;
                }
            }
        }  
        return false;
    }
    // private boolean pwdHasSpecialChar(String pwd) {
    //     String[] specialChars = {"$", "%", "&", "#", "-", "_", "?"};
    //     for(int i=0; i<pwd.length(); i++) {
    //         String character = Character.toString(pwd.charAt(i));
    //         for(String specialChar : specialChars) {
    //             if(character.equals(specialChar)) {
    //                 return true;
    //             }
    //         }
    //     }  
    //     return false;
    // }

    // Validación del rol del usuario - Obligatorio
    public String verifyProfileRole(String profileRole) {
        if(DataHelper.isNull(profileRole)) {
            return env.getProperty("message.field.null");
        } else if(!profileRole.equalsIgnoreCase("ROLE_USER") && !profileRole.equalsIgnoreCase("ROLE_ADMIN")) {
            return env.getProperty("message.field.bad.format.roles");
        }
        return "";
    }

    // Validación del celular del usuario - Opcional
    public String verifyPhoneOptional(String phone) {
        final String PHONE_REGEX = "^\\+56+[0-9]{9}$";
        if(!DataHelper.isNull(phone) && !phone.matches(PHONE_REGEX)) {
            return env.getProperty("message.field.bad.format.phone");
        }
        return "";
    }

    // Validación de la dirección del usuario - Opcional
    public String verifyAddressOptional(String address) {
        if(!DataHelper.isNull(address)) {
            final int ADDRESS_LENGTH = address.trim().length(); // CON TRIM() INCLUIDO (permite saltos de línea) - dato opcional
            final String ADDRESS_REGEX = "^[a-zA-Z0-9áéíóúñçýÁÉÍÓÚÑÇÝ.,_/#\\s\\-]+$";
            if(ADDRESS_LENGTH < 8) {
                return env.getProperty("message.field.min.characters.8");
            } else if(ADDRESS_LENGTH > 50) {
                return env.getProperty("message.field.max.characters.50");
            } else if(!address.matches(ADDRESS_REGEX)) {
                return env.getProperty("message.field.non.allow.characters.address");
            }
        }
        return ""; 
    }

    // Validación de la fecha de nacimiento del usuario - Opcional
    public String verifyDateOfBirthOptional(String strDateOfBirth) {
        if(!DataHelper.isNull(strDateOfBirth)) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate dateOfBirth = LocalDate.parse(strDateOfBirth, formatter);
                long yearsDifference = ChronoUnit.YEARS.between(dateOfBirth, LocalDate.now()); // Comtempla el escenario de que este año, ya se cumplio o no el día de cumpleaños
                if(yearsDifference < 18) {
                    return env.getProperty("message.field.date.over.18");
                }
            } catch (DateTimeParseException e) {
                return env.getProperty("message.field.bad.format.date");
            }
        }
        return "";
    }

    // Validación de la foto perfil del usuario - Opcional
    public String verifyProfilePictureOptional(MultipartFile file) {
        if(file != null && !DataHelper.verifyImageFile(file)) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }

    // Validación de rut - Obligatorio
    public String verifyPersonalId(String personalId) {
        if(DataHelper.isNull(personalId)) {
            return env.getProperty("message.field.null");
        }
        if(!this.validatePersonalId(personalId)) {
            return env.getProperty("message.field.bad.format.personalId");
        }
        return "";
    }
    private boolean validatePersonalId(String id) {
        final String ID_REGEX = "^[0-9kK.\\-]{11,12}$"; // Posibles formatos de rut: XX.XXZ.XXZ-X - X.XXZ.XXZ-X
        if(!id.matches(ID_REGEX)) {
            return false;
        }
        return confirmPersonalId(id);
    }
    private boolean confirmPersonalId(String id) {
        String idClean = id.replaceAll("[.\\-]", "");
        String idValue = idClean.substring(0, idClean.length()-1);
        String dv = idClean.substring(idClean.length()-1);
        if(idValue.matches("^[0-9]{7,8}$") && dv.matches("^[0-9kK]{1}$")) {
            int sum=0;
            int coin=2;
            for(int i=idValue.length()-1; i>=0; i--) {
                int value = Integer.parseInt(idValue.substring(i, i+1)); // Error de conversión de char a integer, por eso se usa substring()
                sum += (value*coin);
                coin++; // se suma 1 al peso
                coin = (coin==8) ? 2 : coin;
            }
            String dvConfirm = String.valueOf(11 - (sum % 11));
            if(dvConfirm.equals("11") && dv.equals("0")) {
                return true;
            } else if(dvConfirm.equals("10") && dv.equalsIgnoreCase("k")) {
                return true;
            } else if(dvConfirm.equals(dv)) {
                return true;
            }
        }
        return false;
    }

    // Validación de un id de cuenta bancaria correcta en actualización de cuenta bancaria - Obligatorio
    public String verifyAccountId(String accountId) {
        if(DataHelper.isNull(accountId)) {
            return env.getProperty("message.field.null");
        }
        if(!ObjectId.isValid(accountId)) {
            return env.getProperty("message.field.bad.id");
        }
        return  "";
    }

    // Validación del nombre del titular en creación de cuenta bancaria - Obligatorio
    public String verifyHolderName(String holderName) {
        if(DataHelper.isNull(holderName)) {
            return env.getProperty("message.field.null");
        }
        final int HOLDER_NAME_LENGTH = holderName.trim().length(); // CON TRIM() INCLUIDO (permite saltos en línea)
        final String HOLDER_NAME_REGEX = "^[a-zA-ZáéíóúñçýÁÉÍÓÚÑÇÝ\\s]+$";
        if(HOLDER_NAME_LENGTH < 2) {
            return env.getProperty("message.field.min.characters.2");
        } else if(HOLDER_NAME_LENGTH > 40) {
            return env.getProperty("message.field.max.characters.40");
        } else if(!holderName.matches(HOLDER_NAME_REGEX)) {
            return env.getProperty("message.field.non.allow.characters.holder-name");
        }
        return "";
    }

    // Validación de alias en creación de cuenta bancaria - Opcional
    public String verifyAliasOptional(String alias) {
        if(!DataHelper.isNull(alias)) {
            final int ALIAS_LENGTH = alias.trim().length();  // CON TRIM() INCLUIDO (permite saltos en línea) - dato opcional
            if(ALIAS_LENGTH < 2) {
                return env.getProperty("message.field.min.characters.2");
            } else if(ALIAS_LENGTH > 40) {
                return env.getProperty("message.field.max.characters.40");
            }
        }
        return "";
    }

    // Validación de selección de banco en creación de cuenta bancaria - Obligatorio
    public String verifyBank(String bank) {
        if(DataHelper.isNull(bank)) {
            return env.getProperty("message.field.null");
        }
        // Lista de bancos
        final String[] BANKS = {"Banco Scotiabank", "Banco BBVA", "Banco Itau", "Banco BICE", "Banco HSBC",
                "Banco Consorcio", "Banco Corpbanca", "Banco BCI/Mach", "Banco Estado", "Banco Falabella",
                "Banco Internacional", "Banco Paris", "Banco Ripley", "Banco Santander", "Banco Security",
                "Banco Chile", "Banco del Desarrollo", "Banco Brasil", "Banco Rabobank", "Banco J.P. Morgan Chase",
                "Transbank", "Coopeuch / Dale", "Tenpo Prepago", "Prepago Los Heroes", "Mercado Pago",
                "TAPP Caja los Andes", "Copec Pay", "La Polar Prepago", "Global66", "Prex"};
        for(String selectedBank : BANKS) {
            if(bank.equals(selectedBank)) {
                return "";
            }
        }
        return env.getProperty("message.field.bad.format.bank");
    }

    // Validación de tipo de cuenta en creación de cuenta bancaria - Obligatorio
    public String verifyAccountType(String accountType) {
        if(DataHelper.isNull(accountType)) {
            return env.getProperty("message.field.null");
        }
        final String[] ACCOUNT_TYPES = {"Corriente", "Vista", "Ahorro"};
        for(String selectedAccount : ACCOUNT_TYPES) {
            if(accountType.equals(selectedAccount)) {
                return "";
            }
        }
        return env.getProperty("message.field.bad.format.account-type");
    }

    // Validación de número de cuenta en creación de cuenta bancaria - Obligatorio
    public String verifyAccountNumber(String accountNumber) {
        if(DataHelper.isNull(accountNumber)) {
            return env.getProperty("message.field.null");
        }
        final String ACCOUNT_NUMBER_REGEX = "^[^\\s]{4,}$";
        String accountNumberToTrim = accountNumber.trim(); // CON TRIM() INCLUIDO (no permite saltos en línea)
        if(!accountNumberToTrim.matches(ACCOUNT_NUMBER_REGEX)) { 
            return env.getProperty("message.field.min.characters.4");
        }
        return "";
    }

    // Validación de la patente de un vehículo - Obligatorio
    public String verifyPpu(String ppu) {
        if(DataHelper.isNull(ppu)) { // Valida si es nulo o el valor es vacío
            return env.getProperty("message.field.null");
        }
        // Formatos validados: "AA111", "AAA11", "AA1111", "BBBB11"
        int ppuLength=ppu.length(), totalNumbers=0, totalLetters=0, totalConsonant=0;
        String regexLetters = "^[a-z]+$";
        String regexNumbers = "^[0-9]+$";
        String regexConsonant = "^[bcdfghjklmnpqrstvwxyz]+$";
        if(ppuLength == 5) {
            for(int i=0; i<ppuLength; i++) {
                String character = ppu.substring(i, i+1).toLowerCase();
                if(character.matches(regexLetters)) {
                    totalLetters++;
                } else if(character.matches(regexNumbers)) {
                    totalNumbers++;
                }
            }
            // Se obtuvo el total de números y letras para una patente de moto, ahora se verifica los casos correctos
            if(totalLetters == 3 && totalNumbers == 2 || totalLetters == 2 && totalNumbers == 3) {
                return "";
            }
        } else if(ppuLength == 6) {
            for(int i=0; i<ppuLength; i++) {
                String character = ppu.substring(i, i+1).toLowerCase();
                if(character.matches(regexLetters)) {
                    totalLetters++;
                } else if(character.matches(regexNumbers)) {
                    totalNumbers++;
                }
                if(character.matches(regexConsonant)) {
                    totalConsonant++;
                }
            }
            // Se obtuvo el total de números y letras para una patente de auto, ahora se verifica los casos correctos
            if(totalLetters == 2 && totalNumbers == 4 || totalConsonant == 4 && totalNumbers == 2) {
                return "";
            }
        }
        return env.getProperty("message.field.bad.format");
    }   
    
    // Validación de un id de un cotizador existente del usuario en proceso de cotización de planes - Obligatorio
    public String verifyQuoterId(String quoterId) {
        if(DataHelper.isNull(quoterId)) {
            return env.getProperty("message.field.null");
        } else if(!ObjectId.isValid(quoterId)) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }
    // Validación de un id de un cotizador existente del usuario en proceso de cotización de planes - Opcional
    public String verifyQuoterIdOptional(String quoterId) {
        if(!DataHelper.isNull(quoterId) && !ObjectId.isValid(quoterId)) {
            return env.getProperty("message.field.bad.id");
        }
        return  "";
    }

    // Validación de marca de vehículo en proceso de cotización de planes - Obligatorio
    public String verifyBrand(String brand) {
        if(DataHelper.isNull(brand)) {
            return env.getProperty("message.field.null");
        } else if (brand.length() > 20) {
            return env.getProperty("message.field.max.characters.20");
        }
        return "";
    }

    // Validación de modelo de vehículo en proceso de cotización de planes - Obligatorio
    public String verifyModel(String model) {
        if(DataHelper.isNull(model)) {
            return env.getProperty("message.field.null");
        } else if (model.length() > 20) {
            return env.getProperty("message.field.max.characters.20");
        }
        return "";
    }

    // Validación de año de vehículo en proceso de cotización de planes - Obligatorio
    public String verifyYear(String yearStr) {
        if(DataHelper.isNull(yearStr)) {
            return env.getProperty("message.field.null");
        }
        try {
            int year = Integer.parseInt(yearStr);
            if(year <= 1900 || year > LocalDate.now().getYear()) {
                return env.getProperty("message.field.bad.format.year");
            }
        } catch(NumberFormatException e) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }

    // Validación de opción que explica la relación con el propietario del vehículo en proceso de cotización de planes - Obligatorio
    public String verifyOwnerOption(String ownerOption) {
        if(DataHelper.isNull(ownerOption)) {
            return env.getProperty("message.field.null");
        }
        try {
            int option = Integer.parseInt(ownerOption);
            if(option < 0 || option > 4) {
                return env.getProperty("message.field.bad.format.owner-option");
            }
        } catch(NumberFormatException e) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }

    // Validación de alias de aseguradora seleccionada en proceso de cotización de planes - Obligatorio
    public String verifyInsurerAlias(String insurerAlias) {
        if(DataHelper.isNull(insurerAlias)) {
            return env.getProperty("message.field.null");
        }
        final int INSURER_LENGTH = insurerAlias.trim().length(); // CON TRIM() INCLUIDO (permite saltos en línea)
        if(INSURER_LENGTH > 20) {
            return env.getProperty("message.field.max.characters.20");
        }
        return "";
    }

    // Validación del id del plan en recopilación de datos del plan - Obligatorio
    public String verifyPlanId(String planId) {
        if(DataHelper.isNull(planId)) {
            return env.getProperty("message.field.null");
        } else if (planId.length() < 4) {
            return env.getProperty("message.field.min.characters.4");
        }
        return "";
    }

    // Validación del nombre de la aseguradora en recopilación de datos del plan - Obligatorio
    public String verifyInsurer(String insurer) {
        if(DataHelper.isNull(insurer)) {
            return env.getProperty("message.field.null");
        }
        final int INSURER_LENGTH = insurer.trim().length(); // CON TRIM() INCLUIDO (permite saltos en línea)
        if (INSURER_LENGTH < 4) {
            return env.getProperty("message.field.min.characters.4");
        }
        return "";
    }

    // Validación del nombre del plan en recopilación de datos del plan - Obligatorio
    public String verifyPlanName(String planName) {
        if(DataHelper.isNull(planName)) {
            return env.getProperty("message.field.null");
        }
        final int PLAN_NAME_LENGTH = planName.trim().length(); // CON TRIM() INCLUIDO (permite saltos en línea)
        if (PLAN_NAME_LENGTH < 4) {
            return env.getProperty("message.field.min.characters.4");
        }
        return "";
    }
    
    // Validación de la dirección del comprador del plan en recopilación de datos del plan - Obligatorio
    public String verifyStreet(String street) {
        if(DataHelper.isNull(street)) {
            return env.getProperty("message.field.null");
        }
        final int STREET_LENGTH = street.trim().length(); // CON TRIM() INCLUIDO (permite saltos en línea)
        final String STREET_REGEX = "^[a-zA-Z0-9áéíóúñçýÁÉÍÓÚÑÇÝ.,_/#\\s\\-]+$";
        if(STREET_LENGTH < 4) {
            return env.getProperty("message.field.min.characters.4");
        } else if(STREET_LENGTH > 50) {
            return env.getProperty("message.field.max.characters.50");
        } else if(!street.matches(STREET_REGEX)) {
            return env.getProperty("message.field.non.allow.characters.address");
        }
        return "";
    }

    // Validación del número de la calle del comprador del plan en recopilación de datos del plan - Obligatorio
    public String verifyStreetNumber(String streetNumber) {
        if(DataHelper.isNull(streetNumber)) {
            return env.getProperty("message.field.null");
        }
        final int STREET_NUMBER_LENGTH = streetNumber.trim().length(); // CON TRIM() INCLUIDO (permite saltos en línea)
        final String STREET_NUMBER_REGEX = "^[a-zA-Z0-9.,_/#\\s\\-]+$";
        if(STREET_NUMBER_LENGTH > 20) {
            return env.getProperty("message.field.max.characters.20");
        } else if(!streetNumber.matches(STREET_NUMBER_REGEX)) {
            return env.getProperty("message.field.non.allow.characters.street-number");
        }
        return "";
    }

    // Validación del número de departamento del comprador del plan en recopilación de datos del plan - Opcional
    public String verifyDepartment(String department) {
        if(!DataHelper.isNull(department)) {
            if(department.trim().length() > 20) { // CON TRIM() INCLUIDO (permite saltos en línea) - opcional
                return env.getProperty("message.field.max.characters.20");
            }
        }
        return "";
    }

    // Validación del tipo de solicitud que se está realizando cuando se buscan los planes, si es una cotización "Manual" o "Auto"
    public String verifyRequestTypeForSearchPlan(String requestType) {
        if(DataHelper.isNull(requestType)) {
            return env.getProperty("message.field.null");
        }
        if(!requestType.equals("Manual") && !requestType.equals("Auto")) {
            return env.getProperty("message.field.bad.format");
        }
        return "";
    }

    // Validación del los valores del plan que sean númericos mayor o igual a 0- Obligatorio
    public String verifyNumberValue(double priceUf) {
        if(priceUf < 0) {
            return env.getProperty("message.field.negative-number");
        }
        return "";
    }

}
