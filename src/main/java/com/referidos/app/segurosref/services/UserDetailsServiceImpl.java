package com.referidos.app.segurosref.services;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.NotificationDataModel;
import com.referidos.app.segurosref.models.NotificationModel;
import com.referidos.app.segurosref.models.ReferredModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.models.WalletModel;
import com.referidos.app.segurosref.models.WhiteListModel;
import com.referidos.app.segurosref.provider.EmailServiceProvider;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.ReferredRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.repositories.WhiteListRepository;
import com.referidos.app.segurosref.requests.ConfirmUserRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.seeder.RunUserSeeder;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private WhiteListRepository whiteListRepository;

    @Autowired
    private ReferredRepository referredRepository;

    @Autowired
    private EmailServiceProvider emailProvider;

    @Autowired
    private ValidateInputHelper validateInputHelper;

    @Autowired
    private UserHelper userHelper;

    @Autowired
    private PasswordEncoder pwdEncoder;

    // @Autowired
    // private ComplexQueryProvider complexQueryProvider;

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
    public ResponseEntity<GeneralResponses> userRegister(UserRegisterRequest userRegister) {
        // Luego de ser validados los primeros datos, se valida el código de referido para saber si se puede continuar
        String[] userReferring = this.validateCodeToRefer(userRegister.codeToRefer());
        if(userReferring == null) {
            return ResponseHelper.locked("el código del referido es inválido", null);
        }
        // El código del referido es correcto y ahora se crea la estructura del usuario,
        UserDataModel userData = this.createUserData(userRegister.name().trim(), userRegister.surname().trim(), // CON TRIM() INCLUIDO (No permite saltos en línea)
                userRegister.pwd(), userRegister.email().toLowerCase(), "ROLE_USER");  // Dejamos email en minúsculas
        WalletModel wallet = new WalletModel(0, 0, 0, 0);
        NotificationModel notifs = new NotificationModel(false, true, true,
                false, false, true, false, false, false, new ArrayList<>());
        return this.createUnconfirmedUser(userReferring, userData, wallet, notifs);
    }

    @Transactional
    private ResponseEntity<GeneralResponses> createUnconfirmedUser(String[] userReferring, UserDataModel userData,
            WalletModel wallet, NotificationModel notifs) {
        // El código del referido ha sido validado en caso de que venga o sino su valor es "Sin Usuario". Ahora se
        // valida si el usuario ya existe.
        LocalDateTime currenDateTime = LocalDateTime.now();
        String email = userData.getEmail(); // El email ya está en minúsculas
        Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(email);
        if(userOptional.isPresent()) {
            UserModel userDB = userOptional.get();
            UserDataModel userDataDB = userDB.getPersonalData();
            if(!userDataDB.getSessionToken().equals("") || !userDataDB.getRefreshToken().equals("")) {
                // Usuario que al menos una vez estuvo: "Activado"
                String statusUserDB = userDataDB.getStatus();
                switch(statusUserDB) {
                    case "Activado" -> {
                        return ResponseHelper.gone("usuario existente", null);
                    }
                    case "Desactivado" -> {
                        if(!userHelper.makeUserObsolete(userRepository, deviceRepository, whiteListRepository, referredRepository, userDB)) {
                            // El usuario no ha quedado obsoleto, por lo tanto, aún se puede habilitar
                            return ResponseHelper.gone("usuario existente", null);
                        }
                        // Usuario quedo obsoleto y, por lo tanto, se puede seguir con el flujo
                        break;
                    }
                    default -> {
                        // No se puede crear un usuario con el email de un usuario obsoleto, porque la estructura no ee
                        // correcta.
                        return ResponseHelper.failedDependency("datos anticuados", null);
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
        emailProvider.sendAuthCodeToRegisterUser(toUsers, codeAuth);

        // Se genera el nuevo usuario (no confirmado), además del registro del referido...
        String userReferringState = (userReferring[0].equals("Sin usuario")) ? "Desactivado" : "Activado";
        userData.setCodeAuth(pwdEncoder.encode(codeAuth));
        userData.setCodeExpirationTime(currenDateTime); // Se establece el tiempo actual al código de confirmación que tiene una validad de 3 minutos
        UserModel userModel = new UserModel("", DataHelper.deprecatedDateTime(), userData, wallet, notifs);
        ReferredModel referredModel = new ReferredModel(userReferring[0], userReferring[1], email, userReferringState, "Activado", currenDateTime, currenDateTime);
        userRepository.save(userModel);
        referredRepository.save(referredModel);

        String responseMessage = "el código de confirmación para finalizar el proceso de registro, ha sido enviado al email: " + email ;
        return ResponseHelper.ok(responseMessage, Map.of("info", "ok"));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<GeneralResponses> confirmRegistration(ConfirmUserRequest confirm,
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
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    @Transactional
    private ResponseEntity<GeneralResponses> successfulRegistration(UserModel userDB, HttpServletRequest request) throws JsonProcessingException {
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
        
        // En filtro ya se verifica que el dispositvo no sea nulo
        String device = request.getHeader("User-Agent");
        String firstIp = (!DataHelper.isNull(request.getRemoteAddr())) ? request.getRemoteAddr() : "desconocido";
        LocalDateTime currenDateTime = LocalDateTime.now();
        // Se relaciona el registro del usuario con el dispositivo que hizo la consulta de confirmar registro
        DeviceModel deviceModel = new DeviceModel(device, userEmail, refreshToken, Collections.singleton(firstIp), currenDateTime, currenDateTime);
        
        // Actualizamos el registro de user y creamos deviceModel que esta relacionado a la cuenta del usuario
        userDB = userRepository.save(userDB);
        deviceRepository.save(deviceModel);

        // Enviamos email, si se registro con el código de un sugerido
        Optional<ReferredModel> referredByUserAOptional = referredRepository.findByReferred(userEmail);
        if(referredByUserAOptional.isPresent() && referredByUserAOptional.get().getUserReferringStatus().equals("Activado")) {
            String userAEmail = referredByUserAOptional.get().getUserReferring();
            Optional<UserModel> userAOptional = userRepository.findByPersonalData_Email(userAEmail);
            if(userAOptional.isPresent()) {
                // Enviar email al usuario A, por un nuevo usuario que se registro con su código de referidos, además de
                // agregarlo a su estructura de notificaciones.
                UserModel userA = userAOptional.get();
                String userACodeToRefer = userA.getCodeToRefer();
                String fullNameReferredUser = userData.getName() + " " + userData.getSurname();
                emailProvider.novaUserRegisteredByCodeToRefer(userAEmail, userACodeToRefer, fullNameReferredUser);
                String message = "El usuario " + fullNameReferredUser + ", se ha acaba de registrar con tu código de referidos!";
                NotificationDataModel notifUserA = DataHelper.novaNotification(message, "Usuario Referido", currenDateTime);
                userA.getNotifPreference().addNotif(notifUserA);
                userRepository.save(userA);
            }
        }
        
        return ResponseHelper.created("usuario registrado exitosamente", DataHelper.buildUser(userDB));
    }

    // SERVICIO PARA INICIO DE SESSIÓN DE UN USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponses> userLogin(UserLoginRequest requestUserLoginDto,
            HttpServletRequest request) throws JsonProcessingException {
        String email = requestUserLoginDto.email().toLowerCase();
        String pwd = requestUserLoginDto.pwd();
        UserModel userDB = this.authenticate(email, pwd);

        if(userDB != null) {
            // Usuario que al menos una vez estuvo: "Activado"
            UserDataModel userData = userDB.getPersonalData();
            LocalDateTime currentDateTime = LocalDateTime.now();
            String device = request.getHeader("User-Agent");
            String deviceIp = (!DataHelper.isNull(request.getRemoteAddr())) ? request.getRemoteAddr() : "desconocido";
            String statusUserDB = userData.getStatus();
            // Manejamos los diferentes escenarios de los estados del usuario
            switch(statusUserDB) {
                case "Activado" -> {
                     // Antes verificamos que no sea un usuario de prueba o un usuario por defecto
                    if(RunUserSeeder.isTestUser(email) || RunUserSeeder.isDefaulUser(email)) {
                        // Actualizamos/generamos el dispositivo del usuario "seeder", e iniciamos sesión
                        userHelper.updateUserDevice(deviceRepository, email, userData.getRefreshToken(), device, deviceIp, currentDateTime);
                        return ResponseHelper.ok("se ha iniciado sesión exitosamente con usuario de prueba", DataHelper.buildUser(userDB));
                    }
                    Optional<DeviceModel> optionalDevice = deviceRepository.findByUserAndDevice(email, device);
                    if(optionalDevice.isEmpty()) {
                        String[] toUsers = {email};
                        String code = userData.generateRandomCode();
                        emailProvider.sendAuthCodeToChangeDevice(toUsers, code);
        
                        // Actualizar datos de código
                        userData.setCodeAuth(pwdEncoder.encode(code));
                        userData.setCodeExpirationTime(currentDateTime);
                        userRepository.save(userDB);
        
                        String responseMessage = "se ha enviado un nuevo código de confirmación al email " + email + ", para actualizar el dispositivo relacionado a la cuenta.";
                        return ResponseHelper.imUsed(responseMessage, null);
                    } else {
                        // Se ha encontrado un dispositivo relacionado al email del usuario, además de validar sus credenciales...
                        // Se actualiza su token de session, el filtro maneja el token de refresco para actualizarlo
                        userData.setSessionToken(JwtConfig.createSessionToken(email, Collections.singletonList(new SimpleGrantedAuthority(userData.getProfileRole()))));
                        userDB = userRepository.save(userDB);
            
                        return ResponseHelper.ok("se ha iniciado sesión exitosamente", DataHelper.buildUser(userDB));
                    }
                }
                case "Desactivado" -> {
                    UserModel activateUser = userHelper.checkUserAccount(userRepository, deviceRepository, whiteListRepository, referredRepository, userDB, device, deviceIp);
                    if(activateUser != null) {
                        emailProvider.userAccountActivated(email, device, deviceIp);
                        return ResponseHelper.accepted("el usuario se ha activado nuevamente", DataHelper.buildUser(activateUser));
                    } else {
                        // El usuario deja de existir, ya que, queda obsoleto
                        return ResponseHelper.failedDependency("datos anticuados", null);
                    }
                }
                default -> {
                    return ResponseHelper.failedDependency("datos anticuados", null);
                }
            }
        }

        return ResponseHelper.locked("credenciales incorrectas", null);
    }

    // SERVICIO PARA CAMBIAR EL DISPOSITIVO RELACIONADO A LA CUENTA DEL USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponses> confirmDeviceChange(ConfirmUserRequest confirm, HttpServletRequest request) {
        String userEmail = confirm.email().toLowerCase();
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("") &&
                userData.getStatus().equals("Activado")) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            boolean isCodeActive = userData.isCodeActive(currentDateTime, 3);
            boolean codesMatch = pwdEncoder.matches(confirm.code(), userData.getCodeAuth());
            if(isCodeActive && codesMatch) {
                // El código no ha expirado, hace match con el código de autenticación, por lo tanto, actualizamos el
                // dispositivo del usuario.
                String device = request.getHeader("User-Agent");
                String deviceIp = (!DataHelper.isNull(request.getRemoteAddr())) ? request.getRemoteAddr() : "desconocido";
                userHelper.updateUserDevice(deviceRepository, userEmail, userData.getRefreshToken(), device, deviceIp, currentDateTime);                
                // Finalmente retornamos el usuario logeado, con el nuevo dispositivo registrado
                return ResponseHelper.ok("se ha realizado el cambio de dispositivo exitosamente", DataHelper.buildUser(userDB));
            } else {
                return ResponseHelper.gone("el código ha expirado o no es correcto", null);
            }
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    // SERVICIOS PARA EL FLUJO DE RESTABLECIMIENTO DE LA CONTRASEÑA DEL USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponses> restorePassword(String email) {
        // Verificamos primero si es un usuario de prueba
        String userEmail = email.toLowerCase();
        if(RunUserSeeder.isTestUser(userEmail)) {
            return ResponseHelper.failedDependency("el usuario de prueba, no puede reestablecer su contraseña", null);
        }
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
                    emailProvider.sendAuthCodeToRestorePassword(toUsers, codeAuth);
                    break;
                }
                case "Desactivado" -> {
                    if(userHelper.makeUserObsolete(userRepository, deviceRepository, whiteListRepository, referredRepository, userDB)) {
                        // Usuario quedo obsoleto
                        return ResponseHelper.failedDependency("datos anticuados", null);
                    } else {
                        // Todavía se puede habilitar
                        emailProvider.sendAuthCodeToRestorePassword(toUsers, codeAuth);
                    }
                    break;
                }
                default -> {
                    return ResponseHelper.failedDependency("datos anticuados", null);
                }
            }
            // Todo bien, porque el usuario está Activado o todavía se puede Habilitar.
            userData.setCodeAuth(pwdEncoder.encode(codeAuth));
            userData.setCodeExpirationTime(LocalDateTime.now());
            userRepository.save(userDB);
            return ResponseHelper.ok("se ha enviado un código de confirmación para restablecer la contraseña al email: " + userEmail, Map.of("info", "ok"));
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    @Transactional
    public ResponseEntity<GeneralResponses> confirmPasswordReset(PasswordResetRequest passwordReset, HttpServletRequest request) {
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
                String device = request.getHeader("User-Agent");
                String deviceIp = (!DataHelper.isNull(request.getRemoteAddr())) ? request.getRemoteAddr() : "desconocido";
                String statusUserDB = userData.getStatus();
                switch (statusUserDB) {
                    case "Activado" -> {
                        userHelper.updateUserDevice(deviceRepository, userEmail, userData.getRefreshToken(), device, deviceIp, currentDateTime);
                        break;
                    }
                    case "Desactivado" -> {
                        // Ya se valido en el paso anterior que el usuario puede volver a habilitarse: "restorePassword"
                        userDB = userHelper.enableUserAccount(userRepository, referredRepository, deviceRepository, userDB, device, deviceIp, currentDateTime);
                        emailProvider.userAccountActivated(userEmail, device, deviceIp); // Se ha vuelto ha activar el usuario
                        break;
                    }
                    default -> {
                        return ResponseHelper.failedDependency("datos anticuados", null);
                    }
                }
                // Si todo va bien, actualizamos la contraseña
                userData.setPwd(pwdEncoder.encode(newPwd));
                userDB = userRepository.save(userDB);
                return ResponseHelper.ok("se ha restablecido exitosamente la contraseña del usuario", DataHelper.buildUser(userDB));
            }
        } else {
            return ResponseHelper.gone("el código ha expirado o no es correcto", null);
        }
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    // SERVICIO PARA REENVIAR CÓDIGO DE CONFIRMACIÓN EN FLUJO ACTIVO, YA SEA DE: REGISTRAR USUARIO, CAMBIO DE DISPOSITIVO O REESTABLECIMIENTO DE LA CONTRASEÑA
    @Transactional
    public ResponseEntity<GeneralResponses> resendUserCode(String email, String type) {
        // Verificamos primero si es un usuario de prueba
        String userEmail = email.toLowerCase();
        if(RunUserSeeder.isTestUser(userEmail)) {
            return ResponseHelper.failedDependency("el usuario de prueba, no puede obtener códigos de confirmación", null);
        }
        UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
        UserDataModel userData = userDB.getPersonalData();
        String userStatusDB = userData.getStatus();
        String[] toUsers = {userEmail};
        String code = userData.generateRandomCode();
        boolean isValid = false;

        // Hacer una validación que general que el código de expiración del usuario sea haya creado como máximo
        // hace 6 horas, para mayor seguridad.

        if(!DataHelper.isNull(type) && (userStatusDB.equals("Activado") || userStatusDB.equals("Desactivado"))) {
            switch(type) {
                case "registerUser": {
                    if(userData.getSessionToken().equals("") && userData.getRefreshToken().equals("")) {
                        emailProvider.sendAuthCodeToRegisterUser(toUsers, code);
                        isValid = true;
                    }
                    break;
                }
                case "changeDevice": {
                    if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("") &&
                            userStatusDB.equals("Activado")) {
                        emailProvider.sendAuthCodeToChangeDevice(toUsers, code);
                        isValid = true;
                    }
                    break;
                }
                case "restorePassword": {
                    if(!userData.getSessionToken().equals("") && !userData.getRefreshToken().equals("")) {
                        emailProvider.sendAuthCodeToRestorePassword(toUsers, code);
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
        
        return ResponseHelper.failedDependency("no es posible continuar con la solicitud", null);
    }

    // SERVICIO PARA DESHABILITAR/ELIMINAR USUARIO DE LA APLICACIÓN
    @Transactional
    public ResponseEntity<GeneralResponses> disableAccount(String emailAuth, String device) {
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
        // Se verifica si existe una lista en el white list
        Optional<WhiteListModel> whiteListOptional = whiteListRepository.findByUser(emailAuth);
        if(whiteListOptional.isPresent()) {
            whiteListRepository.delete(whiteListOptional.get());
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
        emailProvider.userAccountDisabled(emailAuth, currentDateTime);
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
    public ResponseEntity<GeneralResponses> userSave(UserRegisterRequest userRegister) {
        // Luego de ser validados los primeros datos, se valida el código de referido para saber si se puede continuar
        String[] userReferring = this.validateCodeToRefer(userRegister.codeToRefer());
        if(userReferring == null) {
            return ResponseHelper.locked("el código del referido es inválido", null);
        }
        // El código del referido es correcto y ahora se crea la estructura del usuario
        UserDataModel userData = this.createUserData(userRegister.name().trim(), userRegister.surname().trim(), // CON TRIM() INCLUIDO (No permite saltos en línea)
                userRegister.pwd(), userRegister.email().toLowerCase(), userRegister.profileRole()); // Dejamos email en minúsculas
        WalletModel wallet = new WalletModel(0, 0, 0, 0);
        NotificationModel notifs = new NotificationModel(false, true, true,
                false, false, true, false, false, false, new ArrayList<>());
        return this.createUnconfirmedUser(userReferring, userData, wallet, notifs);
    }

}
