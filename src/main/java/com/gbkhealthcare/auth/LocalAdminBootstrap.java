package com.gbkhealthcare.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** One-time, opt-in bootstrap for a local Admin account. */
@Component
@Profile("local")
public class LocalAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LocalAdminBootstrap.class);

    private final AdminAccountRepository admins;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;

    public LocalAdminBootstrap(
            AdminAccountRepository admins,
            PasswordEncoder passwordEncoder,
            @Value("${APP_ADMIN_BOOTSTRAP_ENABLED:false}") boolean enabled,
            @Value("${APP_ADMIN_BOOTSTRAP_EMAIL:}") String email,
            @Value("${APP_ADMIN_BOOTSTRAP_PASSWORD:}") String password) {
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Local Admin bootstrap is enabled but its email or password is missing.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (admins.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            log.info("Local Admin bootstrap found an existing account; no changes made.");
            return;
        }

        AdminAccount admin = new AdminAccount();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setEnabled(true);
        admins.save(admin);

        log.info("Local Admin bootstrap created the initial account.");
    }
}
