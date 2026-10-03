package com.referidos.app.segurosref.configs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referidos.app.segurosref.controllers.AuthController;
import com.referidos.app.segurosref.controllers.UserController;
import com.referidos.app.segurosref.models.AuthModel;
import com.referidos.app.segurosref.repositories.AuthRepository;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.UserService;
import com.referidos.app.segurosref.services.impl.UserDetailsServiceImpl;

class WebSessionSecurityTest {
    private static final String EMAIL = "session-test@example.invalid";
    private static final String CLIENT = "X-Moneyfy-Client";
    private static final String CSRF = "X-CSRF-TOKEN";
    private static final String SESSION_COOKIE = "__Host-MoneyfySession";
    private static final String REFRESH_COOKIE = "__Host-MoneyfyRefresh";
    private static final String CSRF_COOKIE = "__Host-MoneyfyCsrf";
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private AuthRepository repository;
    private UserDetailsServiceImpl auth;
    private String session;
    private String refresh;
    private final ObjectMapper json = new ObjectMapper();

    @Configuration
    @EnableWebMvc
    static class TestMvc {
        @Bean AuthRepository authRepository() { return mock(AuthRepository.class); }
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean UserDetailsServiceImpl userDetailsServiceImpl() { return mock(UserDetailsServiceImpl.class); }
        @Bean AuthController authController(UserService users, UserDetailsServiceImpl auth) { return new AuthController(users, auth); }
        @Bean UserController userController(UserService users) { return new UserController(users); }
    }

    @BeforeEach
    void setup() throws Exception {
        context = new AnnotationConfigWebApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("web-test",
                Map.of("moneyfy.web.allowed-origins", "https://moneyfy.cl,https://app.moneyfy.cl,http://localhost:5173,http://127.0.0.1:5173,https://configured.example.invalid")));
        context.setServletContext(new MockServletContext());
        context.register(SecurityConfig.class, TestMvc.class);
        context.scan("com.referidos.app.segurosref.websession");
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
        repository = context.getBean(AuthRepository.class);
        auth = context.getBean(UserDetailsServiceImpl.class);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(AuthModel.builder().email(EMAIL).role("ROLE_USER")
                .tokenRevocationDate(LocalDateTime.of(2000, 1, 1, 0, 0)).build()));
        session = JwtConfig.createSessionToken(EMAIL, AuthorityUtils.createAuthorityList("ROLE_USER"));
        refresh = JwtConfig.createRefreshToken(EMAIL);
        var response = ResponseEntity.ok(new GeneralResponse("OK", 200,
                Map.of("user", Map.of("userId", "fixture-user"), "sessionToken", session, "refreshToken", refresh)));
        when(auth.userLogin(any())).thenReturn(response);
        when(auth.confirmRegistration(any())).thenReturn(response);
        when(auth.confirmPasswordReset(any())).thenReturn(response);
        when(auth.logout(EMAIL)).thenReturn(ResponseEntity.ok(new GeneralResponse("OK", 200, null)));
        when(context.getBean(UserService.class).hydrationData(EMAIL))
                .thenReturn(ResponseEntity.ok(new GeneralResponse("OK", 200, Map.of("user", Map.of("userId", "fixture-user")))));
    }

    @AfterEach void close() { context.close(); }

    private MvcResult bootstrap() throws Exception {
        return mvc.perform(get("/auth/web/csrf").header(CLIENT, "web").secure(true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.csrfToken").isString()).andReturn();
    }
    private String csrf(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).at("/data/csrfToken").asText(); }
    private Cookie csrfCookie(MvcResult result) throws Exception { return new Cookie(CSRF_COOKIE, csrf(result)); }
    private Cookie[] authCookies() { return new Cookie[]{new Cookie(SESSION_COOKIE, session), new Cookie(REFRESH_COOKIE, refresh)}; }

    @Test void bootstrapIsAnonymousAndItsCookieCannotBeReadByJavascript() throws Exception {
        var result = bootstrap();
        String cookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.startsWith(CSRF_COOKIE + "="));
        for (String attribute : List.of("Path=/", "Secure", "HttpOnly")) assertTrue(cookie.contains(attribute), attribute + " missing");
        // MockMvc does not serialize arbitrary Servlet 6 cookie attributes into its header.
        assertEquals("Strict", result.getResponse().getCookie(CSRF_COOKIE).getAttribute("SameSite"));
        assertFalse(cookie.contains("Domain="));
        assertTrue(result.getResponse().getHeader("Cache-Control").contains("no-store"));
        mvc.perform(get("/auth/web/csrf").header(CLIENT, "web").cookie(csrfCookie(result)).secure(true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.csrfToken").value(csrf(result)));
    }

    @Test void webLoginCannotRunWithoutCsrfEvenThoughItIsPublic() throws Exception {
        mvc.perform(post("/auth/log-in").header(CLIENT, "web").contentType("application/json").content("{\"email\":\"test@example.invalid\",\"pwd\":\"fixture\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WEB_CSRF_INVALID"));
        verify(auth, never()).userLogin(any());
    }

    @Test void webLoginReturnsOnlyUserAndCsrfAndSetsSecureHttpOnlyCookies() throws Exception {
        var bootstrap = bootstrap();
        var result = mvc.perform(post("/auth/log-in").header(CLIENT, "web").header(CSRF, csrf(bootstrap))
                .cookie(csrfCookie(bootstrap)).secure(true).contentType("application/json").content("{\"email\":\"test@example.invalid\",\"pwd\":\"fixture\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.userId").value("fixture-user"))
                .andExpect(jsonPath("$.data.sessionToken").doesNotExist()).andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.csrfToken").isString()).andReturn();
        assertNotEquals(csrf(bootstrap), csrf(result));
        for (String name : List.of(SESSION_COOKIE, REFRESH_COOKIE)) {
            String cookie = result.getResponse().getHeaders("Set-Cookie").stream().filter(value -> value.startsWith(name + "=")).findFirst().orElseThrow();
            for (String attribute : List.of("Path=/", "Secure", "HttpOnly", "SameSite=Strict", "Max-Age=")) assertTrue(cookie.contains(attribute), attribute + " missing: " + cookie);
            assertFalse(cookie.contains("Domain="));
            var parsed = java.net.HttpCookie.parse(cookie).getFirst();
            assertTrue(parsed.getMaxAge() > 3600, "Access cookie must survive access expiry for silent refresh");
        }
        assertNull(result.getResponse().getHeader("X-New-Session-Token"));
        assertNull(result.getResponse().getHeader("X-New-Refresh-Token"));
    }

    @Test void cookieAuthenticationCannotDowngradeToUnprotectedBearerMode() throws Exception {
        mvc.perform(post("/users/hydration/data").cookie(authCookies()).header("Authorization", "Bearer " + session).secure(true))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WEB_CSRF_INVALID"));
    }

    @Test void confirmationAndResetRequireCsrfBeforeInvokingTheirService() throws Exception {
        mvc.perform(post("/auth/confirm/registration").header(CLIENT, "web").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/auth/confirm/password/reset").header(CLIENT, "web").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verify(auth, never()).confirmRegistration(any()); verify(auth, never()).confirmPasswordReset(any());
    }

    @Test void serverSessionCheckUsesCookiesAndIncludesCsrf() throws Exception {
        mvc.perform(get("/auth/web/session").header(CLIENT, "web").cookie(authCookies()).secure(true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.userId").value("fixture-user"))
                .andExpect(jsonPath("$.data.csrfToken").isString()).andExpect(jsonPath("$.data.sessionToken").doesNotExist());
    }

    @Test void invalidWebSessionClearsAllCookiesAndKeepsExisting417Contract() throws Exception {
        var result = mvc.perform(get("/auth/web/session").header(CLIENT, "web")
                .cookie(new Cookie(SESSION_COOKIE, "invalid"), new Cookie(REFRESH_COOKIE, "invalid"), new Cookie(CSRF_COOKIE, "old")).secure(true))
                .andExpect(status().isExpectationFailed()).andExpect(jsonPath("$.internalErrorCode").value(3)).andReturn();
        for (String name : List.of(SESSION_COOKIE, REFRESH_COOKIE, CSRF_COOKIE))
            assertTrue(result.getResponse().getHeaders("Set-Cookie").stream().anyMatch(value -> value.startsWith(name + "=") && value.contains("Max-Age=0")));
    }

    @Test void revokedWebJwtCannotRestoreASession() throws Exception {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(AuthModel.builder().email(EMAIL).role("ROLE_USER").tokenRevocationDate(LocalDateTime.now().plusSeconds(2)).build()));
        mvc.perform(get("/auth/web/session").header(CLIENT, "web").cookie(authCookies()).secure(true))
                .andExpect(status().isExpectationFailed());
    }

    @Test void logoutCallsTheExistingRevocationAndClearsCookies() throws Exception {
        var bootstrap = bootstrap();
        var result = mvc.perform(post("/auth/logout").header(CLIENT, "web").header(CSRF, csrf(bootstrap))
                .cookie(authCookies()).cookie(csrfCookie(bootstrap)).secure(true))
                .andExpect(status().isOk()).andReturn();
        verify(auth).logout(EMAIL);
        for (String name : List.of(SESSION_COOKIE, REFRESH_COOKIE, CSRF_COOKIE))
            assertTrue(result.getResponse().getHeaders("Set-Cookie").stream().anyMatch(value -> value.startsWith(name + "=") && value.contains("Max-Age=0")));
    }

    private String token(long issuedAgo, long expiresIn) {
        return io.jsonwebtoken.Jwts.builder().subject(EMAIL)
                .issuedAt(new java.util.Date(System.currentTimeMillis() - issuedAgo))
                .expiration(new java.util.Date(System.currentTimeMillis() + expiresIn))
                .signWith(JwtConfig.SECRET_KEY).compact();
    }

    @Test void expiredAccessRenewsUsingRefreshCookieWithoutExposingTokens() throws Exception {
        session = token(7_200_000, -1000);
        var result = mvc.perform(get("/auth/web/session").header(CLIENT, "web").cookie(authCookies()).secure(true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.userId").value("fixture-user"))
                .andExpect(header().doesNotExist("X-New-Session-Token"))
                .andExpect(header().doesNotExist("X-New-Refresh-Token")).andReturn();
        String cookie = result.getResponse().getHeaders("Set-Cookie").stream().filter(value -> value.startsWith(SESSION_COOKIE + "=")).findFirst().orElseThrow();
        String renewed = java.net.HttpCookie.parse(cookie).getFirst().getValue();
        assertEquals(EMAIL, JwtConfig.obtainClaims(renewed).getSubject());
        assertNotEquals(session, renewed);
        assertTrue(java.net.HttpCookie.parse(cookie).getFirst().getMaxAge() > 3600);
    }

    @Test void refreshNearExpiryRotatesBothWebCookies() throws Exception {
        session = token(7_200_000, -1000);
        refresh = token(18_000_000, 10_800_000);
        var result = mvc.perform(get("/auth/web/session").header(CLIENT, "web").cookie(authCookies()).secure(true))
                .andExpect(status().isOk()).andReturn();
        String renewed = java.net.HttpCookie.parse(result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(REFRESH_COOKIE + "=")).findFirst().orElseThrow()).getFirst().getValue();
        assertNotEquals(refresh, renewed);
        assertTrue(JwtConfig.obtainClaims(renewed).getExpiration().getTime() - System.currentTimeMillis() > 28_000_000);
    }

    @Test void expiredRefreshAndRevokedRefreshCannotRenew() throws Exception {
        session = token(7_200_000, -1000);
        refresh = token(32_400_000, -1000);
        mvc.perform(get("/auth/web/session").header(CLIENT, "web").cookie(authCookies()).secure(true))
                .andExpect(status().isExpectationFailed());
        refresh = token(7_200_000, 100_000);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(AuthModel.builder().email(EMAIL).role("ROLE_USER")
                .tokenRevocationDate(LocalDateTime.now().minusMinutes(30)).build()));
        mvc.perform(get("/auth/web/session").header(CLIENT, "web").cookie(authCookies()).secure(true))
                .andExpect(status().isExpectationFailed());
    }

    @Test void webMarkerNeverFallsBackToBearerAndInvalidLogoutClearsCookies() throws Exception {
        mvc.perform(get("/auth/web/session").header(CLIENT, "web").header("Authorization", "Bearer " + session).secure(true))
                .andExpect(status().isExpectationFailed());
        var bootstrap = bootstrap();
        var result = mvc.perform(post("/auth/logout").header(CLIENT, "web").header(CSRF, csrf(bootstrap))
                .cookie(csrfCookie(bootstrap), new Cookie(SESSION_COOKIE, "invalid")).secure(true))
                .andExpect(status().isExpectationFailed()).andExpect(jsonPath("$.internalErrorCode").value(3)).andReturn();
        assertEquals(3, result.getResponse().getHeaders("Set-Cookie").stream().filter(value -> value.contains("Max-Age=0")).count());
        verify(auth, never()).logout(anyString());
    }

    @Test void wrongCsrfAndFormParameterCannotAuthorizeWebRequest() throws Exception {
        var bootstrap = bootstrap();
        mvc.perform(post("/auth/logout").header(CLIENT, "web").header(CSRF, "wrong")
                .cookie(authCookies()).cookie(csrfCookie(bootstrap)).secure(true))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WEB_CSRF_INVALID"));
        mvc.perform(post("/auth/logout").header(CLIENT, "web").param("_csrf", csrf(bootstrap))
                .cookie(authCookies()).cookie(csrfCookie(bootstrap)).secure(true))
                .andExpect(status().isForbidden());
        verify(auth, never()).logout(anyString());
    }

    @Test void legacyRefreshStillUsesHeaders() throws Exception {
        session = token(7_200_000, -1000);
        refresh = token(18_000_000, 10_800_000);
        mvc.perform(post("/users/hydration/data").header("Authorization", "Bearer " + session).header("X-New-Refresh-Token", refresh))
                .andExpect(status().isOk()).andExpect(header().exists("X-New-Session-Token"))
                .andExpect(header().exists("X-New-Refresh-Token")).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test void confirmationAndPasswordResetAlsoIssueCookiesWithoutLeakingJwt() throws Exception {
        for (String path : List.of("/auth/confirm/registration", "/auth/confirm/password/reset")) {
            var bootstrap = bootstrap();
            var request = path.contains("password") ? put(path) : post(path);
            mvc.perform(request.header(CLIENT, "web").header(CSRF, csrf(bootstrap)).cookie(csrfCookie(bootstrap))
                    .secure(true).contentType("application/json").content("{}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.sessionToken").doesNotExist())
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist()).andExpect(jsonPath("$.data.csrfToken").isString())
                    .andExpect(header().exists("Set-Cookie"));
        }
    }

    @Test void csrfPreflightAllowsLocalWebHeadersButRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/auth/log-in").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", CLIENT + "," + CSRF + ",Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/auth/log-in").header("Origin", "https://attacker.invalid")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", CLIENT + "," + CSRF))
                .andExpect(status().isForbidden());
    }

    @Test void webOriginIsExactEvenWhenLegacyCorsAllowsSiblingDomains() throws Exception {
        mvc.perform(get("/auth/web/csrf").header(CLIENT, "web").header("Origin", "https://evil.moneyfy.cl"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WEB_ORIGIN_DENIED"));
        var bootstrap = bootstrap();
        mvc.perform(post("/auth/logout").header(CLIENT, "web").header("Origin", "https://evil.moneyfy.cl")
                .header(CSRF, csrf(bootstrap)).cookie(authCookies()).cookie(csrfCookie(bootstrap)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WEB_ORIGIN_DENIED"));
        mvc.perform(get("/auth/web/csrf").header(CLIENT, "web").header("Origin", "https://app.moneyfy.cl"))
                .andExpect(status().isOk());
        mvc.perform(get("/auth/web/csrf").header(CLIENT, "web").header("Origin", "https://configured.example.invalid"))
                .andExpect(status().isOk());
        mvc.perform(get("/auth/web/csrf").header(CLIENT, "web").header("Origin", "null"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("WEB_ORIGIN_DENIED"));
        verify(auth, never()).logout(anyString());
    }

    @Test void mobileBearerAndLoginResponseRemainCompatibleWithoutCsrf() throws Exception {
        mvc.perform(post("/auth/log-in").contentType("application/json").content("{\"email\":\"test@example.invalid\",\"pwd\":\"fixture\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sessionToken").value(session)).andExpect(jsonPath("$.data.refreshToken").value(refresh))
                .andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(post("/users/hydration/data").header("Authorization", "Bearer " + session).header("X-New-Refresh-Token", refresh))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.userId").value("fixture-user"));
    }
}
