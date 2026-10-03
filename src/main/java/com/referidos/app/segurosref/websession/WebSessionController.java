package com.referidos.app.segurosref.websession;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.UserService;

@RestController
@RequestMapping("/auth/web")
public class WebSessionController {
    private final WebSessionSupport sessions;
    private final UserService users;
    public WebSessionController(WebSessionSupport sessions, UserService users) {
        this.sessions = sessions;
        this.users = users;
    }

    @GetMapping("/csrf")
    public GeneralResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        return new GeneralResponse("OK", 200, Map.of("csrfToken", sessions.csrfToken(request, response, false)));
    }

    @GetMapping("/session")
    public ResponseEntity<GeneralResponse> session(Principal principal, HttpServletRequest request, HttpServletResponse response) {
        ResponseEntity<GeneralResponse> hydration = users.hydrationData(principal.getName());
        GeneralResponse body = hydration.getBody();
        Map<String, Object> data = new LinkedHashMap<>();
        if (body != null && body.data() instanceof Map<?, ?> values) {
            values.forEach((key, value) -> data.put(String.valueOf(key), value));
        }
        data.put("csrfToken", sessions.csrfToken(request, response, false));
        return ResponseEntity.status(hydration.getStatusCode())
                .body(new GeneralResponse(body == null ? "OK" : body.message(), hydration.getStatusCode().value(), data));
    }
}
