package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

// El usuario helper, tiene funcionalidad como repositorio, se puede inyectarse a los servicios, para solucionar problemas
// específicos, pero no puede inyectarse es su propia clase: servicios
@Component
public class UserHelper {

    // Lista de los usuarios de prueba
    public static List<String> testUsers() {
        return List.of("nuser.random01@gmail.com");
    }
    // Verificar usuario de pruba
    public static boolean isTestUser(String emailAuth) {
        List<String> testUsers = testUsers();
        for(String testUser : testUsers) {
            if(testUser.equals(emailAuth)) {
                return true;
            }
        }
        return false;
    }

    // Lista de los usuarios por defecto
    public static List<String> defaultUsers() {
        return List.of("nuser.random@gmail.com",
            "gottafindshape@gmail.com",
            "eliu.martineez@gmail.com"
        );
    }
    // Verificar usuario por defecto
    public static boolean isDefaulUser(String emailAuth) {
        List<String> defaultUsers = defaultUsers();
        for(String defaultUser : defaultUsers) {
            if(defaultUser.equals(emailAuth)) {
                return true;
            }
        }
        return false;
    }

    // Actualizar token de refresco
    @Transactional
    public static void updateRefreshToken(UserRepository userRepository, UserModel userDB, DeviceModel deviceDB, DeviceRepository deviceRepository) {
        UserDataModel userDataDB = userDB.getPersonalData();
        String newRefreshToken = JwtConfig.createRefreshToken(userDataDB.getEmail());
        userDataDB.setRefreshToken(newRefreshToken);
        deviceDB.setRefreshToken(newRefreshToken);
        userRepository.save(userDB);
        deviceRepository.save(deviceDB);
    }

    // FLUJOS PARA DESACTIVACIÓN Y ACTIVACIÓN DEL USUARIO
    // Función que verifica el usuario para activarlo o dejarlo obsoleto, ya que, se encuentra desactivado
    @Transactional
    public UserModel checkUserAccount(UserRepository userRepository, DeviceRepository deviceRepository,
            ReferredRepository referredRepository, UserModel userDB,String device, String deviceIp) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDate deactivationDate = userDB.getDisableAccount().toLocalDate();
        long daysBetween = currentDateTime.toLocalDate().toEpochDay() - deactivationDate.toEpochDay();
        // LOGGER_MESSAGES.info("\n-----\nDías transcurridos del usuario deshabilitado: " + daysBetween + "\n-----");
        // El usuario queda obsoleto, si ya transcurrio el tiempo estipulado en el estado 'Desactivado' o sea más de 30 días.
        if(daysBetween > 30 && deactivationDate.getYear() > 2020) {
            this.obsoleteUser(userRepository, deviceRepository, referredRepository, userDB, currentDateTime);
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

    @SuppressWarnings("null")
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
    
    // Se utiliza cuando el usuario se encuentra "Desactivado" y si se cumplen los 30 días del usuario desactivado se "elimina"
    @Transactional
    public boolean makeUserObsolete(UserRepository userRepository, DeviceRepository deviceRepository, ReferredRepository referredRepository, UserModel userDB) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDate deactivationDate = userDB.getDisableAccount().toLocalDate();
        long daysBetween = currentDateTime.toLocalDate().toEpochDay() - deactivationDate.toEpochDay();
        if(daysBetween > 30 && deactivationDate.getYear() > 2020) {
            this.obsoleteUser(userRepository, deviceRepository, referredRepository, userDB, currentDateTime);
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    @Transactional
    public void obsoleteUser(UserRepository userRepository, DeviceRepository deviceRepository,
            ReferredRepository referredRepository, UserModel userDB, LocalDateTime currentDateTime) {
        // El usuario estuvo deshabilidato por más de 30 días, por lo tanto, queda obsoleto cambiándole el email,
        // por un código único con el subfijo ".user-deleted". Los registros de device aunque ya deben estar
        // eliminados, consultamos para asegurarnos que no existan, por otro lado, los registros de referidos
        // quedarían con la nueva llave del email, pero deshabilitados. Ahora, generamos un email de eliminación
        // para el usuario, que será el mismo que el código para referir ahora (un código obsoleto), así libramos
        // un cupo del código anterior del usuario. Y los registros de transacciones, pagos y logs aún quedan
        // relacionados al usuario obsoleto porque se relacionan por id, no por mail.
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
        Optional<DeviceModel> deviceUser = deviceRepository.findByUser(oldEmailUserDB);
        if(deviceUser.isPresent()) {
            deviceRepository.delete(deviceUser.get());
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

    public String[] checkUserAgent(HttpServletRequest request, String userEmail) {
            String device = (!DataHelper.isNull(request.getHeader("User-Agent"))) ? request.getHeader("User-Agent") : "Se está verificando la información del dispositivo:" + userEmail;
            String deviceIp = (!DataHelper.isNull(request.getRemoteAddr())) ? request.getRemoteAddr() : "Se está verificando la IP del dispositivo:" + userEmail;
        return new String[] {device, deviceIp};
    }

}
