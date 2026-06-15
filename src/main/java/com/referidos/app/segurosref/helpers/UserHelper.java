package com.referidos.app.segurosref.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

@Component
public class UserHelper {

    // FLUJOS PARA DESACTIVACIÓN Y ACTIVACIÓN DEL USUARIO
    // Función que verifica el usuario para activarlo o dejarlo obsoleto, ya que, se
    // encuentra desactivado
    @Transactional
    public UserModel checkUserAccount(UserRepository userRepository, ReferredRepository referredRepository,
            UserModel userDB) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDate deactivationDate = userDB.getDisableAccount().toLocalDate();
        long daysBetween = currentDateTime.toLocalDate().toEpochDay() - deactivationDate.toEpochDay();

        // El usuario queda obsoleto, si ya transcurrio el tiempo estipulado en el
        // estado 'Desactivado' o sea más de 30 días.
        if (daysBetween > 30 && deactivationDate.getYear() > 2020) {
            this.obsoleteUser(userRepository, referredRepository, userDB, currentDateTime);
            return null;
        }
        // Se vuelve a activar el registro del usuario, ya que, no ha transcurrido más
        // de 30 días hábiles.
        return this.enableUserAccount(userRepository, referredRepository, userDB, currentDateTime);
    }

    @Transactional
    public UserModel enableUserAccount(UserRepository userRepository, ReferredRepository referredRepository,
            UserModel userDB, LocalDateTime currenDateTime) {
        // Se buscan todos los registros que esten relacionados con el usuario, para
        // volver a activarse
        UserDataModel userDataDB = userDB.getPersonalData();
        String emailAuth = userDataDB.getEmail();
        userDataDB.setStatus("Activado");
        userDB.setDisableAccount(DataHelper.deprecatedDateTime());
        userDB = userRepository.save(userDB);

        // Se recupera la data de los registros relacionados a los referidos para volver
        // a activarlos
        List<ReferredModel> updateTheReferreds = new ArrayList<>();
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(emailAuth);
        if (referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferredStatus("Activado");
            referredByUserA.setUpdatedDate(currenDateTime);
            updateTheReferreds.add(referredByUserA);
        }
        List<ReferredModel> usersC = referredRepository.findAllByUserReferring(emailAuth);
        for (ReferredModel userC : usersC) {
            userC.setUserReferringStatus("Activado");
            userC.setUpdatedDate(currenDateTime);
            updateTheReferreds.add(userC);
        }
        if (updateTheReferreds.size() > 0) {
            referredRepository.saveAll(updateTheReferreds);
        }
        return userDB;
    }

    // Se utiliza cuando el usuario se encuentra "Desactivado" y si se cumplen los
    // 30 días del usuario desactivado se "elimina"
    @Transactional
    public boolean makeUserObsolete(UserRepository userRepository, ReferredRepository referredRepository,
            UserModel userDB) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDate deactivationDate = userDB.getDisableAccount().toLocalDate();
        long daysBetween = currentDateTime.toLocalDate().toEpochDay() - deactivationDate.toEpochDay();
        if (daysBetween > 30 && deactivationDate.getYear() > 2020) {
            this.obsoleteUser(userRepository, referredRepository, userDB, currentDateTime);
            return true;
        }
        return false;
    }

    // TODO: Revisar flujo, si el flujo de desactivación se mantiene como está, se
    // debe evaluar la eliminación del objeto de autenticación asociado a la cuenta
    // del usuario.
    @Transactional
    public void obsoleteUser(UserRepository userRepository, ReferredRepository referredRepository, UserModel userDB,
            LocalDateTime currentDateTime) {
        // El usuario estuvo deshabilidato por más de 30 días, por lo tanto, queda
        // obsoleto cambiándole el email,
        // por un código único con el subfijo ".user-deleted".
        // Los registros de referidos
        // quedarían con la nueva llave del email, pero deshabilitados. Ahora, generamos
        // un email de eliminación
        // para el usuario, que será el mismo que el código para referir ahora (un
        // código obsoleto), así libramos
        // un cupo del código anterior del usuario. Y los registros de transacciones,
        // pagos y logs aún quedan
        // relacionados al usuario obsoleto porque se relacionan por id, no por mail.
        String oldEmailUserDB = userDB.getPersonalData().getEmail();
        String emailForUserDeleted;
        String codeForUserDeleted;
        do {
            codeForUserDeleted = DataHelper.createCode(18, false, "", false, "", 0);
            emailForUserDeleted = codeForUserDeleted + ".user-deleted";
            if (userRepository.existsByPersonalData_Email(emailForUserDeleted)) {
                emailForUserDeleted = "";
            }
        } while (emailForUserDeleted.equals(""));
        // Le asignamos el nuevo email al usuario obsoleto y le intercambiamos el código
        // de referido, para liberar cupo
        userDB.setCodeToRefer(codeForUserDeleted);
        userDB.getPersonalData().setEmail(emailForUserDeleted);
        userDB.getPersonalData().setStatus("Obsoleto");
        userRepository.save(userDB);

        // Buscamos a los referidos para dejarlos obsoletos y para asignarle el nuevo
        // email de usuario eliminado
        List<ReferredModel> updateTheReferreds = new ArrayList<>();
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(oldEmailUserDB);
        if (referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferred(emailForUserDeleted);
            referredByUserA.setReferredStatus("Obsoleto");
            referredByUserA.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(referredByUserA);
        }
        List<ReferredModel> usersC = referredRepository.findAllByUserReferring(oldEmailUserDB);
        for (ReferredModel userC : usersC) {
            userC.setUserReferring(emailForUserDeleted);
            userC.setCodeToRefer(codeForUserDeleted);
            userC.setUserReferringStatus("Obsoleto");
            userC.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(userC);
        }
        if (updateTheReferreds.size() > 0) {
            referredRepository.saveAll(updateTheReferreds);
        }
    }

}
