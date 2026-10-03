package com.referidos.app.segurosref.websession;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.referidos.app.segurosref.responses.GeneralResponse;

@RestControllerAdvice
public class WebSessionResponseAdvice implements ResponseBodyAdvice<Object> {
    private final WebSessionSupport sessions;
    public WebSessionResponseAdvice(WebSessionSupport sessions) { this.sessions = sessions; }

    @Override public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) { return true; }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) return body;
        var req = servletRequest.getServletRequest();
        var res = servletResponse.getServletResponse();
        if (!WebSessionSupport.isWebRequest(req)) return body;
        res.setHeader("Cache-Control", "no-store");
        if (req.getRequestURI().equals(req.getContextPath() + "/auth/logout") && res.getStatus() < 400) {
            WebSessionSupport.clearCookies(res);
        }
        if (body instanceof GeneralResponse general && general.data() instanceof Map<?, ?> original) {
            Object session = original.get("sessionToken");
            Object refresh = original.get("refreshToken");
            if (session instanceof String sessionToken && refresh instanceof String refreshToken) {
                WebSessionSupport.issueAuthCookies(res, sessionToken, refreshToken);
                Map<String, Object> data = new LinkedHashMap<>();
                original.forEach((key, value) -> {
                    if (!"sessionToken".equals(key) && !"refreshToken".equals(key)) data.put(String.valueOf(key), value);
                });
                data.put("csrfToken", sessions.csrfToken(req, res, true));
                return new GeneralResponse(general.message(), general.status(), data);
            }
        }
        return body;
    }
}
