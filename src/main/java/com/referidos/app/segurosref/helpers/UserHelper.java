package com.referidos.app.segurosref.helpers;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.models.WhiteListModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.repositories.WhiteListRepository;
import com.referidos.app.segurosref.seeder.RunUserSeeder;

// El usuario helper, tiene funcionalidad como repositorio, se puede inyectarse a los servicios, para solucionar problemas
// específicos, pero no puede inyectarse es su propia clase: servicios
@Component
public class UserHelper {

    // FUNCIÓN PARA REGISTRAR USUARIOS DE PRUEBAS
    @Transactional
    public String seedTestUsers(UserRepository userRepository, ReferredRepository referredRepository,
            DeviceRepository deviceRepository, WhiteListRepository whiteListRepository, PasswordEncoder pwdEncoder) {
        boolean novaUsers = false;
        boolean existUsers = false;
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime deprecatedDateTime = DataHelper.deprecatedDateTime();

        for(String user : RunUserSeeder.testUsers()) {
            Optional<UserModel> optionalUser = userRepository.findByPersonalData_Email(user);
            if(optionalUser.isPresent()) {
                UserModel userDB = optionalUser.get();
                UserDataModel userDataDB = userDB.getPersonalData();
                switch (userDataDB.getStatus()) {
                    case "Activado" -> {
                        existUsers = true;
                        break;
                    }
                    case "Desactivado" -> {
                        if(!userDataDB.getSessionToken().equals("") && !userDataDB.getRefreshToken().equals("")) {
                            // Tiene tokens, hay que ver si se puede eliminar el usuario => como makeUserObsolet
                            if(this.makeUserObsolete(userRepository, deviceRepository, whiteListRepository, referredRepository, userDB)) {
                                // El usuario queda obsoleto, y se puede crear nuevamente el usuario de prueba
                                String novaUser = this.registerTestUser(user, userRepository, referredRepository, pwdEncoder, deprecatedDateTime, currentDate);
                                if(novaUser == null) {
                                    return null;
                                } else {
                                    novaUsers = true;
                                }
                            } else {
                                // Usuario que todavía se puede habilitar, por lo tanto, existe
                                existUsers = true;
                            }
                        } else {
                            // Usuario no está confirmado, se puede eliminar el usuario con su registro de referido.
                            Optional<ReferredModel> optionalReferred = referredRepository.findByReferred(user);
                            if(optionalReferred.isPresent()) {
                                referredRepository.delete(optionalReferred.get());
                            }
                            userRepository.delete(userDB);
                            // Luego de haberse eliminado el usuario NO confirmado, lo registramos
                            String novaUser = this.registerTestUser(user, userRepository, referredRepository, pwdEncoder,
                                    deprecatedDateTime, currentDate);
                            if(novaUser == null) {
                                return null;
                            } else {
                                novaUsers = true;
                            }
                        }
                        break;
                    }
                    default -> {
                        return null;
                    }
                }
            } else {
                String novaUser = registerTestUser(user, userRepository, referredRepository, pwdEncoder,
                        deprecatedDateTime, currentDate);
                if(novaUser == null) {
                    return null;
                } else {
                    novaUsers = true;
                }
            }
        }

        if(novaUsers && !existUsers) {
            return "se han registrados los usuarios de prueba";
        } else if(!novaUsers && existUsers) {
            return "los usuarios de prueba se encuentran registrados";
        } else if(novaUsers && existUsers) {
            return "hay usuarios de prueba existentes y se han registrado nuevos usuarios de prueba";
        } else {
            return null;
        }
    }

    // REGISTRAR UN USUARIO DE PRUEBA
    private String registerTestUser(String user, UserRepository userRepository, ReferredRepository referredRepository,
            PasswordEncoder pwdEncoder, LocalDateTime deprecatedDateTime, LocalDateTime currentDate) {
        try {
            String sessionToken = JwtConfig.createSessionToken(user, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
            String refreshToken = JwtConfig.createRefreshToken(user);
            String codeToRefer = DataHelper.generateCodeToRefer(userRepository);
            UserDataModel userData = new UserDataModel("Test", "User", user, "", "",
                    DataHelper.deprecatedDate(), "Activado", new byte[0], pwdEncoder.encode("Testing_123"), "",
                    "ROLE_USER", "", deprecatedDateTime, sessionToken, refreshToken);
            WalletModel userWallet = new WalletModel(0, 0, 0, 0);
            NotificationModel userNotifs = new NotificationModel(false, true, true,
                    false, false, true, false, false, false, new ArrayList<>());
            // Creamos la estructura del usuario 'seeder'
            UserModel novaUser = new UserModel(codeToRefer, deprecatedDateTime, userData, userWallet, userNotifs);
            ReferredModel novaReferred = new ReferredModel("Sin usuario", "Sin usuario",
                    user, "Desactivado", "Activado", currentDate, currentDate);
            // Guardamos en la base de datos, y lo agregamos a la lista de los usuarios 'seeders'
            userRepository.save(novaUser);
            referredRepository.save(novaReferred);
            return user;
        } catch(Exception e) {
            LOGGER_MESSAGES.info("No se ha podido registrar el usuario: " + user);
        }
        return null;
    }

    // FLUJOS PARA DESACTIVACIÓN Y ACTIVACIÓN DEL USUARIO
    // Función que verifica el usuario para activarlo o dejarlo obsoleto, ya que, se encuentra desactivado
    @Transactional
    public UserModel checkUserAccount(UserRepository userRepository, DeviceRepository deviceRepository,
            WhiteListRepository whiteListRepository, ReferredRepository referredRepository, UserModel userDB,
            String device, String deviceIp) {
        // Usuario que al menos una vez estuvo: "Activado"
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDate deactivationDate = userDB.getDisableAccount().toLocalDate();
        long daysBetween = currentDateTime.toLocalDate().toEpochDay() - deactivationDate.toEpochDay();
        // LOGGER_MESSAGES.info("\n-----\nDías transcurridos del usuario deshabilitado: " + daysBetween + "\n-----");
        // El usuario queda obsoleto, si ya transcurrio el tiempo estipulado en el estado 'Desactivado' o sea más de 30 días.
        if(daysBetween > 30 && deactivationDate.getYear() > 2020) {
            this.obsoleteUser(userRepository, deviceRepository, whiteListRepository, referredRepository, userDB, currentDateTime);
            return null;
        }
        // Se vuelve a activar el registro del usuario, ya que, no ha transcurrido más de 30 días hábiles.
        return this.enableUserAccount(userRepository, referredRepository, deviceRepository, userDB, device, deviceIp, currentDateTime);
    }

    @Transactional
    public UserModel enableUserAccount(UserRepository userRepository, ReferredRepository referredRepository,
            DeviceRepository deviceRepository, UserModel userDB, String device, String deviceIp, LocalDateTime currenDateTime) {
        // Se buscan todos los registros que esten relacionados con el usuario, para volver a activarse
        UserDataModel userDataDB = userDB.getPersonalData();
        String emailAuth = userDataDB.getEmail();
        String refreshToken = userDataDB.getRefreshToken();
        userDataDB.setStatus("Activado");
        userDB.setDisableAccount(DataHelper.deprecatedDateTime());
        userDB = userRepository.save(userDB);
        // Actualizamos el dispositivo del usuario
        this.updateUserDevice(deviceRepository, emailAuth, refreshToken, device, deviceIp, currenDateTime);
        // Se recupera la data de los registros relacionados a los referidos para volver a activarlos
        List<ReferredModel> updateTheReferreds = new ArrayList<>();
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(emailAuth);
        if(referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferredStatus("Activado");
            referredByUserA.setUpdatedDate(currenDateTime);
            updateTheReferreds.add(referredByUserA);
        }
        List<ReferredModel> usersC = referredRepository.findAllByUserReferring(emailAuth);
        for(ReferredModel userC : usersC) {
            userC.setUserReferringStatus("Activado");
            userC.setUpdatedDate(currenDateTime);
            updateTheReferreds.add(userC);
        }
        if(updateTheReferreds.size() > 0) {
            referredRepository.saveAll(updateTheReferreds);
        }
        return userDB;
    }

    @Transactional
    public void updateUserDevice(DeviceRepository deviceRepository, String email, String refreshToken, String device,
            String deviceIp, LocalDateTime currentDateTime) {
        Optional<DeviceModel> deviceOptional = deviceRepository.findByUser(email);
        if(deviceOptional.isPresent() && !deviceOptional.get().getDevice().equals(device)) {
            deviceRepository.delete(deviceOptional.get());
            deviceRepository.save(new DeviceModel(device, email, refreshToken, Collections.singleton(deviceIp), currentDateTime, currentDateTime));
        } else if(deviceOptional.isEmpty()){
            deviceRepository.save(new DeviceModel(device, email, refreshToken, Collections.singleton(deviceIp), currentDateTime, currentDateTime));
        }
    }
    
    @Transactional
    public boolean makeUserObsolete(UserRepository userRepository, DeviceRepository deviceRepository,
            WhiteListRepository whiteListRepository, ReferredRepository referredRepository, UserModel userDB) {
        // Usuario que al menos una vez estuvo: "Activado"
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDate deactivationDate = userDB.getDisableAccount().toLocalDate();
        long daysBetween = currentDateTime.toLocalDate().toEpochDay() - deactivationDate.toEpochDay();
        if(daysBetween > 30 && deactivationDate.getYear() > 2020) {
            this.obsoleteUser(userRepository, deviceRepository, whiteListRepository, referredRepository, userDB, currentDateTime);
            return true;
        }
        return false;
    }

    @Transactional
    public void obsoleteUser(UserRepository userRepository, DeviceRepository deviceRepository,
            WhiteListRepository whiteListRepository, ReferredRepository referredRepository, UserModel userDB,
            LocalDateTime currentDateTime) {
        // El usuario estuvo deshabilidato por más de 30 días, por lo tanto, queda obsoleto cambiándole el email,
        // por un código único con el subfijo ".user-deleted". Los registros de device y whitelist, aunque, ya deben estar
        // eliminados, consultamos para asegurarnos que no existan, por otro lado, los registros de transacciones y
        // de referidos quedarían con la nueva llave del email, pero deshabilitados. Ahora, generamos un email de
        // eliminación para el usuario, que será el mismo que el código para referir ahora (un código obsoleto),
        // así libramos un cupo del código anterior del usuario.
        String oldEmailUserDB = userDB.getPersonalData().getEmail();
        String emailForUserDeleted;
        String codeForUserDeleted;
        do {
            codeForUserDeleted = DataHelper.createCode(18, false, "", false, "", 0);
            emailForUserDeleted = codeForUserDeleted + ".user-deleted";
            if(userRepository.existsByPersonalData_Email(emailForUserDeleted)) {
                emailForUserDeleted = "";
            }
        } while(emailForUserDeleted.equals(""));
        // Le asignamos el nuevo email al usuario obsoleto y le intercambiamos el código de referido, para liberar cupo
        userDB.setCodeToRefer(codeForUserDeleted);
        userDB.getPersonalData().setEmail(emailForUserDeleted);
        userDB.getPersonalData().setStatus("Obsoleto");
        userRepository.save(userDB);
        // Revisamos si existe dispositivo
        Optional<DeviceModel> deviceUserB = deviceRepository.findByUser(oldEmailUserDB);
        if(deviceUserB.isPresent()) {
            deviceRepository.delete(deviceUserB.get());
        }
        // Revisamos si existe whilelist
        Optional<WhiteListModel> whilteListUserB = whiteListRepository.findByUser(oldEmailUserDB);
        if(whilteListUserB.isPresent()) {
            whiteListRepository.delete(whilteListUserB.get());
        }
        // Buscamos a los referidos para dejarlos obsoletos y para asignarle el nuevo email de usuario eliminado
        List<ReferredModel> updateTheReferreds = new ArrayList<>(); 
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(oldEmailUserDB);
        if(referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferred(emailForUserDeleted);
            referredByUserA.setReferredStatus("Obsoleto");
            referredByUserA.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(referredByUserA);
        }
        List<ReferredModel> usersC = referredRepository.findAllByUserReferring(oldEmailUserDB);
        for(ReferredModel userC : usersC) {
            userC.setUserReferring(emailForUserDeleted);
            userC.setCodeToRefer(codeForUserDeleted);
            userC.setUserReferringStatus("Obsoleto");
            userC.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(userC);
        }
        if(updateTheReferreds.size() > 0) {
            referredRepository.saveAll(updateTheReferreds);
        }
    }

}
