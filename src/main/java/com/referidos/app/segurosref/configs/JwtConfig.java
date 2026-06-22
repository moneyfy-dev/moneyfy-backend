package com.referidos.app.segurosref.configs;

import java.util.Collection;
import java.util.Date;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtConfig {

    private static final String SECRET_ENV = "MONEYFY_JWT_SECRET";

    public static final SecretKey SECRET_KEY = buildSecretKey();
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String CONTENT_TYPE = "application/json";

    // Tiempos de expiración (en milisegundos)
    public static final long SESSION_TOKEN_EXPIRATION = 1000 * 60 * 60; // 1 Hora
    public static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 8; // 8 Horas
    public static final long REFRESH_THRESHOLD = 1000 * 60 * 60 * 4; // 4 Horas (50%)

    private static SecretKey buildSecretKey() {
        String configuredSecret = System.getenv(SECRET_ENV);
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("CRITICAL ERROR: JWT Secret Key is missing. Please set the '" + SECRET_ENV
                    + "' environment variable.");
        }
        return Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
    }

    public static String createSessionToken(String email, Collection<? extends GrantedAuthority> authorities)
            throws JsonProcessingException {
        Claims claims = Jwts.claims()
                .add("authorities", new ObjectMapper().writeValueAsString(authorities))
                .build();

        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + SESSION_TOKEN_EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String createRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims obtainClaims(String token) throws JwtException {
        return Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();
    }

    public static String getSubject(Claims claims) {
        return claims.getSubject();
    }

    public static String getClaim(Claims claims, String key) {
        return (String) claims.get(key);
    }

}
