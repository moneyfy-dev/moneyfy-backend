package com.referidos.app.segurosref.configs.filters;

import static com.referidos.app.segurosref.configs.JwtConfig.HEADER_AUTHORIZATION;
import static com.referidos.app.segurosref.configs.JwtConfig.PREFIX_TOKEN;
import static com.referidos.app.segurosref.configs.JwtConfig.REFRESH_THRESHOLD;
import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import java.io.IOException;
import com.referidos.app.segurosref.websession.WebSessionSupport;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.referidos.app.segurosref.helpers.FilterHelper;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.configs.JwtConfig;
import com.referidos.app.segurosref.configs.SimpleGrantedAuthorityJsonCreator;
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.responses.ErrorResponse;
import com.referidos.app.segurosref.responses.enums.BusinessCodeEnum;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtValidationFilter extends BasicAuthenticationFilter {

    private final AuthRepository authRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtValidationFilter(AuthenticationManager authenticationManager, AuthRepository authRepository) {
        super(authenticationManager);
        this.authRepository = authRepository;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        return FilterHelper.checkPublicRoute(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String tokenHeader = request.getHeader(HEADER_AUTHORIZATION);
        String refreshToken = request.getHeader("X-New-Refresh-Token");
        boolean web = WebSessionSupport.isWebRequest(request);
        if (web) {
            String cookieToken = WebSessionSupport.cookie(request, WebSessionSupport.SESSION_COOKIE);
            tokenHeader = cookieToken == null ? null : PREFIX_TOKEN + cookieToken;
            refreshToken = WebSessionSupport.cookie(request, WebSessionSupport.REFRESH_COOKIE);
            response.setHeader("Cache-Control", "no-store");
        }

        if (tokenHeader == null || !tokenHeader.startsWith(PREFIX_TOKEN)) {
            sendUnauthorizedError(request, response);
            return;
        }

        String sessionToken = tokenHeader.replace(PREFIX_TOKEN, "");

        try {
            // Intento 1: Validar Session Token
            Claims claims = JwtConfig.obtainClaims(sessionToken);
            String userEmail = JwtConfig.getSubject(claims);

            if (!validateTokenNotRevoked(userEmail, claims.getIssuedAt())) {
                sendUnauthorizedError(request, response);
                return;
            }

            // Autorizar directamente
            String strAuthorities = JwtConfig.getClaim(claims, "authorities");
            Collection<? extends GrantedAuthority> authorities = Arrays.asList(new ObjectMapper()
                    .addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityJsonCreator.class)
                    .readValue(strAuthorities.getBytes(), SimpleGrantedAuthority[].class));

            Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authForUser);
            chain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // Intento 2: Session Token Expirado, evaluar Refresh Token (Silent Refresh /
            // Sliding Session)
            if (DataHelper.isNull(refreshToken)) {
                sendUnauthorizedError(request, response);
                return;
            }
            handleRefreshToken(request, response, chain, refreshToken, e.getClaims().getSubject());

        } catch (JwtException e) {
            sendUnauthorizedError(request, response);
        } catch (Exception e) {
            sendUnauthorizedError(request, response);
        }
    }

    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String refreshToken, String accessSubject) throws IOException, ServletException {
        try {
            Claims claims = JwtConfig.obtainClaims(refreshToken);
            if (!java.util.Objects.equals(accessSubject, claims.getSubject())) {
                sendUnauthorizedError(request, response);
                return;
            }
            String userEmail = JwtConfig.getSubject(claims);

            if (!validateTokenNotRevoked(userEmail, claims.getIssuedAt())) {
                sendUnauthorizedError(request, response);
                return;
            }

            Optional<AuthModel> authOptional = authRepository.findByEmail(userEmail);
            if (authOptional.isEmpty()) {
                sendUnauthorizedError(request, response);
                return;
            }

            AuthModel authModel = authOptional.get();
            Collection<GrantedAuthority> authorities = AuthorityUtils
                    .commaSeparatedStringToAuthorityList(authModel.getRole());

            // Generar nuevo Session Token
            String newSessionToken = JwtConfig.createSessionToken(userEmail, authorities);
            String effectiveRefreshToken = refreshToken;

            // Verificar si el Refresh Token también necesita actualizarse (Sliding Session)
            Date expiration = claims.getExpiration();
            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            if (timeLeft <= REFRESH_THRESHOLD) {
                effectiveRefreshToken = JwtConfig.createRefreshToken(userEmail);
            }

            if (WebSessionSupport.isWebRequest(request)) {
                WebSessionSupport.issueAuthCookies(response, newSessionToken, effectiveRefreshToken);
            } else {
                response.addHeader("X-New-Session-Token", newSessionToken);
                if (!effectiveRefreshToken.equals(refreshToken)) response.addHeader("X-New-Refresh-Token", effectiveRefreshToken);
            }
            Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authForUser);
            chain.doFilter(request, response);

        } catch (Exception e) {
            sendUnauthorizedError(request, response);
        }
    }

    private boolean validateTokenNotRevoked(String email, Date issuedAt) {
        Optional<AuthModel> authOptional = authRepository.findByEmail(email);
        if (authOptional.isPresent()) {
            AuthModel auth = authOptional.get();
            LocalDateTime revocationDate = auth.getTokenRevocationDate();
            if (revocationDate != null) {
                LocalDateTime tokenIat = issuedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                return !tokenIat.isBefore(revocationDate);
            }
            return true;
        }
        return false;
    }

    private void sendUnauthorizedError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        if (WebSessionSupport.isWebRequest(request)) WebSessionSupport.clearCookies(response);
        response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        response.setContentType(CONTENT_TYPE);

        BusinessCodeEnum errorEnum = BusinessCodeEnum.APP_TOKEN_INVALID_OR_EXPIRED;
        ErrorResponse<Object> errorResponse = new ErrorResponse<>(
                errorEnum.getErrorDescription(),
                errorEnum.getErrorCode(),
                null);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
