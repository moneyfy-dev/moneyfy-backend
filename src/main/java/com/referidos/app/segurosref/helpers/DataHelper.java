package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import com.referidos.app.segurosref.dtos.UserSimpleDto;
import com.referidos.app.segurosref.models.AccountModel;
import com.referidos.app.segurosref.models.NotificationDataModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.UserRepository;

public class DataHelper {

    public static UserSimpleDto buildSimpleUser(UserModel userModel) {
        UserDataModel userData = userModel.getPersonalData();
        String name = userData.getName();
        String surname = userData.getSurname();
        String email = userData.getEmail();
        String profileRole = "USER";
        String status = userData.getStatus();
        String phone = (userData.getPhone() != null) ? userData.getPhone() : "";
        String dateOfBirth = (userData.getDateOfBirth() != null) ? userData.getDateOfBirth().toString() : "";
        return new UserSimpleDto(userModel.getUserId(), name, surname, email, phone, profileRole, dateOfBirth, status);
    }

    public static Map<String, Object> buildUser(UserModel userModel) {
        return Map.of("user", userModel);
    }

    public static Map<String, Object> buildUser(UserModel userModel, String key3, Object value3) {
        return Map.of("user", userModel, key3, value3);
    }

    public static Map<String, Object> buildUser(UserModel userModel, String key3, Object value3, String key4,
            Object value4) {
        return Map.of("user", userModel, key3, value3, key4, value4);
    }

    public static Map<String, Object> buildUser(UserModel userModel, Map<String, Object> data) {
        return Map.of("user", userModel, "data", data);
    }

    public static Map<String, Object> buildUserAuthData(UserModel userModel, String sessionToken, String refreshToken) {
        return Map.of(
                "user", userModel,
                "sessionToken", sessionToken,
                "refreshToken", refreshToken
        );
    }

    public static LocalDate deprecatedDate() {
        return LocalDate.of(1900, 1, 1);
    }

    public static LocalDateTime deprecatedDateTime() {
        return LocalDateTime.of(1900, 1, 1, 0, 0, 0);
    }

    public static boolean isNull(String field) {
        return field == null || field.isBlank();
    }

    // Buscamos si al menos existe una cuenta del usuario activa
    public static boolean accountAvailable(UserModel userDB) {
        for (AccountModel accountDB : userDB.getAccounts()) {
            if (accountDB.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public static String findQuoteOwnerOption(String ownerOption) { // Valor númerico de option
        String[][] quoteOwnerOption = quoteOwnerOption();

        for (String[] quoteOwner : quoteOwnerOption) {
            if (quoteOwner[0].equals(ownerOption)) {
                // Retornar valor del la opción númerica
                return quoteOwner[1];
            }
        }

        return null;
    }

    private static String[][] quoteOwnerOption() {
        return new String[][] {
                { "0", "si, soy el dueno del vehiculo" },
                { "1", "no, soy el padre/madre del dueno" },
                { "2", "no, soy el conviviente civil del dueno" },
                { "3", "no, soy el conyuge del dueno" },
                { "4", "no, soy el hijo(a) del dueno" } };
    }

    public static String createCode(int length, boolean withPrefix, String prefix, boolean withPattern, String pattern,
            int patternMultiple) {
        String[] letters = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "o", "p", "q", "r",
                "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L",
                "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5",
                "6", "7", "8", "9" };
        StringBuilder sb;
        // Revisar si lleva algún prefijo
        if (withPrefix) {
            sb = new StringBuilder(prefix);
        } else {
            sb = new StringBuilder();
        }
        // Revisar si lleva algún patrón
        if (withPattern) {
            for (int i = 0; i < length; i++) {
                if ((i + 1) % patternMultiple == 0) {
                    sb.append(pattern);
                } else {
                    sb.append(letters[((int) (Math.random() * letters.length))]);
                }
            }
        } else {
            for (int i = 0; i < length; i++) {
                sb.append(letters[((int) (Math.random() * letters.length))]);
            }
        }
        // LOGGER_MESSAGES.info("----- " + sb.toString() + "-----\n\n");
        return sb.toString();
    }

    // Creación de nueva notificación del usuario
    public static NotificationDataModel novaNotification(String message, String type, LocalDateTime currenDateTime) {
        return new NotificationDataModel(new ObjectId(), message, type, false, false, currenDateTime, currenDateTime);
    }

    // Creamos un código para que el usuario pueda referir, y verificamos que el
    // código no exista actualmente.
    @Transactional(readOnly = true)
    public static String generateCodeToRefer(UserRepository userRepository) {
        String codeToRefer;
        do {
            codeToRefer = DataHelper.createCode(6, false, "", false, "", 0);
            if (userRepository.existsByCodeToRefer(codeToRefer)) {
                codeToRefer = "";
            }
        } while (codeToRefer.equals(""));
        return codeToRefer;
    }

    public static Map<String, Object> buildErrorFields(BindingResult result) {
        Map<String, Object> json = new HashMap<>();

        result.getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            json.put(fieldName, "The field " + fieldName + " " + error.getDefaultMessage());
        });

        return json;
    }

    public static String generateRandomCode() {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < 6; i++) {
            sb.append(((int) (Math.random() * 10)));
        }
        return sb.toString();
    }

}
