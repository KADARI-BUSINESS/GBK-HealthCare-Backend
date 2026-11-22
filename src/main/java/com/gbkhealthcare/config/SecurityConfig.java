
package com.gbkhealthcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                // Cookie-based authentication endpoints perform
                // their own Origin and double-submit CSRF checks.
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**")
                        .permitAll()

                        // Public doctor registration.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/doctor/register")
                        .permitAll()

                        // Existing doctor authentication endpoints.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/doctor/login",
                                "/api/auth/doctor/refresh",
                                "/api/auth/doctor/logout")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/doctor/csrf")
                        .permitAll()

                        // Admin authentication is a separate cookie/session flow.
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/admin/login", "/api/auth/admin/refresh", "/api/auth/admin/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/admin/csrf")
                        .permitAll()

                        // Staff sessions are independent from Doctor and Admin sessions.
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/staff/login", "/api/auth/staff/refresh", "/api/auth/staff/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/staff/csrf")
                        .permitAll()

                        .requestMatchers("/api/admin/**")
                        .hasAuthority("SCOPE_ADMIN")

                        .requestMatchers("/api/staff/**")
                        .hasAuthority("SCOPE_STAFF")

                        // Reception is Staff-authenticated and role-checked again in its controller.
                        .requestMatchers("/api/reception/**")
                        .hasAuthority("SCOPE_STAFF")

                        // Nursing is Staff-authenticated and role-checked in its controller.
                        .requestMatchers("/api/nursing/**")
                        .hasAuthority("SCOPE_STAFF")

                        // Billing is shared clinic work, restricted to Staff scope and then CHECKOUT role in services.
                        .requestMatchers("/api/billing/**")
                        .hasAuthority("SCOPE_STAFF")

                        // Other API endpoints require Doctor JWT.
                        .requestMatchers("/api/**")
                        .hasAuthority("SCOPE_DOCTOR")

                        .anyRequest().permitAll())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }
}
