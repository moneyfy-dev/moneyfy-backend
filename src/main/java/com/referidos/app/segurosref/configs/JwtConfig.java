package com.referidos.app.segurosref.configs;

import java.util.Collection;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class JwtConfig {

    public static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String CONTENT_TYPE = "application/json";

    public static final String REFRESH_SUBJECT = "r3f3r1d0s0pp";
    public static final String REFRESH_CLAIM = "3x1t0";

    public static String createSessionToken(String email, Collection<? extends GrantedAuthority> authorities) throws JsonProcessingException {
        Claims claims = Jwts.claims()
            .add("authorities", new ObjectMapper().writeValueAsString(authorities))
            .build();
        
        String token = Jwts.builder()
            .subject(email)
            .claims(claims)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + (1000 * 60 * 15))) // 15 minutos / 1 día
            .signWith(SECRET_KEY) // Puede haber excepción / Una solución rápida es que devolver el token antiguo en caso de error.
            .compact();

        return token;
    }

    public static String createRefreshToken(String email) {
        String currentTime = String.valueOf(System.currentTimeMillis());
        Claims claims = Jwts.claims()
            .add("refreshValue", REFRESH_SUBJECT + ":" + currentTime + ":" + REFRESH_CLAIM)
            .add("user", email).build();

        String token = Jwts.builder()
            .subject(REFRESH_SUBJECT)
            .claims(claims)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 4))) // 4 horas / 1 día
            .signWith(SECRET_KEY) // Puede haber excepción / Una solución rápida es que devolver el token antiguo en caso de error.
            .compact();

        return token;
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
