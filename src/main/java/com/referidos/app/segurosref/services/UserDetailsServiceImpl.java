package com.referidos.app.segurosref.services;

import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.helpers.UserHelper;
import com.referidos.app.segurosref.helpers.ValidateInputHelper;
import com.referidos.app.segurosref.integrations.email.providers.EmailAppProvider;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.NotificationDataModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.requests.ConfirmUserRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private EmailAppProvider emailAppProvider;

    @Autowired
    private ValidateInputHelper validateInputHelper;

    @Autowired
    private UserHelper userHelper;

    @Autowired
    private PasswordEncoder pwdEncoder;

    @Transactional(readOnly=true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String userEmail = email.toLowerCase();
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);

        if(userOptional.isEmpty()) {
            throw new UsernameNotFoundException(String.format("El usuario %s no ha sido encontrado", userEmail));
        }
        
        return this.buildUserDetails(userOptional.get());
    }

    private UserDetails buildUserDetails(UserModel user) {
        UserDataModel userData = user.getPersonalData();
        return new User(userData.getEmail(),
            userData.getPwd(),
            true,
            true,
            true,
            true,
            Collections.singletonList(new SimpleGrantedAuthority(userData.getProfileRole())));
    }

    // SERVICIOS PARA EL FLUJO DE REGISTRAR UN NUEVO USUARIO DE LA APLICACIÓN
    public ResponseEntity<GeneralResponse> userRegister(UserRegisterRequest userRegister) {
        // Luego de ser validados los primeros datos, se valida el código de referido para saber si se puede continuar
        String[] userReferring = this.validateCodeToRefer(userRegister.codeToRefer());
        if(userReferring == null) {
            return ResponseHelper.locked("el código del referido es inválido", null);
        }
        // El código del referido sea a encontrando o no se ha incluido, se puede proseguir con la solicitud
        UserDataModel userData = this.createUserData(userRegister.name().strip(), userRegister.surname().strip(), // Usamos strip() para quitar espacios al inicio y final
                userRegister.pwd(), userRegister.email().toLowerCase(), "ROLE_USER");  // Dejamos email en minúsculas
        WalletModel wallet = new WalletModel(0, 0, 0, 0);
        NotificationModel notifs = new NotificationModel(true, true, true,
                false, false, true, false, false, false, new ArrayList<>());
        return this.createUnconfirmedUser(userReferring, userData, wallet, notifs);
    }

    @SuppressWarnings("null")
    @Transactional
    private ResponseEntity<GeneralResponse> createUnconfirmedUser(String[] userReferring, UserDataModel userData,
            WalletModel wallet, NotificationModel notifs) {
        // En caso de no sea haya incluído el código de referido se los valores de userReferring son "Sin Usuario"
        String email = userData.getEmail(); // Mail se trabaja en minúsculas
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(email);
        if(userOptional.isPresent()) {
            UserModel userDB = userOptional.get();
            UserDataModel userDataDB = userDB.getPersonalData();
            if(!userDataDB.getSessionToken().equals("") || !userDataDB.getRefreshToken().equals("")) {
                // Si el usuario tiene valor en alguno de los tokens, quiere decir que en algún momento se creó con éxito
                String statusUserDB = userDataDB.getStatus();
                switch(statusUserDB) {
                    case "Activado" -> {
                        return ResponseHelper.gone("usuario existente", null);
                    }
                    case "Desactivado" -> {
                        if(!userHelper.makeUserObsolete(userRepository, deviceRepository, referredRepository, userDB)) {
                            // El usuario no ha quedado obsoleto, por lo tanto, aún se puede habilitar
                            return ResponseHelper.gone("usuario existente", null);
                        }
                        // Usuario quedo obsoleto y, por lo tanto, se puede seguir con el flujo
                        break;
                    }
                    default -> {
                        // Es imposible llegar a está instancia, por seguridad se agrega dentro del flujo
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    }
                }
            } else {
                // Usuario sin confirmar, por lo tanto, se debe eliminar para seguir el flujo y verificar si tenía un
                // registro como referido para ser eliminado también.
                Optional<ReferredModel> referredOptional = referredRepository.findByReferred(email);
                if(referredOptional.isPresent()) {
                    referredRepository.delete(referredOptional.get());
                }
                // Se elimina el usuario que no fue confirmado
                userRepository.delete(userDB);
            }
        }

        // Todo bien, se envía email para confirmar registro
        String[] toUsers = {email};
        String codeAuth = userData.generateRandomCode();
        emailAppProvider.sendAuthCodeToRegisterUser(toUsers, codeAuth);

        // Se genera el nuevo usuario (no confirmado), además del registro del referido...
        String userReferringState = (userReferring[0].equals("Sin usuario")) ? "Desactivado" : "Activado";
        LocalDateTime currenDateTime = LocalDateTime.now();
        userData.setCodeAuth(pwdEncoder.encode(codeAuth));
        userData.setCodeExpirationTime(currenDateTime); // Se establece el tiempo actual al código de confirmación que tiene una validad de 3 minutos
        UserModel userModel = new UserModel("", DataHelper.deprecatedDateTime(), userData, wallet, notifs);
        ReferredModel referredModel = new ReferredModel(userReferring[0], userReferring[1], email, userReferringState, "Desactivado", currenDateTime, currenDateTime);
        userRepository.save(userModel);
        referredRepository.save(referredModel);

        String responseMessage = "el código de confirmación para finalizar el proceso de registro, ha sido enviado al email: " + email ;
        return ResponseHelper.ok(responseMessage, Map.of("info", "ok"));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<GeneralResponse> confirmRegistration(ConfirmUserRequest confirm,
            HttpServletRequest request) throws JsonProcessingException {
        String userEmail = confirm.email().toLowerCase();
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        if(userData.getSessionToken().equals("") && userData.getRefreshToken().equals("")) {
            boolean isCodeActive = userData.isCodeActive(LocalDateTime.now(), 3);
            boolean codeMatches = pwdEncoder.matches(confirm.code(), userData.getCodeAuth());
            if(isCodeActive && codeMatches) {
                return this.successfulRegistration(userDB, request);
            }
            return ResponseHelper.gone("el código ha expirado o no es correcto", null);
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    private ResponseEntity<GeneralResponse> successfulRegistration(UserModel userDB, HttpServletRequest request) throws JsonProcessingException {
        UserDataModel userData = userDB.getPersonalData();
        String userEmail = userData.getEmail();
        String codeToRefer = DataHelper.generateCodeToRefer(userRepository);
        userDB.setCodeToRefer(codeToRefer);

        // Creamos los tokens para administrar la sesión del usuario
        String sessionToken = JwtConfig.createSessionToken(userEmail, Collections.singletonList(new SimpleGrantedAuthority(userData.getProfileRole())));
        String refreshToken = JwtConfig.createRefreshToken(userEmail);
        userData.setSessionToken(sessionToken);
        userData.setRefreshToken(refreshToken);
        userData.setStatus("Activado");
        
        LocalDateTime currenDateTime = LocalDateTime.now();
        String[] userDeviceInfo = userHelper.checkUserAgent(request, userEmail);
        String device = userDeviceInfo[0];
        String firstIp = userDeviceInfo[1];
        // Se relaciona el registro del usuario con el dispositivo que hizo la consulta de confirmar registro
        DeviceModel deviceModel = new DeviceModel(device, userEmail, refreshToken, Collections.singleton(firstIp), currenDateTime, currenDateTime);
        
        // Actualizamos el registro de user y creamos deviceModel que esta relacionado a la cuenta del usuario
        userDB = userRepository.save(userDB);
        deviceRepository.save(deviceModel);

        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(userEmail);
        if(referredByUserAOptional.isPresent()) {
            // Actualizamos el registro del referido a "Activado"
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferredStatus("Activado");
            referredByUserA.setUpdatedDate(currenDateTime);
            referredRepository.save(referredByUserA);
            
            // Crear notificación de referido, si existe el usuario A, y luego enviar notificación de mail, en caso de estar activada
            if(referredByUserA.getUserReferringStatus().equals("Activado")) {
                try {
                    String fullNameReferredUser = userData.getName() + " " + userData.getSurname();
                    String userAEmail = referredByUserA.getUserReferring();
                    UserModel userA = userRepository.findByPersonalData_Email(userAEmail).orElseThrow();
                    // Creamos notificación y se la guardamos al usuario A
                    String message = "El usuario " + fullNameReferredUser + ", se ha acaba de registrar con tu código de referidos!";
                    NotificationModel userANotifPreference = userA.getNotifPreference();
                    NotificationDataModel newNotifUserA = DataHelper.novaNotification(message, "Usuario Referido", currenDateTime);
                    userANotifPreference.addNotif(newNotifUserA);
                    userRepository.save(userA);
                    // Enviamos notificación por mail, solo si el usuario A la tiene notificación activada
                    if(userANotifPreference.isByEmail() && userANotifPreference.isReferredRegistered()) {
                        String userACodeToRefer = userA.getCodeToRefer();
                        emailAppProvider.novaUserRegisteredByCodeToRefer(userAEmail, userACodeToRefer, fullNameReferredUser);
                    }
                } catch(NoSuchElementException e) {
                    LOGGER_MESSAGES.info("No es posible identificar al usuario que ha referido");
                }
            }
        }
        return ResponseHelper.created("usuario registrado exitosamente", DataHelper.buildUser(userDB));
    }

    // SERVICIO PARA INICIO DE SESSIÓN DE UN USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponse> userLogin(UserLoginRequest requestUserLoginDto,
            HttpServletRequest request) throws JsonProcessingException {
        String email = requestUserLoginDto.email().toLowerCase();
        String pwd = requestUserLoginDto.pwd();
        UserModel userDB = this.authenticate(email, pwd);

        if(userDB != null) {
            // Usuario que al menos una vez estuvo: "Activado"
            UserDataModel userData = userDB.getPersonalData();
            LocalDateTime currentDateTime = LocalDateTime.now();
            String[] userDeviceInfo = userHelper.checkUserAgent(request, email);
            String device = userDeviceInfo[0];
            String deviceIp = userDeviceInfo[1];
            String statusUserDB = userData.getStatus();
            // Manejamos los diferentes escenarios de los estados del usuario
            switch(statusUserDB) {
                case "Activado" -> {

                    // No es un usuario de prueba y se tiene que verificar que el usuario este relacionado al dispositivo que está haciendo la consulta
                    Optional<DeviceModel> optionalDevice = deviceRepository.findByUserAndDevice(email, device);
                    if(optionalDevice.isEmpty()) {
                        // No existe dispositivo y puede que el usuario este intentando entrar desde otro, se envía código para actualizar dispositivo
                        String[] toUsers = {email};
                        String code = userData.generateRandomCode();
                        emailAppProvider.sendAuthCodeToChangeDevice(toUsers, code);
                        userData.setCodeAuth(pwdEncoder.encode(code));
                        userData.setCodeExpirationTime(currentDateTime);
                        userRepository.save(userDB);
                        String responseMessage = "se ha enviado un nuevo código de confirmación al email " + email + ", para actualizar el dispositivo relacionado a la cuenta.";
                        return ResponseHelper.imUsed(responseMessage, null);
                    } else {
                        // Se ha encontrado un dispositivo relacionado al email del usuario, además de validar sus credenciales...
                        // Se actualizan ambos tokens para iniciar una sesion limpia despues de cada login.
                        String sessionToken = JwtConfig.createSessionToken(email, Collections.singletonList(new SimpleGrantedAuthority(userData.getProfileRole())));
                        String refreshToken = JwtConfig.createRefreshToken(email);
                        DeviceModel deviceDB = optionalDevice.get();
                        userData.setSessionToken(sessionToken);
                        userData.setRefreshToken(refreshToken);
                        deviceDB.setRefreshToken(refreshToken);
                        deviceDB.setUpdatedDate(currentDateTime);
                        deviceRepository.save(deviceDB);
                        userDB = userRepository.save(userDB);
                        return ResponseHelper.ok("se ha iniciado sesión exitosamente", DataHelper.buildUser(userDB));
                    }
                }
                case "Desactivado" -> {
                    UserModel activateUser = userHelper.checkUserAccount(userRepository, deviceRepository, referredRepository, userDB, device, deviceIp);
                    if(activateUser != null) {
                        emailAppProvider.userAccountActivated(email, device, deviceIp);
                        return ResponseHelper.accepted("el usuario se ha activado nuevamente", DataHelper.buildUser(activateUser));
                    } else {
                        // El usuario deja de existir, ya que, queda obsoleto
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    }
                }
                default -> {
                    return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                }
            }
        }

        return ResponseHelper.locked("credenciales incorrectas", null);
    }

    // SERVICIO PARA CAMBIAR EL DISPOSITIVO RELACIONADO A LA CUENTA DEL USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponse> confirmDeviceChange(ConfirmUserRequest confirm, HttpServletRequest request) {
        String userEmail = confirm.email().toLowerCase();
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("") &&
                userData.getStatus().equals("Activado")) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            boolean isCodeActive = userData.isCodeActive(currentDateTime, 3);
            boolean codesMatch = pwdEncoder.matches(confirm.code(), userData.getCodeAuth());
            if(isCodeActive && codesMatch) {
                // El código no ha expirado, hace match con el código de autenticación, por lo tanto, actualizamos el dispositivo del usuario.
                String[] userDeviceInfo = userHelper.checkUserAgent(request, userEmail);
                String device = userDeviceInfo[0];
                String deviceIp = userDeviceInfo[1];
                userHelper.updateUserDevice(deviceRepository, userEmail, userData.getRefreshToken(), device, deviceIp, currentDateTime);                
                // Finalmente retornamos el usuario logeado, con el nuevo dispositivo registrado
                return ResponseHelper.ok("se ha realizado el cambio de dispositivo exitosamente", DataHelper.buildUser(userDB));
            } else {
                return ResponseHelper.gone("el código ha expirado o no es correcto", null);
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    // SERVICIOS PARA EL FLUJO DE RESTABLECIMIENTO DE LA CONTRASEÑA DEL USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponse> restorePassword(String email) {
        String userEmail = email.toLowerCase();
        // No es un usuario 'seeder', se puede seguir con la lógica
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("")) {
            // Usuario que al menos una vez estuvo: "Activado"
            String statusUserDB = userData.getStatus();
            String[] toUsers = {userEmail};
            String codeAuth = userData.generateRandomCode();
            switch(statusUserDB) {
                case "Activado" -> {
                    emailAppProvider.sendAuthCodeToRestorePassword(toUsers, codeAuth);
                    break;
                }
                case "Desactivado" -> {
                    if(userHelper.makeUserObsolete(userRepository, deviceRepository, referredRepository, userDB)) {
                        // Usuario quedo obsoleto
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    } else {
                        // Todavía se puede habilitar
                        emailAppProvider.sendAuthCodeToRestorePassword(toUsers, codeAuth);
                    }
                    break;
                }
                default -> {
                    return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                }
            }
            // Todo bien, porque el usuario está Activado o todavía se puede Habilitar.
            userData.setCodeAuth(pwdEncoder.encode(codeAuth));
            userData.setCodeExpirationTime(LocalDateTime.now());
            userRepository.save(userDB);
            return ResponseHelper.ok("se ha enviado un código de confirmación para restablecer la contraseña al email: " + userEmail, Map.of("info", "ok"));
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    @Transactional
    public ResponseEntity<GeneralResponse> confirmPasswordReset(PasswordResetRequest passwordReset, HttpServletRequest request) {
        String userEmail = passwordReset.email().toLowerCase();

        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        LocalDateTime currentDateTime = LocalDateTime.now();
        // Obtención de data para la verificación de datos
        String codeAuth = passwordReset.code();
        if(pwdEncoder.matches(codeAuth, userData.getCodeAuth()) && userData.isCodeActive(currentDateTime, 5)) {
            String newPwd = passwordReset.newPwd();
            String repeatedPwd = passwordReset.repeatedPwd();
            if(this.validateInputHelper.verifyPwd(newPwd).equals("") && !DataHelper.isNull(repeatedPwd) &&
                    newPwd.equals(repeatedPwd) && !userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("")) {
                // Usuario que al menos una vez estuvo: "Activado", y cumple con las validaciones
                String[] userDeviceInfo = userHelper.checkUserAgent(request, userEmail);
                String device = userDeviceInfo[0];
                String deviceIp = userDeviceInfo[1];
                String statusUserDB = userData.getStatus();
                switch (statusUserDB) {
                    case "Activado" -> {
                        userHelper.updateUserDevice(deviceRepository, userEmail, userData.getRefreshToken(), device, deviceIp, currentDateTime);
                        break;
                    }
                    case "Desactivado" -> {
                        userDB = userHelper.checkUserAccount(userRepository, deviceRepository, referredRepository, userDB, device, deviceIp);
                        if(userDB != null) {
                            emailAppProvider.userAccountActivated(userEmail, device, deviceIp); // Se ha vuelto ha activar el usuario
                        } else {
                            // Usuario quedo obsoleto
                            return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                        }
                        break;
                    }
                    default -> {
                        return ResponseHelper.failedDependency("datos anticuados", "failed dependency");
                    }
                }
                // Si todo va bien, el usuario está activado o se acaba de habilitar nuevamente
                userData.setPwd(pwdEncoder.encode(newPwd));
                userDB = userRepository.save(userDB);
                return ResponseHelper.ok("se ha restablecido exitosamente la contraseña del usuario", DataHelper.buildUser(userDB));
            }
        } else {
            return ResponseHelper.gone("el código ha expirado o no es correcto", null);
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    // SERVICIO PARA REENVIAR CÓDIGO DE CONFIRMACIÓN EN FLUJO ACTIVO, YA SEA DE: REGISTRAR USUARIO, CAMBIO DE DISPOSITIVO O REESTABLECIMIENTO DE LA CONTRASEÑA
    @Transactional
    public ResponseEntity<GeneralResponse> resendUserCode(String email, String type) {
        String userEmail = email.toLowerCase();
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        String userStatusDB = userData.getStatus();
        String[] toUsers = {userEmail};
        String code = userData.generateRandomCode();
        boolean isValid = false;
        // Verificamos el tipo de flujo para enviar el mail correcto
        if(!DataHelper.isNull(type)) {
            switch(type) {
                case "registerUser": {
                    if(userData.getSessionToken().equals("") && userData.getRefreshToken().equals("")
                        && userStatusDB.equals("Desactivado")) {
                        emailAppProvider.sendAuthCodeToRegisterUser(toUsers, code);
                        isValid = true;
                    }
                    break;
                }
                case "changeDevice": {
                    if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("")
                        && userStatusDB.equals("Activado")) {
                        emailAppProvider.sendAuthCodeToChangeDevice(toUsers, code);
                        isValid = true;
                    }
                    break;
                }
                case "restorePassword": {
                    if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("")) {
                        emailAppProvider.sendAuthCodeToRestorePassword(toUsers, code);
                        isValid = true;
                    }
                    break;
                }
            }
    
            if(isValid) {    
                userData.setCodeAuth(pwdEncoder.encode(code));
                userData.setCodeExpirationTime(LocalDateTime.now());
                userDB = userRepository.save(userDB);
    
                String responseMessage = "el código de confirmación se ha vuelto ha enviar al email: " + userEmail;
                return ResponseHelper.ok(responseMessage, Map.of("info", "ok"));
            }
        }
        
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", "failed dependency");
    }

    // SERVICIO PARA DESHABILITAR/ELIMINAR USUARIO DE LA APLICACIÓN
    @SuppressWarnings("null")
    @Transactional
    public ResponseEntity<GeneralResponse> disableAccount(String emailAuth) {

        // No se puede deshabilitar/eliminar, si el usuario tiene transacciones pendientes o tiene dinero disponible en su wallet
        UserModel userB = userRepository.findByPersonalData_Email(emailAuth).orElseThrow();
        if(userB.getWallet().getTotalBalance() > 0) {
            return ResponseHelper.locked("no es posible deshabilitar el usuario, porque tiene transacciones pendientes o aún hay circulación de dinero en el balance total", null);
        }
        // El usuario se puede deshabilitar con sus registros relacionados
        LocalDateTime currentDateTime = LocalDateTime.now();
        userB.getPersonalData().setStatus("Desactivado");
        userB.setDisableAccount(currentDateTime); // Se coloca la fecha que se decidió deshabilitar el usuario como cronómetro
        userRepository.save(userB);
        // Se elimina el dispositivo del usuario
        Optional<DeviceModel> deviceOptional = deviceRepository.findByUser(emailAuth);
        if(deviceOptional.isPresent()) {
            deviceRepository.delete(deviceOptional.get());
        }
        // Se recupera la data de referidos para desactivar los registros
        List<ReferredModel> updateTheReferreds = new ArrayList<>();
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(emailAuth);
        if(referredByUserAOptional.isPresent()) {
            ReferredModel referredByUserA = referredByUserAOptional.get();
            referredByUserA.setReferredStatus("Desactivado");
            referredByUserA.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(referredByUserA);
        }
        List<ReferredModel> usersC = referredRepository.findAllByUserReferring(emailAuth);
        for(ReferredModel userC : usersC) {
            userC.setUserReferringStatus("Desactivado");
            userC.setUpdatedDate(currentDateTime);
            updateTheReferreds.add(userC);
        }
        if(updateTheReferreds.size() > 0) {
            referredRepository.saveAll(updateTheReferreds);
        }
        emailAppProvider.userAccountDisabled(emailAuth, currentDateTime);
        return ResponseHelper.ok("la cuenta del usuario se ha deshabilitado por un rango de 30 días, luego del tiempo estipulado, si no hay actividad la cuenta se eliminará definitivamente", Map.of("info", "ok"));
    }

    // FUNCIONES DE APOYO PARA LOS FLUJOS, LÓGICA, IMPLEMENTACIÓN DE REGLA DE NEGOCIOS...
    @Transactional(readOnly = true)
    private String[] validateCodeToRefer(String codeToRefer) {
        if(DataHelper.isNull(codeToRefer)) {
            return new String[]{"Sin usuario", "Sin usuario"};
        }
        Optional<UserModel> userOptional = userRepository.findByCodeToRefer(codeToRefer);
        if(userOptional.isPresent()) {
            // Fijarnos que el usuario este habilitado
            UserDataModel userData = userOptional.get().getPersonalData();
            if(userData.getStatus().equals("Activado")) {
                String userReferring = userData.getEmail();
                return new String[]{userReferring, codeToRefer};
            }
        }
        return null;
    }

    private UserDataModel createUserData(String name, String surname, String pwd, String email, String profileRole) {
        return new UserDataModel(name, surname, email, "", "", DataHelper.deprecatedDate(), "Desactivado", new byte[0],
                    pwdEncoder.encode(pwd), "", profileRole, "", DataHelper.deprecatedDateTime(), "", "");
    }

    @Transactional(readOnly = true)
    private UserModel authenticate(String email, String pwd) {
        Optional<UserModel> optionalUser = userRepository.findByPersonalData_Email(email);
        if(optionalUser.isPresent()) {
            UserModel userDB = optionalUser.get();
            UserDataModel userData = userDB.getPersonalData();
            if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("")
                    && pwdEncoder.matches(pwd, userData.getPwd())) {
                return userDB;
            }
        }
        return null;
    }

    // SERVICIO SUPUESTO PARA CREAR USUARIO ADMINISTRADOR, NO IMPLEMENTADO
    public ResponseEntity<GeneralResponse> userSave(UserRegisterRequest userRegister) {
        // Luego de ser validados los primeros datos, se valida el código de referido para saber si se puede continuar
        String[] userReferring = this.validateCodeToRefer(userRegister.codeToRefer());
        if(userReferring == null) {
            return ResponseHelper.locked("el código del referido es inválido", null);
        }
        // El código del referido es correcto y ahora se crea la estructura del usuario
        UserDataModel userData = this.createUserData(userRegister.name().strip(), userRegister.surname().strip(), // Usamos strip() para quitar espacios al inicio y final
                userRegister.pwd(), userRegister.email().toLowerCase(), userRegister.profileRole()); // Dejamos email en minúsculas
        WalletModel wallet = new WalletModel(0, 0, 0, 0);
        NotificationModel notifs = new NotificationModel(true, true, true,
                false, false, true, false, false, false, new ArrayList<>());
        return this.createUnconfirmedUser(userReferring, userData, wallet, notifs);
    }

}
