package com.gbkhealthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LocalAdminBootstrapTest {
    private static final String EMAIL = "admin.local@gbkhealthcare.test";
    private static final String TEST_PASSWORD = "test-only-password";

    @Test
    void createsEnabledAdminWithEncodedPasswordWhenEnabledAndMissing() {
        RepositoryState repositoryState = new RepositoryState();
        AtomicInteger encodeCalls = new AtomicInteger();

        bootstrap(repositoryState, encodeCalls, true,
                " Admin.Local@GBKHealthCare.Test ", TEST_PASSWORD)
                .run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, repositoryState.lookupCalls);
        assertEquals(1, repositoryState.saveCalls);
        assertEquals(EMAIL, repositoryState.saved.getEmail());
        assertEquals("test-only-bcrypt-hash", repositoryState.saved.getPasswordHash());
        assertTrue(repositoryState.saved.isEnabled());
        assertEquals(1, encodeCalls.get());
    }

    @Test
    void existingAccountIsNotChangedOrReencoded() {
        RepositoryState repositoryState = new RepositoryState();
        repositoryState.existing = new AdminAccount();
        repositoryState.existing.setEmail(EMAIL);
        repositoryState.existing.setPasswordHash("existing-test-hash");
        AtomicInteger encodeCalls = new AtomicInteger();

        bootstrap(repositoryState, encodeCalls, true, EMAIL, TEST_PASSWORD)
                .run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, repositoryState.lookupCalls);
        assertEquals(0, repositoryState.saveCalls);
        assertEquals("existing-test-hash", repositoryState.existing.getPasswordHash());
        assertEquals(0, encodeCalls.get());
    }

    @Test
    void enabledBootstrapRejectsMissingEmailOrPassword() {
        RepositoryState repositoryState = new RepositoryState();
        AtomicInteger encodeCalls = new AtomicInteger();

        assertThrows(IllegalStateException.class,
                () -> bootstrap(repositoryState, encodeCalls, true, "", TEST_PASSWORD)
                        .run(new DefaultApplicationArguments(new String[0])));
        assertThrows(IllegalStateException.class,
                () -> bootstrap(repositoryState, encodeCalls, true, EMAIL, " ")
                        .run(new DefaultApplicationArguments(new String[0])));

        assertEquals(0, repositoryState.lookupCalls);
        assertEquals(0, repositoryState.saveCalls);
        assertEquals(0, encodeCalls.get());
    }

    @Test
    void bootstrapIsDisabledByDefaultAndDoesNotQueryRepository() throws Exception {
        try (AnnotationConfigApplicationContext context = profileContext("local")) {
            context.getBean(LocalAdminBootstrap.class)
                    .run(new DefaultApplicationArguments(new String[0]));
            assertEquals(0, ((RepositoryState) context.getBean("repositoryState")).lookupCalls);
            assertEquals(0, ((RepositoryState) context.getBean("repositoryState")).saveCalls);
        }
    }

    @Test
    void bootstrapComponentIsAvailableOnlyWithLocalProfile() {
        try (AnnotationConfigApplicationContext local = profileContext("local")) {
            assertTrue(local.containsBean("localAdminBootstrap"));
        }
        try (AnnotationConfigApplicationContext production = profileContext("prod")) {
            assertFalse(production.containsBean("localAdminBootstrap"));
        }
    }

    private static LocalAdminBootstrap bootstrap(RepositoryState repositoryState,
                                                   AtomicInteger encodeCalls,
                                                   boolean enabled,
                                                   String email,
                                                   String password) {
        return new LocalAdminBootstrap(repository(repositoryState), encoder(encodeCalls),
                enabled, email, password);
    }

    private static AnnotationConfigApplicationContext profileContext(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        RepositoryState repositoryState = new RepositoryState();
        context.registerBean("repositoryState", RepositoryState.class, () -> repositoryState);
        context.registerBean(AdminAccountRepository.class,
                () -> repository(repositoryState));
        context.registerBean(PasswordEncoder.class, () -> encoder(new AtomicInteger()));
        context.register(LocalAdminBootstrap.class);
        context.refresh();
        return context;
    }

    private static AdminAccountRepository repository(RepositoryState state) {
        return (AdminAccountRepository) Proxy.newProxyInstance(
                AdminAccountRepository.class.getClassLoader(),
                new Class<?>[]{AdminAccountRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmailIgnoreCase" -> {
                        state.lookupCalls++;
                        yield Optional.ofNullable(state.existing);
                    }
                    case "save" -> {
                        state.saveCalls++;
                        state.saved = (AdminAccount) args[0];
                        yield state.saved;
                    }
                    case "toString" -> "LocalAdminBootstrapTestRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static PasswordEncoder encoder(AtomicInteger encodeCalls) {
        return (PasswordEncoder) Proxy.newProxyInstance(
                PasswordEncoder.class.getClassLoader(),
                new Class<?>[]{PasswordEncoder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "encode" -> {
                        encodeCalls.incrementAndGet();
                        yield "test-only-bcrypt-hash";
                    }
                    case "matches", "upgradeEncoding" -> false;
                    case "toString" -> "LocalAdminBootstrapTestEncoder";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class RepositoryState {
        private AdminAccount existing;
        private AdminAccount saved;
        private int lookupCalls;
        private int saveCalls;
    }
}
