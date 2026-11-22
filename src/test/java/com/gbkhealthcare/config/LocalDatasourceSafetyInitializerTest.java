package com.gbkhealthcare.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalDatasourceSafetyInitializerTest {
    private final LocalDatasourceSafetyInitializer initializer = new LocalDatasourceSafetyInitializer();

    @Test
    void allowsCorrectLocalConfiguration() {
        assertDoesNotThrow(() -> initialize("local", localProperties()));
    }

    @Test
    void allowsCorrectOpdTestConfigurationWithRequiredEnvironmentVariables() {
        assertDoesNotThrow(() -> initializeOpdTest("opd-test", opdTestProperties(), opdTestEnvironmentVariables()));
    }

    @Test
    void opdTestRequiresExactEnvironmentProfileAndOnlyOneActiveProfile() {
        Map<String, Object> missingProfileVariable = opdTestEnvironmentVariables();
        missingProfileVariable.remove("SPRING_PROFILES_ACTIVE");
        assertRejectedOpdTest("opd-test", opdTestProperties(), missingProfileVariable);

        Map<String, Object> wrongProfileVariable = opdTestEnvironmentVariables();
        wrongProfileVariable.put("SPRING_PROFILES_ACTIVE", "local");
        assertRejectedOpdTest("opd-test", opdTestProperties(), wrongProfileVariable);

        assertRejectedOpdTest("opd-test,prod", opdTestProperties(), opdTestEnvironmentVariables());
        assertRejectedOpdTest("prod", opdTestProperties(), opdTestEnvironmentVariables());
    }

    @Test
    void opdTestRequiresDatabasePasswordAndJwtSecretEnvironmentVariables() {
        Map<String, Object> missingDatabasePassword = opdTestEnvironmentVariables();
        missingDatabasePassword.remove("LOCAL_DB_PASSWORD");
        assertRejectedOpdTest("opd-test", opdTestProperties(), missingDatabasePassword);

        Map<String, Object> blankDatabasePassword = opdTestEnvironmentVariables();
        blankDatabasePassword.put("LOCAL_DB_PASSWORD", " ");
        assertRejectedOpdTest("opd-test", opdTestProperties(), blankDatabasePassword);

        Map<String, Object> missingJwtSecret = opdTestEnvironmentVariables();
        missingJwtSecret.remove("JWT_SECRET_BASE64");
        assertRejectedOpdTest("opd-test", opdTestProperties(), missingJwtSecret);

        Map<String, Object> blankJwtSecret = opdTestEnvironmentVariables();
        blankJwtSecret.put("JWT_SECRET_BASE64", " ");
        assertRejectedOpdTest("opd-test", opdTestProperties(), blankJwtSecret);
    }

    @Test
    void opdTestAcceptsOnlyItsExactLocalhostDatabaseUrl() {
        for (String unsafeUrl : new String[] {
                LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_URL,
                "jdbc:mysql://127.0.0.1:3306/gbk_healthcare",
                "jdbc:mysql://aws.example.invalid/gbk_healthcare",
                "jdbc:mysql://tidb.example.invalid/gbk_healthcare",
                "jdbc:mysql://127.0.0.1:3307/gbk_healthcare_opd_test"
        }) {
            Map<String, Object> properties = opdTestProperties();
            properties.put("spring.datasource.url", unsafeUrl);
            assertRejectedOpdTest("opd-test", properties, opdTestEnvironmentVariables());
        }
    }

    @Test
    void opdTestRejectsIncorrectOrMissingDdlSetting() {
        Map<String, Object> wrong = opdTestProperties();
        wrong.put("spring.jpa.hibernate.ddl-auto", "update");
        assertRejectedOpdTest("opd-test", wrong, opdTestEnvironmentVariables());

        Map<String, Object> missing = opdTestProperties();
        missing.remove("spring.jpa.hibernate.ddl-auto");
        assertRejectedOpdTest("opd-test", missing, opdTestEnvironmentVariables());

        Map<String, Object> alternate = opdTestProperties();
        alternate.put("spring.jpa.properties.hibernate.hbm2ddl.auto", "create-drop");
        assertRejectedOpdTest("opd-test", alternate, opdTestEnvironmentVariables());

        Map<String, Object> sqlInit = opdTestProperties();
        sqlInit.put("spring.sql.init.mode", "always");
        assertRejectedOpdTest("opd-test", sqlInit, opdTestEnvironmentVariables());

        Map<String, Object> generateDdl = opdTestProperties();
        generateDdl.put("spring.jpa.generate-ddl", "true");
        assertRejectedOpdTest("opd-test", generateDdl, opdTestEnvironmentVariables());

        Map<String, Object> schemaGeneration = opdTestProperties();
        schemaGeneration.put("jakarta.persistence.schema-generation.database.action", "create");
        assertRejectedOpdTest("opd-test", schemaGeneration, opdTestEnvironmentVariables());

        Map<String, Object> migrationRunner = opdTestProperties();
        migrationRunner.put("spring.flyway.enabled", "true");
        assertRejectedOpdTest("opd-test", migrationRunner, opdTestEnvironmentVariables());
    }

    @Test
    void opdTestRejectsDatasourceOverridesAndAlternativeDataSources() {
        for (Map.Entry<String, Object> override : Map.<String, Object>of(
                "spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_URL,
                "spring.datasource.hikari.jdbc-url", "jdbc:mysql://other.example.invalid/other",
                "spring.datasource.jndi-name", "java:comp/env/jdbc/other",
                "spring.datasource.type", "com.example.CustomDataSource").entrySet()) {
            MapPropertySource source = new MapPropertySource("commandLineArgs", Map.of(override.getKey(), override.getValue()));
            assertRejectedOpdTestWithSource(source);
        }

        assertRejectedOpdTestWithSource(new MapPropertySource("externalConfig",
                Map.of("spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_URL)));

        assertRejectedOpdTestWithSource(new SystemEnvironmentPropertySource("externalEnvironment",
                Map.of("SPRING_DATASOURCE_URL", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_URL)));
        assertRejectedOpdTestWithSource(new MapPropertySource("systemProperties",
                Map.of("spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_URL)));
    }

    @Test
    void rejectsIncorrectAndMissingDatasourceUrl() {
        Map<String, Object> wrong = localProperties();
        wrong.put("spring.datasource.url", "jdbc:mysql://remote.example/gbk_healthcare");
        assertRejected("local", wrong);

        Map<String, Object> missing = localProperties();
        missing.remove("spring.datasource.url");
        assertRejected("local", missing);
    }

    @Test
    void rejectsIncorrectAndMissingDdlSetting() {
        Map<String, Object> wrong = localProperties();
        wrong.put("spring.jpa.hibernate.ddl-auto", "update");
        assertRejected("local", wrong);

        Map<String, Object> missing = localProperties();
        missing.remove("spring.jpa.hibernate.ddl-auto");
        assertRejected("local", missing);
    }

    @Test
    void rejectsHikariUrlOverride() {
        Map<String, Object> properties = localProperties();
        properties.put("spring.datasource.hikari.jdbc-url", "jdbc:mysql://remote.example/prod");
        assertRejected("local", properties);
    }

    @Test
    void rejectsJndiDatasourceConfiguration() {
        Map<String, Object> properties = localProperties();
        properties.put("spring.datasource.jndi-name", "java:comp/env/jdbc/production");
        assertRejected("local", properties);
    }

    @Test
    void rejectsAlternativeDatasourceConfiguration() {
        Map<String, Object> properties = localProperties();
        properties.put("spring.datasource.type", "com.example.CustomDataSource");
        assertRejected("local", properties);
    }

    @Test
    void rejectsAlternateHibernateSchemaGenerationSettings() {
        Map<String, Object> hbm2ddl = localProperties();
        hbm2ddl.put("spring.jpa.properties.hibernate.hbm2ddl.auto", "create-drop");
        assertRejected("local", hbm2ddl);

        Map<String, Object> jpaAction = localProperties();
        jpaAction.put("jakarta.persistence.schema-generation.database.action", "create");
        assertRejected("local", jpaAction);

        Map<String, Object> generateDdl = localProperties();
        generateDdl.put("spring.jpa.generate-ddl", "true");
        assertRejected("local", generateDdl);

        Map<String, Object> sqlInit = localProperties();
        sqlInit.put("spring.sql.init.mode", "always");
        assertRejected("local", sqlInit);
    }

    @Test
    void rejectsEnvironmentSystemAndCommandLineDatasourceOverrides() {
        assertRejectedWithSource(new SystemEnvironmentPropertySource("systemEnvironment",
                Map.of("SPRING_DATASOURCE_URL", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_URL)));
        assertRejectedWithSource(new MapPropertySource("systemProperties",
                Map.of("spring.datasource.username", "root")));
        assertRejectedWithSource(new MapPropertySource("commandLineArgs",
                Map.of("spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_URL)));
    }

    @Test
    void rejectsExternalConfigurationDatasourceOverrides() {
        assertRejectedWithSource(new MapPropertySource("externalConfig",
                Map.of("spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_URL)));
    }

    @Test
    void rejectsMissingActiveProfileToAvoidBaseConfigurationFallback() {
        assertRejected(null, localProperties());
    }

    @Test
    void nonLocalProfileBehaviorIsUnaffected() {
        assertDoesNotThrow(() -> initialize("prod", Map.of()));
        assertDoesNotThrow(() -> initialize("test", Map.of()));
    }

    @Test
    void localCannotBeCombinedWithAnotherProfile() {
        assertRejected("local,prod", localProperties());
    }

    private void assertRejected(String profiles, Map<String, Object> properties) {
        assertThrows(IllegalStateException.class, () -> initialize(profiles, properties));
    }

    private void assertRejectedWithSource(PropertySource<?> override) {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.getPropertySources().addFirst(localConfigSource(localProperties()));
        environment.getPropertySources().addFirst(override);
        context.setEnvironment(environment);
        try {
            assertThrows(IllegalStateException.class, () -> initializer.initialize(context));
        } finally {
            context.close();
        }
    }

    private void assertRejectedOpdTest(String profiles, Map<String, Object> properties,
            Map<String, Object> environmentVariables) {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles.split(","));
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("systemEnvironment", environmentVariables));
        environment.getPropertySources().addFirst(opdTestConfigSource(properties));
        context.setEnvironment(environment);
        try {
            assertThrows(IllegalStateException.class, () -> initializer.initialize(context));
        } finally {
            context.close();
        }
    }

    private void assertRejectedOpdTestWithSource(PropertySource<?> override) {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("opd-test");
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "systemEnvironment", opdTestEnvironmentVariables()));
        environment.getPropertySources().addFirst(opdTestConfigSource(opdTestProperties()));
        environment.getPropertySources().addFirst(override);
        context.setEnvironment(environment);
        try {
            assertThrows(IllegalStateException.class, () -> initializer.initialize(context));
        } finally {
            context.close();
        }
    }

    private void initializeOpdTest(String profiles, Map<String, Object> properties,
            Map<String, Object> environmentVariables) {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles.split(","));
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("systemEnvironment", environmentVariables));
        environment.getPropertySources().addFirst(opdTestConfigSource(properties));
        context.setEnvironment(environment);
        try {
            initializer.initialize(context);
        } finally {
            context.close();
        }
    }

    private void initialize(String profiles, Map<String, Object> localProperties) {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment();
        if (profiles != null) {
            environment.setActiveProfiles(profiles.split(","));
        }
        environment.getPropertySources().addFirst(localConfigSource(localProperties));
        context.setEnvironment(environment);
        try {
            initializer.initialize(context);
        } finally {
            context.close();
        }
    }

    private static Map<String, Object> localProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_URL);
        properties.put("spring.datasource.username", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_USERNAME);
        properties.put("spring.datasource.password", "${LOCAL_DB_PASSWORD}");
        properties.put("spring.datasource.driver-class-name", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_DRIVER);
        properties.put("spring.jpa.hibernate.ddl-auto", LocalDatasourceSafetyInitializer.EXPECTED_LOCAL_DDL_AUTO);
        return properties;
    }

    private static Map<String, Object> opdTestProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_URL);
        properties.put("spring.datasource.username", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_USERNAME);
        properties.put("spring.datasource.password", "${LOCAL_DB_PASSWORD}");
        properties.put("spring.datasource.driver-class-name", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_DRIVER);
        properties.put("spring.jpa.hibernate.ddl-auto", LocalDatasourceSafetyInitializer.EXPECTED_OPD_TEST_DDL_AUTO);
        return properties;
    }

    private static Map<String, Object> opdTestEnvironmentVariables() {
        return new HashMap<>(Map.of(
                "SPRING_PROFILES_ACTIVE", "opd-test",
                "LOCAL_DB_PASSWORD", "test-only-placeholder",
                "JWT_SECRET_BASE64", "test-only-placeholder"));
    }

    private static MapPropertySource localConfigSource(Map<String, Object> properties) {
        return new MapPropertySource("applicationConfig: [classpath:/application-local.properties]", properties);
    }

    private static MapPropertySource opdTestConfigSource(Map<String, Object> properties) {
        return new MapPropertySource("applicationConfig: [classpath:/application-opd-test.properties]", properties);
    }
}
