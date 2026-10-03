package com.referidos.app.segurosref.websession;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class WebOriginValidationFilter extends OncePerRequestFilter {
    private final Set<String> allowedOrigins;

    public WebOriginValidationFilter(String origins) {
        allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty() && !origin.equals("null") && !origin.contains("*"))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (WebSessionSupport.isWebRequest(request) && origin != null && !allowedOrigins.contains(origin)) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"status\":403,\"code\":\"WEB_ORIGIN_DENIED\",\"message\":\"Origen web no autorizado\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }
}
