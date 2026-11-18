package com.gbkhealthcare.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminAuthController {
    private static final String REFRESH_COOKIE = "admin_refresh";
    private static final String CSRF_COOKIE = "admin_csrf";
    private final AdminAuthService auth;
    private final AdminRefreshSessionService sessions;
    private final String cookieDomain, cookieSameSite;
    private final long refreshLifetime;
    private final boolean secureCookies;
    private final String[] allowedOrigins;

    public AdminAuthController(AdminAuthService auth, AdminRefreshSessionService sessions,
            @Value("${app.security.cookie-domain:}") String cookieDomain,
            @Value("${app.security.admin-refresh-expiration-seconds:2592000}") long refreshLifetime,
            @Value("${app.security.cookies-secure:true}") boolean secureCookies,
            @Value("${app.security.cookie-same-site:None}") String cookieSameSite,
            @Value("${app.security.allowed-origins:http://localhost:5173,https://main.d2jr9663g6l11x.amplifyapp.com}") String allowedOrigins) {
        this.auth = auth; this.sessions = sessions; this.cookieDomain = cookieDomain; this.refreshLifetime = refreshLifetime;
        this.secureCookies = secureCookies; this.cookieSameSite = cookieSameSite; this.allowedOrigins = allowedOrigins.split(",");
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody AdminLoginRequest request,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireTrustedOrigin(origin);
        try {
            AdminAuthService.LoginResult result = auth.login(request);
            String csrf = newCsrfToken();
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(result.session().refreshToken(), refreshLifetime).toString())
                    .header(HttpHeaders.SET_COOKIE, csrfCookie(csrf, refreshLifetime).toString())
                    .body(new AccessTokenResponse(result.accessToken(), "Bearer", result.expiresInSeconds(), result.adminEmail(), csrf));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireCsrf(csrfCookie, csrfHeader, origin);
        AdminRefreshSessionService.IssuedSession session;
        try { session = sessions.rotate(refreshToken); }
        catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) return clearCookiesForAccess(HttpStatus.UNAUTHORIZED);
            throw ex;
        }
        String csrf = newCsrfToken();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), refreshLifetime).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie(csrf, refreshLifetime).toString())
                .body(new AccessTokenResponse(session.accessToken(), "Bearer", auth.getAccessLifetime(), session.adminEmail(), csrf));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(@CookieValue(name = CSRF_COOKIE, required = false) String existing,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireTrustedOrigin(origin);
        String csrf = existing == null || existing.isBlank() ? newCsrfToken() : existing;
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, csrfCookie(csrf, refreshLifetime).toString()).body(new CsrfTokenResponse(csrf));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireCsrf(csrfCookie, csrfHeader, origin);
        sessions.revoke(refreshToken);
        return clearCookies(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<Void> clearCookies(HttpStatus status) {
        return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", 0).toString()).build();
    }
    private ResponseEntity<AccessTokenResponse> clearCookiesForAccess(HttpStatus status) {
        return ResponseEntity.<AccessTokenResponse>status(status).header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", 0).toString()).build();
    }
    private void requireCsrf(String cookie, String header, String origin) {
        requireTrustedOrigin(origin);
        if (cookie == null || header == null || !MessageDigest.isEqual(cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CSRF validation failed.");
    }
    private void requireTrustedOrigin(String origin) {
        if (origin == null || Arrays.stream(allowedOrigins).map(String::trim).noneMatch(origin::equals))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request origin is not allowed.");
    }
    private ResponseCookie refreshCookie(String value, long age) { return cookie(REFRESH_COOKIE, value, true, age); }
    private ResponseCookie csrfCookie(String value, long age) { return cookie(CSRF_COOKIE, value, false, age); }
    private ResponseCookie cookie(String name, String value, boolean httpOnly, long age) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value).httpOnly(httpOnly).secure(secureCookies)
                .sameSite(cookieSameSite).path("/api/auth/admin").maxAge(Duration.ofSeconds(age));
        if (!cookieDomain.isBlank()) builder.domain(cookieDomain);
        return builder.build();
    }
    private static String newCsrfToken() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }

    public record AccessTokenResponse(String accessToken, String tokenType, long expiresInSeconds, String adminEmail, String csrfToken) {}
    public record CsrfTokenResponse(String csrfToken) {}
}
