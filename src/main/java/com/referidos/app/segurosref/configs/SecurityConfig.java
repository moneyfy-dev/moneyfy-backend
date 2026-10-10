package com.referidos.app.segurosref.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.referidos.app.segurosref.helpers.FilterHelper;

import com.referidos.app.segurosref.configs.filters.JwtValidationFilter;
import com.referidos.app.segurosref.repositories.AuthRepository;

import java.util.Arrays;
import java.time.Duration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import com.referidos.app.segurosref.websession.WebSessionSupport;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final String DEFAULT_WEB_ORIGINS = "https://moneyfy.cl,https://app.moneyfy.cl,https://web.moneyfy.cl,http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:80";

    @org.springframework.beans.factory.annotation.Value("${moneyfy.web.allowed-origins:" + DEFAULT_WEB_ORIGINS + "}")
    private String webAllowedOrigins = DEFAULT_WEB_ORIGINS;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            JwtValidationFilter jwtValidationFilter, CookieCsrfTokenRepository csrfRepository) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        // Stateless JWT validation authenticates every request. Rotate CSRF only
                        // when issuing login/confirmation/reset cookies in WebSessionResponseAdvice.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler() {
                            @Override public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request, CsrfToken token) {
                                return request.getHeader(token.getHeaderName());
                            }
                        })
                        .requireCsrfProtectionMatcher(request -> WebSessionSupport.isWebRequest(request)
                                && !Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod())))
                .exceptionHandling(errors -> errors
                        .accessDeniedHandler((request, response, error) -> {
                            if (!(error instanceof org.springframework.security.web.csrf.CsrfException)) {
                                response.sendError(403);
                                return;
                            }
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.setHeader("Cache-Control", "no-store");
                            response.getWriter().write("{\"status\":403,\"code\":\"WEB_CSRF_INVALID\",\"message\":\"Token CSRF invalido\",\"data\":null}");
                        }))
                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(this.corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(FilterHelper.PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new com.referidos.app.segurosref.websession.WebOriginValidationFilter(webAllowedOrigins),
                        org.springframework.web.filter.CorsFilter.class)
                .addFilterBefore(jwtValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtValidationFilter jwtValidationFilter(
            AuthenticationManager authenticationManager,
            AuthRepository authRepository) {
        return new JwtValidationFilter(authenticationManager, authRepository);
    }

    @Bean
    public CookieCsrfTokenRepository webCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName(WebSessionSupport.CSRF_COOKIE);
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(true).sameSite("Strict")
                .maxAge(cookie.build().getMaxAge().isZero() ? Duration.ZERO : Duration.ofHours(8)));
        return repository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();

        java.util.List<String> exactOrigins = new java.util.ArrayList<>(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"));
        Arrays.stream(webAllowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty() && !origin.equals("null") && !origin.contains("*"))
                .forEach(exactOrigins::add);
        cors.setAllowedOrigins(exactOrigins);

        cors.setAllowedOriginPatterns(Arrays.asList(
                "https://moneyfy.cl",
                "https://*.moneyfy.cl",
                "http://192.168.*.*:*",
                "http://10.*.*.*:*",
                "exp://*"));
        cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Refresh-Token", "Origin", "User-Agent",
                        "X-New-Session-Token", "X-New-Refresh-Token", "X-Moneyfy-Client", "X-CSRF-TOKEN"));
        // Browsers must read these headers to persist rotated JWTs.
        cors.setExposedHeaders(Arrays.asList("X-New-Session-Token", "X-New-Refresh-Token"));
        cors.setAllowCredentials(true);

        // Creamos la instancia del objeto que implementa la interfaz Cors... y
        // entregamos las
        // configuraciones del source, que se aplicaran en una ruta de nuestra app del
        // backend.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);

        return source;
    }

}
