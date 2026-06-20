package com.referidos.app.segurosref.helpers;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.AntPathMatcher;

public class FilterHelper {

    public static final String[] PUBLIC_ROUTES = {
            "/auth/register",
            "/auth/confirm/registration",
            "/auth/log-in",
            "/auth/confirm/device/change",
            "/auth/restore/password",
            "/auth/confirm/password/reset",
            "/auth/resend/code",
            "/api/v1/manager/auth/log-in",
            "/api/v1/manager/auth/restore/password",
            "/api/v1/manager/auth/confirm/password/reset",
            "/api/v1/manager/auth/resend/code",
            "/home",
            "/quoter/commission/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @SuppressWarnings("null")
    public static boolean checkPublicRoute(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        for (String publicRoute : PUBLIC_ROUTES) {
            if (pathMatcher.match(publicRoute, path)) {
                return true;
            }
        }
        return false;
    }
}
