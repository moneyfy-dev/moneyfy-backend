package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.referidos.app.segurosref.dtos.TokensDto;
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
        String profileRole = userData.getProfileRole();
        String status = userData.getStatus();
        String phone = (userData.getPhone() != null) ? userData.getPhone() : "";
        String dateOfBirth = (userData.getDateOfBirth() != null) ? userData.getDateOfBirth().toString() : "";
        return new UserSimpleDto(userModel.getUserId(), name, surname, email, phone, profileRole, dateOfBirth, status);
    }

    public static Map<String, Object> buildUser(UserModel userModel) {
        String jwtSession = userModel.getPersonalData().getSessionToken();
        String jwtRefresh = userModel.getPersonalData().getRefreshToken();
        return Map.of("user", userModel, "tokens", new TokensDto(jwtSession, jwtRefresh));
    }

    public static Map<String, Object> buildUser(UserModel userModel, String key3, Object value3) {
        String jwtSession = userModel.getPersonalData().getSessionToken();
        String jwtRefresh = userModel.getPersonalData().getRefreshToken();
        TokensDto tokens = new TokensDto(jwtSession, jwtRefresh);
        return Map.of("user", userModel, "tokens", tokens, key3, value3);
    }

    public static Map<String, Object> buildUser(UserModel userModel, String key3, Object value3, String key4, Object value4) {
        String jwtSession = userModel.getPersonalData().getSessionToken();
        String jwtRefresh = userModel.getPersonalData().getRefreshToken();
        TokensDto tokens = new TokensDto(jwtSession, jwtRefresh);
        return Map.of("user", userModel, "tokens", tokens, key3, value3, key4, value4);
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
        for(AccountModel accountDB : userDB.getAccounts()) {
            if(accountDB.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public static boolean verifyImageFile(MultipartFile file) {
        if(file == null) {
            return false;
        }
        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("image/")
                || file.getSize() > 204800) { // 200KB
            return false;
        }
        return true;
    }

    public static String findQuoteOwnerOption(String ownerOption) { // Valor númerico de option
        String[][] quoteOwnerOption = quoteOwnerOption();

        for(String[] quoteOwner : quoteOwnerOption) {
            if(quoteOwner[0].equals(ownerOption)) {
                // Retornar valor del la opción númerica
                return quoteOwner[1];
            }
        }

        return null;
    }

    private static String[][] quoteOwnerOption() {
        return new String[][] {
                {"0", "si, soy el dueno del vehiculo"},
                {"1", "no, soy el padre/madre del dueno"},
                {"2", "no, soy el conviviente civil del dueno"},
                {"3", "no, soy el conyuge del dueno"},
                {"4", "no, soy el hijo(a) del dueno"}};
    }

    public static String createCode(int length, boolean withPrefix, String prefix, boolean withPattern, String pattern, int patternMultiple) {
        String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "o", "p", "q", "r",
                "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L",
                "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5",
                "6", "7", "8", "9"};
        StringBuilder sb;
        // Revisar si lleva algún prefijo
        if(withPrefix) {
            sb = new StringBuilder(prefix);
        } else {
            sb = new StringBuilder();
        }
        // Revisar si lleva algún patrón
        if(withPattern) {
            for(int i=0; i<length; i++) {
                if((i+1)%patternMultiple == 0) {
                    sb.append(pattern);
                } else {
                    sb.append(letters[ ((int) (Math.random()*62)) ]);
                }
            }
        } else {
            for(int i=0; i<length; i++) {
                sb.append(letters[ ((int) (Math.random()*62)) ]);
            }
        }
        // LOGGER_MESSAGES.info("\n-----\nCódigo final generado: " + sb.toString() + "\n-----");
        return sb.toString();
    }

    // Creación de nueva notificación del usuario
    public static NotificationDataModel novaNotification(String message, String type, LocalDateTime currenDateTime) {
        return new NotificationDataModel(new ObjectId(), message, type, "Sin notificar", currenDateTime, currenDateTime);
    }

    // Creamos un código para que el usuario pueda referir, y verificamos que el código no exista actualmente.
    @Transactional(readOnly = true)
    public static String generateCodeToRefer(UserRepository userRepository) {
        String codeToRefer;
        do {
            codeToRefer = DataHelper.createCode(6, false, "", false, "", 0);
            if(userRepository.existsByCodeToRefer(codeToRefer)) {
                codeToRefer = "";
            }
        } while(codeToRefer.equals(""));
        return codeToRefer;
    }

}
