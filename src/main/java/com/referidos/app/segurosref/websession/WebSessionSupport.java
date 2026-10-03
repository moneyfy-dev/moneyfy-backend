package com.referidos.app.segurosref.websession;

import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import com.referidos.app.segurosref.configs.JwtConfig;

@Component
public class WebSessionSupport {
    public static final String SESSION_COOKIE = "__Host-MoneyfySession";
    public static final String REFRESH_COOKIE = "__Host-MoneyfyRefresh";
    public static final String CSRF_COOKIE = "__Host-MoneyfyCsrf";
    private final CookieCsrfTokenRepository csrfRepository;

    public WebSessionSupport(CookieCsrfTokenRepository csrfRepository) {
        this.csrfRepository = csrfRepository;
    }

    public static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }

    public static boolean isWebRequest(HttpServletRequest request) {
        return "web".equalsIgnoreCase(request.getHeader("X-Moneyfy-Client"))
                || cookie(request, SESSION_COOKIE) != null || cookie(request, REFRESH_COOKIE) != null
                || request.getServletPath().startsWith("/auth/web/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/auth/web/");
    }

    private static void writeCookie(HttpServletResponse response, String name, String value, long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value)
                .path("/").httpOnly(true).secure(true).sameSite("Strict")
                .maxAge(Duration.ofSeconds(seconds)).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    public static void issueAuthCookies(HttpServletResponse response, String session, String refresh) {
        long seconds = Math.max(1, (JwtConfig.obtainClaims(refresh).getExpiration().getTime() - System.currentTimeMillis()) / 1000);
        // Keep expired access JWT available until refresh expires, for silent renewal.
        writeCookie(response, SESSION_COOKIE, session, seconds);
        writeCookie(response, REFRESH_COOKIE, refresh, seconds);
    }

    public static void clearCookies(HttpServletResponse response) {
        for (String name : new String[]{SESSION_COOKIE, REFRESH_COOKIE, CSRF_COOKIE}) writeCookie(response, name, "", 0);
    }

    public String csrfToken(HttpServletRequest request, HttpServletResponse response, boolean rotate) {
        CsrfToken token = rotate ? null : csrfRepository.loadToken(request);
        if (token == null) {
            token = csrfRepository.generateToken(request);
            csrfRepository.saveToken(token, request, response);
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return token.getToken();
    }
}
