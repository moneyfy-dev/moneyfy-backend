package com.referidos.app.segurosref.configs.filters;

// import static com.referidos.app.segurosref.configs.PropertyConfig.LOGGER_MESSAGES;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.models.DeviceModel;
import com.referidos.app.segurosref.models.WhiteListModel;
import com.referidos.app.segurosref.repositories.DeviceRepository;
import com.referidos.app.segurosref.repositories.WhiteListRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DeviceValidationFilter extends OncePerRequestFilter {

    private WhiteListRepository whiteListRepository;

    private DeviceRepository deviceRepository;

    public DeviceValidationFilter(WhiteListRepository whiteListRepository, DeviceRepository deviceRepository) {
        this.whiteListRepository = whiteListRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {


        String uri = request.getRequestURI();

        // Ignorar rutas públicas
        if (uri.equals("/") || uri.equals("/moneyfy/") || uri.startsWith("/auth") 
            || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }
        
                                        
        // VERIFIFICAR EL DISPOSITIVO MEDIANTE LA CABECERA 'User-Agent'
        String userAgent = request.getHeader("User-Agent");

        // UserAgent Obligatorio
        if(DataHelper.isNull(userAgent)) {
            ResponseHelper.failedDependency(response, "no es posible continuar con la solicitud", null);
            return;
        }

        // Verificar que el dispositivo este permitido
        if(!this.isDeviceBanned(userAgent)) {
            chain.doFilter(request, response);
            return;
        }

        String ipDevice = request.getRemoteAddr();
        // LOGGER_MESSAGES.info("\n-----\nLa ip del dispositivo es: " + ipDevice + "\n-----");

        // Solo se ejecuta cuando no se reconoció un dispositivo válido
        if(!DataHelper.isNull(ipDevice)) {

            // Si existe un registro de usuario con la ip que esta haciendo la solicitud, pasa el filtro
            if(whiteListRepository.existsByIpsContaining(ipDevice)) {
                chain.doFilter(request, response);
                return;
            }

            // Actualizar white list - buscando todos los dispositivos de los usuario desarrolladores
            String[] adminUsers = {"alejandro.osses.r@gmail.com", "nuser.random@gmail.com"};
            List<DeviceModel> adminDevices = deviceRepository.findAllByUserContaining("@connect360.cl");
            for(String adminUser : adminUsers) {
                Optional<DeviceModel> optionalDevice = deviceRepository.findByUser(adminUser);
                if(optionalDevice.isPresent()) {
                    adminDevices.add(optionalDevice.get());
                }
            }

            // Se tiene la lista completa de los dispositivos de los administradores, ahora se revisa si su IP esta guardada
            boolean isValid = false;
            for(DeviceModel deviceDB : adminDevices) {
                // Se verifica si la IP que está realizando la consulta está en los dispositivos autorizados
                Set<String> ips = deviceDB.getIps();
                if(ips.contains(ipDevice)) {
                    isValid = true;
                }

                // Se revisa si existe un WhiteList relacionado al usuario del dispositivo autorizado
                String userEmail = deviceDB.getUser();
                Optional<WhiteListModel> whiteListOptional = whiteListRepository.findByUser(userEmail);
                LocalDateTime currenDateTime = LocalDateTime.now();
                if(whiteListOptional.isEmpty()) {
                    WhiteListModel novaWhiteList = new WhiteListModel(userEmail, ips, currenDateTime, currenDateTime);
                    whiteListRepository.save(novaWhiteList);
                } else if(whiteListOptional.isPresent() && isValid) {
                    // Existe registro y en el dispositivo autorizado existía la ip, hay que actualizar la WhiteList
                    WhiteListModel whiteListDB = whiteListOptional.get();
                    whiteListDB.setIps(ips);
                    whiteListDB.setUpdatedDate(currenDateTime);
                    whiteListRepository.save(whiteListDB);
                }

                if(isValid) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        ResponseHelper.failedDependency(response, "no es posible continuar con la solicitud", null);
    }

    private boolean isDeviceBanned(String userAgent) {
        // Quitar phrases muy genéricas: "x11", "pc", 
        String[] bannedDevices = {
            // Navegadores de escritorio
            //"chrome", "safari", "firefox", "opera", "edge", "chromium", "brave", "vivaldi",
            //"internet explorer", "microsoft edge", // Versiones anteriores
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
