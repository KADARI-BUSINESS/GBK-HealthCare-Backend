package com.gbkhealthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpdTestAdminBootstrapTest {
    private static final String EMAIL_ENV = "OPD_TEST_ADMIN_BOOTSTRAP_EMAIL";
    private static final String PASSWORD_ENV = "OPD_TEST_ADMIN_BOOTSTRAP_PASSWORD";
    private static final String ENABLED_ENV = "OPD_TEST_ADMIN_BOOTSTRAP_ENABLED";
    private static final String PASSWORD = "only-a-test-password-123";

    @Test
    void bootstrapIsDisabledWhenEnableFlagIsAbsent() {
        AdminAccountRepository admins = mock(AdminAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        OpdTestAdminBootstrap bootstrap = bootstrap(admins, encoder, environment("opd-test", Map.of()));

        bootstrap.run(arguments());

        verifyNoInteractions(admins, encoder);
    }

    @Test
    void bootstrapComponentExistsOnlyWhenOpdTestProfileIsActive() {
        try (AnnotationConfigApplicationContext opdTest = profileContext("opd-test")) {
            assertTrue(opdTest.containsBean("opdTestAdminBootstrap"));
        }
        try (AnnotationConfigApplicationContext production = profileContext("prod")) {
            assertFalse(production.containsBean("opdTestAdminBootstrap"));
        }
    }

    @Test
    void enabledBootstrapRejectsMissingEmailOrPassword() {
        AdminAccountRepository admins = mock(AdminAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        OpdTestAdminBootstrap missingEmail = bootstrap(admins, encoder,
                environment("opd-test", Map.of(ENABLED_ENV, "true", PASSWORD_ENV, PASSWORD)));
        OpdTestAdminBootstrap missingPassword = bootstrap(admins, encoder,
                environment("opd-test", Map.of(ENABLED_ENV, "true", EMAIL_ENV, "admin@example.test")));

        assertThrows(IllegalStateException.class, () -> missingEmail.run(arguments()));
        assertThrows(IllegalStateException.class, () -> missingPassword.run(arguments()));
        verifyNoInteractions(admins, encoder);
    }

    @Test
    void enabledBootstrapRequiresOpdTestToBeTheOnlyActiveProfile() {
        AdminAccountRepository admins = mock(AdminAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        OpdTestAdminBootstrap bootstrap = bootstrap(admins, encoder,
                environment(new String[] {"opd-test", "prod"}, Map.of(ENABLED_ENV, "true")));

        assertThrows(IllegalStateException.class, () -> bootstrap.run(arguments()));
        verifyNoInteractions(admins, encoder);
    }

    @Test
    void normalizesEmailAndStoresBcryptHashForEnabledAccount() {
        AdminAccountRepository admins = mock(AdminAccountRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        when(admins.findByEmailIgnoreCase("admin.opd@example.test")).thenReturn(Optional.empty());
        when(admins.saveAndFlush(any(AdminAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, String> env = Map.of(ENABLED_ENV, "true", EMAIL_ENV,
                " Admin.OPD@Example.Test ", PASSWORD_ENV, PASSWORD);

        bootstrap(admins, encoder, environment("opd-test", env)).run(arguments());

        var captor = org.mockito.ArgumentCaptor.forClass(AdminAccount.class);
        verify(admins).saveAndFlush(captor.capture());
        AdminAccount saved = captor.getValue();
        assertEquals("admin.opd@example.test", saved.getEmail());
        assertTrue(saved.isEnabled());
        assertNotEquals(PASSWORD, saved.getPasswordHash());
        assertTrue(encoder.matches(PASSWORD, saved.getPasswordHash()));
    }

    @Test
    void existingAdminIsNeverOverwrittenOrReencoded() {
        AdminAccountRepository admins = mock(AdminAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AdminAccount existing = new AdminAccount();
        existing.setEmail("admin@example.test");
        existing.setPasswordHash("existing-hash");
        when(admins.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(existing));

        bootstrap(admins, encoder, environment("opd-test", Map.of(ENABLED_ENV, "true",
                EMAIL_ENV, "ADMIN@example.test", PASSWORD_ENV, PASSWORD))).run(arguments());

        assertEquals("existing-hash", existing.getPasswordHash());
        verify(admins, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void rejectsInvalidEmailAndBcryptOversizedPassword() {
        AdminAccountRepository admins = mock(AdminAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        assertThrows(IllegalStateException.class, () -> bootstrap(admins, encoder,
                environment("opd-test", Map.of(ENABLED_ENV, "true", EMAIL_ENV, "bad-email",
                        PASSWORD_ENV, PASSWORD))).run(arguments()));
        assertThrows(IllegalStateException.class, () -> bootstrap(admins, encoder,
                environment("opd-test", Map.of(ENABLED_ENV, "true", EMAIL_ENV, "admin@example.test",
                        PASSWORD_ENV, "x".repeat(73)))).run(arguments()));
        verifyNoInteractions(admins, encoder);
    }

    private static OpdTestAdminBootstrap bootstrap(AdminAccountRepository admins, PasswordEncoder encoder,
                                                    StandardEnvironment environment) {
        return new OpdTestAdminBootstrap(admins, encoder, environment);
    }

    private static StandardEnvironment environment(String profile, Map<String, String> variables) {
        return environment(new String[] {profile}, variables);
    }

    private static StandardEnvironment environment(String[] profiles, Map<String, String> variables) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profiles);
        environment.getPropertySources().addFirst(new MapPropertySource("systemEnvironment",
                new HashMap<>(variables)));
        return environment;
    }

    private static AnnotationConfigApplicationContext profileContext(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.registerBean(AdminAccountRepository.class, () -> mock(AdminAccountRepository.class));
        context.registerBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class));
        context.register(OpdTestAdminBootstrap.class);
        context.refresh();
        return context;
    }

    private static DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
