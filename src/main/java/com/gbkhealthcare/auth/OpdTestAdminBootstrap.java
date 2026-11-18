package com.gbkhealthcare.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Explicit, one-time bootstrap for an Admin in the isolated opd-test profile. */
@Component
@Profile("opd-test")
public class OpdTestAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OpdTestAdminBootstrap.class);
    private static final String ENABLED_ENV = "OPD_TEST_ADMIN_BOOTSTRAP_ENABLED";
    private static final String EMAIL_ENV = "OPD_TEST_ADMIN_BOOTSTRAP_EMAIL";
    private static final String PASSWORD_ENV = "OPD_TEST_ADMIN_BOOTSTRAP_PASSWORD";
    private static final int MIN_PASSWORD_CHARACTERS = 12;
    private static final int MAX_BCRYPT_BYTES = 72;

    private final AdminAccountRepository admins;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public OpdTestAdminBootstrap(AdminAccountRepository admins, PasswordEncoder passwordEncoder,
                                 Environment environment) {
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length != 1 || !"opd-test".equals(activeProfiles[0])) {
            throw new IllegalStateException(
                    "Admin bootstrap requires opd-test to be the only active Spring profile.");
        }

        String enabledValue = environmentVariable(ENABLED_ENV);
        if (enabledValue == null || enabledValue.isBlank() || "false".equalsIgnoreCase(enabledValue.trim())) {
            return;
        }
        if (!"true".equalsIgnoreCase(enabledValue.trim())) {
            throw new IllegalStateException(ENABLED_ENV + " must be true or false.");
        }

        String emailValue = environmentVariable(EMAIL_ENV);
        String password = environmentVariable(PASSWORD_ENV);
        if (emailValue == null || emailValue.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled but the required Admin email or password environment variable is missing.");
        }

        String email = emailValue.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 160 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("Admin bootstrap email is invalid.");
        }
        if (password.length() < MIN_PASSWORD_CHARACTERS
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalStateException(
                    "Admin bootstrap password must be at least 12 characters and within the BCrypt input limit.");
        }

        if (admins.findByEmailIgnoreCase(email).isPresent()) {
            log.info("opd-test Admin bootstrap found an existing account; no changes made.");
            return;
        }

        AdminAccount admin = new AdminAccount();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setEnabled(true);
        try {
            admins.saveAndFlush(admin);
        } catch (DataIntegrityViolationException duplicateOrConstraintFailure) {
            // The database unique constraint remains the final guard against a concurrent bootstrap.
            // Never log the submitted email, password, or encoded hash.
            throw new IllegalStateException(
                    "Admin bootstrap could not create the account because a database constraint was violated.");
        }
        log.info("opd-test Admin bootstrap created the initial account.");
    }

    private String environmentVariable(String name) {
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            return null;
        }
        PropertySource<?> source = configurableEnvironment.getPropertySources().get("systemEnvironment");
        if (source == null) {
            return null;
        }
        Object value = source.getProperty(name);
        return value == null ? null : value.toString();
    }
}
