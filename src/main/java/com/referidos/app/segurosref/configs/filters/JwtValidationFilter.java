package com.referidos.app.segurosref.configs.filters;

import static com.referidos.app.segurosref.configs.JwtConfig.*;
// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.configs.SimpleGrantedAuthorityJsonCreator;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.UserRepository;
import com.referidos.app.segurosref.seeder.RunUserSeeder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// FILTRO DE AUTENTICACIÓN PARA: VALIDAR EL TOKEN DE LA SOLICITUD
@Component
public class JwtValidationFilter extends BasicAuthenticationFilter {

    private UserRepository userRepository;

    private DeviceRepository deviceRepository;

    // En los filtros de autenticación debemos entregar el objeto AuthenticationManager
    public JwtValidationFilter(AuthenticationManager authenticationManager, UserRepository userRepository,
            DeviceRepository deviceRepository) {
        super(authenticationManager);
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Por el filtro de permitir dispositivos (que se ejecuta primero), este header no es nulo
        String device = request.getHeader("User-Agent");
        String refreshToken = request.getHeader("Refresh-Token");
        String endpoint = request.getRequestURI();

        // Ignorar rutas pÃºblicas
        if (endpoint.equals("/") || endpoint.equals("/moneyfy/") || endpoint.startsWith("/auth")
                || endpoint.startsWith("/swagger-ui") || endpoint.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        // Revisar si es el endpoint de cotización para validarlo / sin actualizar credenciales
        if(endpoint.contains("/quoter/search/plan")) {
            this.validatePlanFinder(request, response, chain, device, refreshToken);
            return;
        }

        // // En caso de que la URL NO sea un endpoint especial (o sea puede que sea probablemente privado), y el dispositivo
        // // que esta realizando la consulta NO está registrado en la base de datos, entonces, no se puede seguir con la
        // // solicitud, porque el dispositivo debería estar registrado.
        // if(!this.specialEndpoints(endpoint) && !deviceRepository.existsByDevice(device)) {
        //     // LOGGER_MESSAGES.info("\n-----\nURL: " + endpoint + "\n-----");
        //     ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        //     return;
        // }

        // Obtener token para autenticar
        String tokenHeader = request.getHeader(HEADER_AUTHORIZATION);
        if(tokenHeader == null || !tokenHeader.startsWith(PREFIX_TOKEN) || DataHelper.isNull(refreshToken)) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
            return;
        }
        String sessionToken = tokenHeader.replace(PREFIX_TOKEN, "");

        try {
            // Luego de tener el token, obtenemos los claims/payload del token, para validar info
            Claims claims = JwtConfig.obtainClaims(sessionToken);
            String userEmail = JwtConfig.getSubject(claims);
            // Buscamos usuario "Activado" y dispositivo relacionado al usuario que encontramos en el session token
            Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);
            if(userOptional.isPresent() && userOptional.get().getPersonalData().getStatus().equals("Activado")) {
                if(RunUserSeeder.isTestUser(userEmail) || RunUserSeeder.isDefaulUser(userEmail)) {
                    // Es un usuario con dispositivo dinámico y está activado, así que, se autentica
                    this.authDefaultUsers(request, response, chain, userEmail);
                    return;
                }
                Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(userEmail, device);
                if(deviceOptional.isPresent()) {
                    DeviceModel deviceDB = deviceOptional.get();
                    // Actualizar ips de dispositivo verificado, si es el caso
                    String ipAddress = !DataHelper.isNull(request.getRemoteAddr()) ? request.getRemoteAddr() : "desconocido";
                    if(!ipAddress.equals("desconocido") && !deviceDB.getIps().contains(ipAddress)) {
                        deviceDB.addIp(ipAddress);
                        deviceDB.setUpdatedDate(LocalDateTime.now());
                        deviceRepository.save(deviceDB);
                    }
                    // Generamos objeto de autorización para permitir al usuario realizar la solicitud (credenciales actualizadas)
                    String strAuthorities = JwtConfig.getClaim(claims, "authorities");
                    Collection<? extends GrantedAuthority> authorities = Arrays.asList(new ObjectMapper()
                            .addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityJsonCreator.class)
                            .readValue(strAuthorities.getBytes(), SimpleGrantedAuthority[].class));
                    // El "Updated", es porque no se tiene que actualizar el token de refresco
                    Authentication auth = new UsernamePasswordAuthenticationToken(userEmail, "Updated", authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    chain.doFilter(request, response);
                    return;
                }   
            }
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch(SignatureException | ExpiredJwtException e) {
            // Obtener token de refresco para validar token y dispositivo que esta realizando la consulta
            this.checkRefreshToken(request, response, chain, device, refreshToken);
        } catch(JwtException e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        }
    }

    // NO SE ACTUALIZA EL SESSION TOKEN
    @Transactional(readOnly = true)
    private void validatePlanFinder(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String device, String refreshToken) throws IOException, ServletException {
        String ipAddress = !DataHelper.isNull(request.getRemoteAddr()) ? request.getRemoteAddr() : "desconocido";
        LocalDateTime currentDateTime = LocalDateTime.now();
        try {
            Claims claims = JwtConfig.obtainClaims(refreshToken);
            String user = JwtConfig.getClaim(claims, "user");
            // Buscamos usuario "Activado" y dispositivo relacionado al usuario que encontramos en el refresh token
            UserModel userDB = userRepository.findByPersonalData_Email(user).orElseThrow();
            UserDataModel userData = userDB.getPersonalData();
            if(!userData.getStatus().equals("Activado")) {
                ResponseHelper.invalidJWT(response, "datos anticuados", null);
                return;
            }

            // Verificamos si es un usuario de prueba o por defecto, para autenticarlo rápidamente
            if(RunUserSeeder.isTestUser(user) || RunUserSeeder.isDefaulUser(user)) {
                // Es un usuario con dispositivo dinámico y está activado, así que, se autentica
                this.authDefaultUsers(request, response, chain, user);
                return;
            }

            DeviceModel deviceDB = deviceRepository.findByUserAndDevice(user, device).orElseThrow();
            // Actualizar ips de dispositivo verificado, si es el caso
            if(!ipAddress.equals("desconocido") && !deviceDB.getIps().contains(ipAddress)) {
                deviceDB.addIp(ipAddress);
                deviceDB.setUpdatedDate(currentDateTime);
                deviceRepository.save(deviceDB);
            }
            // Generamos objeto de autorización por el usuario verificado (credenciales actualizadas).
            String userRole = userData.getProfileRole();
            Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(userRole));
            // El "Updated", es porque no se tiene que actualizar el token de refresco
            Authentication auth = new UsernamePasswordAuthenticationToken(user, "Updated", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch(SignatureException | ExpiredJwtException e) {
            // Buscamos dispositivo relacionado al refreshToken del usuario y un usuario "Activado".
            Optional<DeviceModel> deviceOptional2 = deviceRepository.findByDeviceAndRefreshToken(device, refreshToken);
            if(deviceOptional2.isPresent()) {
                DeviceModel deviceDB2 = deviceOptional2.get();
                String userEmail = deviceDB2.getUser();
                Optional<UserModel> userOptional2 = userRepository.findByPersonalData_Email(userEmail);
                if(userOptional2.isPresent() && userOptional2.get().getPersonalData().getStatus().equals("Activado")) {
                    // Actualizar ips de dispositivo verificado, si es el caso
                    if(!ipAddress.equals("desconocido") && !deviceDB2.getIps().contains(ipAddress)) {
                        deviceDB2.addIp(ipAddress);
                        deviceDB2.setUpdatedDate(currentDateTime);
                        deviceRepository.save(deviceDB2);
                    }
                    // Generamos objeto de autorización por el usuario verificado (credenciales actualizadas, porque
                    // en el endpoint de cotización no se deberían actualizar los tokens).
                    String userRole2 = userOptional2.get().getPersonalData().getProfileRole();
                    Collection<? extends GrantedAuthority> authorities2 = Collections.singletonList(new SimpleGrantedAuthority(userRole2));
                    // El "Updated", es porque no se tiene que actualizar el token de refresco
                    Authentication auth2 = new UsernamePasswordAuthenticationToken(userEmail, "Updated", authorities2);
                    SecurityContextHolder.getContext().setAuthentication(auth2);
                    chain.doFilter(request, response);
                    return;
                }
            } else {
                // Puede que el dispositivo sea dinámico
                Optional<DeviceModel> deviceUserOptional = deviceRepository.findByRefreshToken(refreshToken);
                if(deviceUserOptional.isPresent()) {
                    String defaultUser = deviceUserOptional.get().getUser();
                    
                    // Verificamos si es un usuario de prueba o por defecto, para autenticarlo rápidamente
                    if(RunUserSeeder.isTestUser(defaultUser) || RunUserSeeder.isDefaulUser(defaultUser)) {
                        // Buscamos al usuario
                        Optional<UserModel> defaultUserOptinal = userRepository.findByPersonalData_Email(defaultUser);
                        if(defaultUserOptinal.isPresent() && defaultUserOptinal.get().getPersonalData().getStatus().equals("Activado")) {
                            // Es un usuario con dispositivo dinámico que se encuentra activo, así que, se autentica
                            this.authDefaultUsers(request, response, chain, defaultUser);
                            return;
                        }
                    }

                }
            }
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch(JwtException e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch(Exception e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        }
    }

    // Verificar token de refresco si es correcto para actualizar el token de expiración
    @Transactional
    private void checkRefreshToken(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String device, String refreshToken) throws IOException, ServletException {
        String ipAddress = !DataHelper.isNull(request.getRemoteAddr()) ? request.getRemoteAddr() : "desconocido";
        LocalDateTime currentDateTime = LocalDateTime.now();
        // LOGGER_MESSAGES.info("\n-----\nValor token de refresco: " + refreshToken + "\n-----");
        try {
            Claims claims = JwtConfig.obtainClaims(refreshToken);
            String user = JwtConfig.getClaim(claims, "user");
            // Buscamos usuario "Activado" y dispositivo relacionado al usuario que encontramos en el refresh token
            UserModel userDB = userRepository.findByPersonalData_Email(user).orElseThrow();
            UserDataModel userData = userDB.getPersonalData();
            // Generamos objeto de autorización por el usuario verificado (se necesita actualizar el sessionToken,
            // pero, el refreshToken está actualizado).
            Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(userData.getProfileRole()));
            userData.setSessionToken(JwtConfig.createSessionToken(user, authorities));
            if(!userData.getStatus().equals("Activado")) {
                ResponseHelper.invalidJWT(response, "datos anticuados", null);
                return;
            }

            // Verificamos si es un usuario de prueba o por defecto, para autenticarlo rápidamente
            if(RunUserSeeder.isTestUser(user) || RunUserSeeder.isDefaulUser(user)) {
                // Es un usuario con dispositivo dinámico y está activado, así que, se autentica
                userRepository.save(userDB);
                this.authDefaultUsers(request, response, chain, user);
                return;
            }

            DeviceModel deviceDB = deviceRepository.findByUserAndDevice(user, device).orElseThrow();
            // Actualizar ips del dispositivo, si es el caso
            if(!ipAddress.equals("desconocido") && !deviceDB.getIps().contains(ipAddress)) {
                deviceDB.addIp(ipAddress);
                deviceDB.setUpdatedDate(currentDateTime);
                deviceRepository.save(deviceDB);
            }
            userRepository.save(userDB);
            // El "Updated", es porque no se tiene que actualizar el token de refresco
            Authentication auth = new UsernamePasswordAuthenticationToken(user, "Updated", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch(SignatureException | ExpiredJwtException e) {
            // Buscamos dispositivo relacionado al refreshToken del usuario y un usuario "Activado".
            Optional<DeviceModel> deviceOptional = deviceRepository.findByDeviceAndRefreshToken(device, refreshToken);
            if(deviceOptional.isPresent()) {
                DeviceModel deviceDB2 = deviceOptional.get();
                String user2 = deviceDB2.getUser();
                Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(user2);
                if(userOptional.isPresent() && userOptional.get().getPersonalData().getStatus().equals("Activado")) {
                    // Actualizar ips de dispositivo verificado, si es el caso
                    if(!ipAddress.equals("desconocido") && !deviceDB2.getIps().contains(ipAddress)) {
                        deviceDB2.addIp(ipAddress);
                        deviceDB2.setUpdatedDate(currentDateTime);
                        deviceRepository.save(deviceDB2);
                    }
                    // Generamos objeto de autorización por el usuario verificado (se necesita actualizar el sessionToken,
                    // y el refresh token). Aunque, el refresh token solo se actualiza, si la solicitud es aceptada.
                    UserModel userDB2 = userOptional.get();
                    UserDataModel userData2 = userDB2.getPersonalData();
                    Collection<? extends GrantedAuthority> authorities2 = Collections.singletonList(new SimpleGrantedAuthority(userData2.getProfileRole()));
                    userData2.setSessionToken(JwtConfig.createSessionToken(user2, authorities2));
                    userRepository.save(userDB2);
                    // El "Dated", es porque se tiene que actualizar el token de refresco, si la solicitud es aceptada.
                    Authentication auth2 = new UsernamePasswordAuthenticationToken(user2, "Dated", authorities2);
                    SecurityContextHolder.getContext().setAuthentication(auth2);
                    chain.doFilter(request, response);
                    return;
                }
            } else {
                // Puede que el dispositivo sea dinámico
                Optional<DeviceModel> deviceOptional2 = deviceRepository.findByRefreshToken(refreshToken);
                if(deviceOptional2.isPresent()) {
                    String defaultUser = deviceOptional2.get().getUser();
                    
                    // Verificamos si es un usuario de prueba o por defecto, para autenticarlo rápidamente
                    if(RunUserSeeder.isTestUser(defaultUser) || RunUserSeeder.isDefaulUser(defaultUser)) {
                        // Buscamos al usuario
                        Optional<UserModel> defaultUserOptinal = userRepository.findByPersonalData_Email(defaultUser);
                        if(defaultUserOptinal.isPresent() && defaultUserOptinal.get().getPersonalData().getStatus().equals("Activado")) {
                            // Es un usuario con dispositivo dinámico que se encuentra activo, así que, se autentica
                            UserModel defaultUserDB = defaultUserOptinal.get();
                            UserDataModel defaultUserData = defaultUserDB.getPersonalData();
                            defaultUserData.setSessionToken(JwtConfig.createSessionToken(defaultUser, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))));
                            userRepository.save(defaultUserDB);
                            this.authDefaultUsers(request, response, chain, defaultUser);
                            return;
                        }
                    }

                }
            }
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch(Exception e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        }
    }

    // private boolean specialEndpoints(String url) {
    //     String[] specialEndpoints = {"/auth/register", "/auth/confirm/registration", "/auth/log-in", "/auth/confirm/device/change",
    //             "/auth/restore/password", "/auth/confirm/password/reset", "/auth/resend/code", "/quoter/register/vehicle/brands",
    //             "/quoter/register/insurer", "/quoter/view/test/data", "/quoter/test/nova/functions", "/quoter/commission/report",
    //             "/quoter/commission/payments", "/logs/find/all", "/logs/notify/account/not-found", "/logs/update",
    //             "/cities/register", "/swagger-ui", "/v3/api-docs"}; // Todos contienen la barra lateral
    //     for(String endpoint : specialEndpoints) {
    //         if(url.contains(endpoint)) {
    //             return true;
    //         }
    //     }
    //     return url.equals("/") || url.equals("/segurosref/");
    // }

    // FLUJO DE AUTENTICACIÓN PARA USUARIOS DE PRUEBA O USUARIOS POR DEFECTO
    public void authDefaultUsers(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String emailAuth) throws IOException, ServletException {
        // El "Updated", es porque no se tiene que actualizar el token de refresco
        Authentication auth = new UsernamePasswordAuthenticationToken(emailAuth, "Updated", Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

}
