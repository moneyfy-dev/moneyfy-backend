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
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.configs.SimpleGrantedAuthorityJsonCreator;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.FilterHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.UserDataModel;
import com.referidos.app.segurosref.models.UserModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// FILTRO DE AUTENTICACIÓN PARA: VALIDAR EL TOKEN DE LA SOLICITUD
public class JwtValidationFilter extends BasicAuthenticationFilter {

    private UserRepository userRepository;

    private DeviceRepository deviceRepository;

    // En los filtros de autenticación debemos entregar el objeto
    // AuthenticationManager
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

        response.addHeader("X-JWT-Filter", "hit");
        // Ignorar rutas públicas
        if (FilterHelper.checkPublicRoute(request)) {
            chain.doFilter(request, response);
            return;
        }

        // "Updated" en objeto de autorización se utiliza para confirmar que el usuario
        // tiene el token de refresco actualizado
        String refreshToken = request.getHeader("Refresh-Token");
        String device = request.getHeader("User-Agent");

        // Revisar si es el endpoint de cotización de planes / para autorizar sin
        // actualizar credenciales
        if (request.getRequestURI().contains("/quoter/search/plan")) {
            this.validatePlanFinder(request, response, chain, device, refreshToken);
            return;
        }

        // Obtener token para autorizar
        String tokenHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (tokenHeader == null || !tokenHeader.startsWith(PREFIX_TOKEN) || DataHelper.isNull(refreshToken)) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
            return;
        }
        String sessionToken = tokenHeader.replace(PREFIX_TOKEN, "");

        try {
            // Luego de tener el token, obtenemos los claims/payload del token, para validar
            // info
            Claims claims = JwtConfig.obtainClaims(sessionToken);
            String userEmail = JwtConfig.getSubject(claims);
            // Buscamos usuario "Activado" y dispositivo relacionado al usuario que
            // encontramos en el session token
            Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);
            if (userOptional.isPresent() && userOptional.get().getPersonalData().getStatus().equals("Activado")) {

                // No es un usuario de prueba, se debe confirmar que tiene un dispositivo
                // relacionado
                Optional<DeviceModel> deviceOptional = deviceRepository.findByUserAndDevice(userEmail, device);
                if (deviceOptional.isPresent()) {
                    // Actualizar ips si es el caso
                    DeviceModel deviceDB = deviceOptional.get();
                    String ipAddress = !DataHelper.isNull(request.getRemoteAddr()) ? request.getRemoteAddr() : null;
                    this.refreshIpAddress(ipAddress, deviceDB);
                    // Generamos objeto de autorización
                    String strAuthorities = JwtConfig.getClaim(claims, "authorities");
                    Collection<? extends GrantedAuthority> authorities = Arrays.asList(new ObjectMapper()
                            .addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityJsonCreator.class)
                            .readValue(strAuthorities.getBytes(), SimpleGrantedAuthority[].class));
                    Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, "Updated",
                            authorities);
                    this.authContextForUser(request, response, chain, authForUser);
                    return;
                }
            }
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch (SignatureException | ExpiredJwtException e) {
            // Actualizar token de refresco si es el caso
            this.checkRefreshToken(request, response, chain, device, refreshToken);
        } catch (JwtException e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        } catch (Exception e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        }
    }

    @Transactional
    private void validatePlanFinder(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String device, String refreshToken) throws IOException, ServletException {
        String ipAddress = !DataHelper.isNull(request.getRemoteAddr()) ? request.getRemoteAddr() : null;
        try {
            // Intentamos leer el Token de Refresco
            Claims claims = JwtConfig.obtainClaims(refreshToken);
            String user = JwtConfig.getClaim(claims, "user");

            // Buscamos usuario "Activado" y dispositivo relacionado
            UserModel userDB = userRepository.findByPersonalData_Email(user).orElseThrow();
            UserDataModel userData = userDB.getPersonalData();
            String userRole = userData.getProfileRole();
            if (!userData.getStatus().equals("Activado")) {
                ResponseHelper.invalidJWT(response, "datos anticuados", null);
                return;
            }

            // Actualizar ips si es el caso
            DeviceModel deviceDB = deviceRepository.findByUserAndDevice(user, device).orElseThrow();
            this.refreshIpAddress(ipAddress, deviceDB);

            // Generamos objeto de autorización por el usuario verificado (credenciales
            // actualizadas).
            Authentication authForUser = new UsernamePasswordAuthenticationToken(user, "Updated",
                    Collections.singletonList(new SimpleGrantedAuthority(userRole)));
            this.authContextForUser(request, response, chain, authForUser);
        } catch (SignatureException | ExpiredJwtException e) {
            // Buscamos dispositivo relacionado al refreshToken del usuario y un usuario
            // "Activado".
            Optional<DeviceModel> deviceOptional = deviceRepository.findByDeviceAndRefreshToken(device, refreshToken);
            if (deviceOptional.isPresent()) {
                DeviceModel deviceDB = deviceOptional.get();
                String userEmail = deviceDB.getUser();
                Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);
                if (userOptional.isPresent() && userOptional.get().getPersonalData().getStatus().equals("Activado")) {
                    // Actualizar ips si es el caso
                    this.refreshIpAddress(ipAddress, deviceDB);
                    // Generamos objeto de autenticación
                    String userRole = userOptional.get().getPersonalData().getProfileRole();
                    Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, "Updated",
                            Collections.singletonList(new SimpleGrantedAuthority(userRole)));
                    this.authContextForUser(request, response, chain, authForUser);
                    return;
                }
            }
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch (JwtException e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch (Exception e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        }
    }

    // Verificar token de refresco si es correcto para actualizar el token de
    // expiración
    @Transactional
    private void checkRefreshToken(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String device, String refreshToken) throws IOException, ServletException {
        String ipAddress = !DataHelper.isNull(request.getRemoteAddr()) ? request.getRemoteAddr() : null;
        try {
            Claims claims = JwtConfig.obtainClaims(refreshToken);
            String userEmail = JwtConfig.getClaim(claims, "user");
            // Verificamos un usuario que este "Activado"
            UserModel userDB = userRepository.findByPersonalData_Email(userEmail).orElseThrow();
            UserDataModel userData = userDB.getPersonalData();
            if (!userData.getStatus().equals("Activado")) {
                ResponseHelper.invalidJWT(response, "datos anticuados", null);
                return;
            }

            // No es un usuario de prueba, por lo que hay que validar con un usuario
            // relacionado a un dispositivo
            DeviceModel deviceDB = deviceRepository.findByUserAndDevice(userEmail, device).orElseThrow();
            // Actualizar ips si es el caso
            this.refreshIpAddress(ipAddress, deviceDB);
            // Se actualiza token de sessión y se autoriza
            Collection<GrantedAuthority> authorities = Collections
                    .singletonList(new SimpleGrantedAuthority(userData.getProfileRole()));
            Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, "Updated", authorities);
            this.authContextForUser(request, response, chain, authForUser);
            this.updateSessionToken(userEmail,
                    authorities,
                    userDB);
        } catch (SignatureException | ExpiredJwtException e) {
            // Buscamos dispositivo relacionado al refreshToken del usuario y un usuario
            // "Activado".
            Optional<DeviceModel> deviceOptional = deviceRepository.findByDeviceAndRefreshToken(device, refreshToken);
            if (deviceOptional.isPresent()) {
                DeviceModel deviceDB = deviceOptional.get();
                String userEmail = deviceDB.getUser();
                Optional<UserModel> userOptional = userRepository.findByPersonalData_Email(userEmail);
                if (userOptional.isPresent() && userOptional.get().getPersonalData().getStatus().equals("Activado")) {
                    // Actualizar ips si es el caso
                    this.refreshIpAddress(ipAddress, deviceDB);
                    // Se autoriza y se actualiza el sesión token, y el refresh token solo se
                    // actualiza si la
                    // solicitud es completada en su totalidad.
                    UserModel userDB = userOptional.get();
                    UserDataModel userData = userDB.getPersonalData();
                    Collection<GrantedAuthority> authorities = Collections
                            .singletonList(new SimpleGrantedAuthority(userData.getProfileRole()));
                    // El "Dated", es porque se tiene que actualizar el token de refresco.
                    Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, "Dated",
                            authorities);
                    this.authContextForUser(request, response, chain, authForUser);
                    this.updateSessionToken(userEmail, authorities, userDB);
                    return;
                }
            }
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", null);
        } catch (Exception e) {
            ResponseHelper.invalidJWT(response, "no es posible continuar con la solicitud", e.getMessage());
        }
    }

    // Autenticación rápida para el contexto de Spring
    private void authContextForUser(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication auth) throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private void refreshIpAddress(String ipAddress, DeviceModel deviceDB) {
        if (!DataHelper.isNull(ipAddress) && !deviceDB.getIps().contains(ipAddress)) {
            deviceDB.addIp(ipAddress);
            deviceDB.setUpdatedDate(LocalDateTime.now());
            deviceRepository.save(deviceDB);
        }
    }

    private void updateSessionToken(String email, Collection<GrantedAuthority> authorities, UserModel userDB)
            throws JsonProcessingException {
        userDB.getPersonalData().setSessionToken(JwtConfig.createSessionToken(email, authorities));
        userRepository.save(userDB);
    }

}
