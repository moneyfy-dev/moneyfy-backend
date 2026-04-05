package com.referidos.app.segurosref.helpers;

import jakarta.servlet.http.HttpServletRequest;

public class FilterHelper {

    public static boolean checkPublicRoute(HttpServletRequest request) {
        String endpoint = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!DataHelper.isNull(contextPath) && endpoint.startsWith(contextPath)) {
            endpoint = endpoint.substring(contextPath.length());
        }
        if (endpoint.isEmpty()) {
            endpoint = "/";
        }
        return endpoint.equals("/") || endpoint.equals("/swagger-ui.html") || endpoint.startsWith("/swagger-ui")
            || endpoint.startsWith("/v3/api-docs") || (endpoint.startsWith("/auth") && !endpoint.equals("/auth/disable/account"))
            || endpoint.startsWith("/seed");
    }

}
