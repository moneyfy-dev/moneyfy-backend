package com.referidos.app.segurosref.configs.filters;

import static com.referidos.app.segurosref.configs.JwtConfig.HEADER_AUTHORIZATION;
import static com.referidos.app.segurosref.configs.JwtConfig.PREFIX_TOKEN;
import static com.referidos.app.segurosref.configs.JwtConfig.REFRESH_THRESHOLD;
import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.AntPathMatcher;

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
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Patrones de rutas públicas (no requieren token)
    private static final String[] PUBLIC_ROUTES = {
            "/auth/register",
            "/auth/confirm/registration",
            "/auth/log-in",
            "/auth/confirm/device/change",
            "/auth/restore/password",
            "/auth/confirm/password/reset",
            "/auth/resend/code",
            "/home",
            "/seed/**",
            "/log/**",
            "/transaction/**",
            "/quoter/commission/**",
            "/quoter/finalize/quote",
            "/api/v1/manager/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",

    };

    public JwtValidationFilter(AuthenticationManager authenticationManager, AuthRepository authRepository) {
        super(authenticationManager);
        this.authRepository = authRepository;
    }

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getServletPath();
        for (String publicRoute : PUBLIC_ROUTES) {
            if (pathMatcher.match(publicRoute, path)) {
                chain.doFilter(request, response);
                return;
            }
        }

        String tokenHeader = request.getHeader(HEADER_AUTHORIZATION);
        String refreshToken = request.getHeader("X-New-Refresh-Token");

        if (tokenHeader == null || !tokenHeader.startsWith(PREFIX_TOKEN)) {
            sendUnauthorizedError(response);
            return;
        }

        String sessionToken = tokenHeader.replace(PREFIX_TOKEN, "");

        try {
            // Intento 1: Validar Session Token
            Claims claims = JwtConfig.obtainClaims(sessionToken);
            String userEmail = JwtConfig.getSubject(claims);

            if (!validateTokenNotRevoked(userEmail, claims.getIssuedAt())) {
                sendUnauthorizedError(response);
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
                sendUnauthorizedError(response);
                return;
            }
            handleRefreshToken(request, response, chain, refreshToken);

        } catch (JwtException e) {
            sendUnauthorizedError(response);
        } catch (Exception e) {
            sendUnauthorizedError(response);
        }
    }

    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String refreshToken) throws IOException, ServletException {
        try {
            Claims claims = JwtConfig.obtainClaims(refreshToken);
            String userEmail = JwtConfig.getClaim(claims, "user");

            if (!validateTokenNotRevoked(userEmail, claims.getIssuedAt())) {
                sendUnauthorizedError(response);
                return;
            }

            Optional<AuthModel> authOptional = authRepository.findByEmail(userEmail);
            if (authOptional.isEmpty()) {
                sendUnauthorizedError(response);
                return;
            }

            AuthModel authModel = authOptional.get();
            Collection<GrantedAuthority> authorities = Collections
                    .singletonList(new SimpleGrantedAuthority(authModel.getRole()));

            // Generar nuevo Session Token
            String newSessionToken = JwtConfig.createSessionToken(userEmail, authorities);
            response.addHeader("X-New-Session-Token", newSessionToken);

            // Verificar si el Refresh Token también necesita actualizarse (Sliding Session)
            Date expiration = claims.getExpiration();
            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            if (timeLeft <= REFRESH_THRESHOLD) {
                String newRefreshToken = JwtConfig.createRefreshToken(userEmail);
                response.addHeader("X-New-Refresh-Token", newRefreshToken);
            }

            Authentication authForUser = new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authForUser);
            chain.doFilter(request, response);

        } catch (Exception e) {
            sendUnauthorizedError(response);
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

    private void sendUnauthorizedError(HttpServletResponse response) throws IOException {
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
