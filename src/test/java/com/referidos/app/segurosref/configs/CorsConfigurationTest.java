package com.referidos.app.segurosref.configs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.filter.CorsFilter;

import com.referidos.app.segurosref.configs.filters.JwtValidationFilter;
import com.referidos.app.segurosref.repositories.AuthRepository;

class CorsConfigurationTest {
    private final CorsFilter cors = new CorsFilter(new SecurityConfig().corsConfigurationSource());

    private MockHttpServletResponse preflight(String origin) throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/auth/log-in");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type,authorization,x-new-refresh-token");
        var response = new MockHttpServletResponse();
        cors.doFilter(request, response, (req, res) -> fail("Preflight must finish before authentication"));
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1:5173"})
    void permitsOnlyTheConfiguredLocalWebOrigins(String origin) throws Exception {
        var response = preflight(origin);
        assertEquals(200, response.getStatus());
        assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
        assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("x-new-refresh-token"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://moneyfy.cl", "https://admin.moneyfy.cl", "https://app.moneyfy.cl"})
    void preservesExistingProductionOrigins(String origin) throws Exception {
        assertEquals(200, preflight(origin).getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://untrusted.example", "http://localhost:5174", "http://127.0.0.1:5174", "http://localhost.evil.example:5173", "null"})
    void rejectsUnconfiguredOrigins(String origin) throws Exception {
        var response = preflight(origin);
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void localLoginPostReachesTheApplicationAndExposesRotationHeaders() throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/log-in");
        request.addHeader("Origin", "http://127.0.0.1:5173");
        request.setContentType("application/json");
        var response = new MockHttpServletResponse();
        cors.doFilter(request, response, (req, res) -> {
            response.setHeader("X-New-Session-Token", "fixture-session");
            response.setHeader("X-New-Refresh-Token", "fixture-refresh");
            response.setStatus(204);
        });
        assertEquals(204, response.getStatus());
        assertEquals("http://127.0.0.1:5173", response.getHeader("Access-Control-Allow-Origin"));
        var exposed = response.getHeader("Access-Control-Expose-Headers");
        assertNotNull(exposed);
        assertTrue(exposed.contains("X-New-Session-Token"));
        assertTrue(exposed.contains("X-New-Refresh-Token"));
        assertFalse(exposed.contains("*"));
    }

    @Test
    void existingAdminOriginCanReadRotatedTokens() throws Exception {
        var response = preflight("https://admin.moneyfy.cl");
        var exposed = response.getHeader("Access-Control-Expose-Headers");
        assertNotNull(exposed);
        assertTrue(exposed.contains("X-New-Session-Token"));
        assertTrue(exposed.contains("X-New-Refresh-Token"));
    }

    @Test
    void untrustedLoginPostNeverReachesTheApplication() throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/log-in");
        request.addHeader("Origin", "https://untrusted.example");
        request.setContentType("application/json");
        var response = new MockHttpServletResponse();
        cors.doFilter(request, response, (req, res) -> fail("Untrusted origin must not reach login"));
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void localOriginStillRequiresRealJwtForProtectedData() throws Exception {
        var jwt = new JwtValidationFilter(mock(AuthenticationManager.class), mock(AuthRepository.class));
        var request = new MockHttpServletRequest("POST", "/users/hydration/data");
        request.setServletPath("/users/hydration/data");
        request.addHeader("Origin", "http://127.0.0.1:5173");
        var response = new MockHttpServletResponse();
        cors.doFilter(request, response, (req, res) -> jwt.doFilter(req, res,
                (authenticatedRequest, authenticatedResponse) -> fail("Missing JWT must never reach protected data")));
        assertEquals(417, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"internalErrorCode\":3"));
    }
}
