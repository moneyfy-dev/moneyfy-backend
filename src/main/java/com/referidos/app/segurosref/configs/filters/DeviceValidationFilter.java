package com.referidos.app.segurosref.configs.filters;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.FilterHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DeviceValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        // Ignorar rutas públicas
        if (FilterHelper.checkPublicRoute(request)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Verificar con 'User-Agent'
        String userAgent = request.getHeader("User-Agent");
        if(!DataHelper.isNull(userAgent) && !this.isDeviceBanned(userAgent)) {
            chain.doFilter(request, response);
            return;
        }

        ResponseHelper.failedDependency(response, "no es posible continuar con la solicitud", null);
    }

    private boolean isDeviceBanned(String userAgent) {
        String[] bannedDevices = {
            // Navegadores de escritorio
            "chrome", "safari", "firefox", "opera", "edge", "chromium", "brave", "vivaldi",
            "internet explorer", "microsoft edge", // Versiones anteriores
            // Sistemas operativos
            //"windows nt", "macos", "linux",
            // Aplicaciones web y herramientas de desarrollo
            //"postmann", "curl", "wget", "puppeteer", "selenium", "nodejs",
            // Bots y crawlers
            //"googlebot", "bingbot", "yandexbot", "baiduspider", "ahrefsbot",
            // Otros
            //"desktop", "laptop", "computer"
        };
        String userAgentLower = userAgent.toLowerCase();
        for(String device : bannedDevices) {
            if(userAgentLower.contains(device)) {
                return true;
            }
        }
        return false;
    }

}
