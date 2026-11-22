package com.gbkhealthcare.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;

/**
 * Fails closed before context refresh if a local run could use another datasource
 * or ask Hibernate to change the schema.
 */
public class LocalDatasourceSafetyInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String EXPECTED_LOCAL_URL = "jdbc:mysql://127.0.0.1:3306/gbk_healthcare_local";
    static final String EXPECTED_LOCAL_USERNAME = "root";
    static final String EXPECTED_LOCAL_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String EXPECTED_LOCAL_DDL_AUTO = "validate";
    static final String EXPECTED_OPD_TEST_URL = "jdbc:mysql://127.0.0.1:3306/gbk_healthcare_opd_test";
    static final String EXPECTED_OPD_TEST_USERNAME = "root";
    static final String EXPECTED_OPD_TEST_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String EXPECTED_OPD_TEST_DDL_AUTO = "validate";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        String[] activeProfiles = environment.getActiveProfiles();

        // With no explicit profile, application.properties can select DB_URL and ddl-auto=update.
        if (activeProfiles.length == 0) {
            throw new IllegalStateException(
                    "An explicit Spring profile is required. Use 'local' or 'opd-test' for local development.");
        }

        String requestedProfiles = environmentVariable((ConfigurableEnvironment) environment,
                "SPRING_PROFILES_ACTIVE");
        if (requestsOpdTestProfile(requestedProfiles)
                && (activeProfiles.length != 1 || !"opd-test".equals(activeProfiles[0]))) {
            throw new IllegalStateException(
                    "SPRING_PROFILES_ACTIVE=opd-test requires opd-test to be the only effective profile.");
        }

        boolean localProfile = environment.acceptsProfiles(Profiles.of("local"));
        boolean opdTestProfile = environment.acceptsProfiles(Profiles.of("opd-test"));
        if (!localProfile && !opdTestProfile) {
            return;
        }
        if (activeProfiles.length != 1) {
            String requiredProfile = opdTestProfile ? "opd-test" : "local";
            throw new IllegalStateException("The " + requiredProfile
                    + " profile must be the only active profile. Refusing datasource initialization.");
        }

        rejectExternalDatasourceOverrides((ConfigurableEnvironment) environment);

        if (localProfile) {
            rejectUnsafeSchemaGeneration(environment, "local");
            validateLocalConfiguration(environment);
            return;
        }

        rejectUnsafeSchemaGeneration(environment, "opd-test");
        requireActiveProfileEnvironmentVariable((ConfigurableEnvironment) environment);
        requireEnvironmentVariable((ConfigurableEnvironment) environment, "LOCAL_DB_PASSWORD");
        requireEnvironmentVariable((ConfigurableEnvironment) environment, "JWT_SECRET_BASE64");
        validateOpdTestConfiguration(environment);
    }

    private static void validateLocalConfiguration(Environment environment) {
        requireExact(environment, "spring.datasource.url", EXPECTED_LOCAL_URL);
        requireExact(environment, "spring.datasource.username", EXPECTED_LOCAL_USERNAME);
        requireExact(environment, "spring.datasource.driver-class-name", EXPECTED_LOCAL_DRIVER);
        requireExact(environment, "spring.jpa.hibernate.ddl-auto", EXPECTED_LOCAL_DDL_AUTO);
    }

    private static void validateOpdTestConfiguration(Environment environment) {
        requireExact(environment, "spring.datasource.url", EXPECTED_OPD_TEST_URL, "opd-test");
        requireExact(environment, "spring.datasource.username", EXPECTED_OPD_TEST_USERNAME, "opd-test");
        requireExact(environment, "spring.datasource.driver-class-name", EXPECTED_OPD_TEST_DRIVER, "opd-test");
        requireExact(environment, "spring.jpa.hibernate.ddl-auto", EXPECTED_OPD_TEST_DDL_AUTO, "opd-test");
    }

    private static void requireActiveProfileEnvironmentVariable(ConfigurableEnvironment environment) {
        String configured = environmentVariable(environment, "SPRING_PROFILES_ACTIVE");
        if (!"opd-test".equals(configured)) {
            throw new IllegalStateException(
                    "The opd-test profile requires SPRING_PROFILES_ACTIVE=opd-test. Refusing startup.");
        }
    }

    private static void requireEnvironmentVariable(ConfigurableEnvironment environment, String name) {
        String value = environmentVariable(environment, name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("The opd-test profile requires the " + name
                    + " environment variable. Refusing startup.");
        }
    }

    private static String environmentVariable(ConfigurableEnvironment environment, String name) {
        PropertySource<?> source = environment.getPropertySources().get("systemEnvironment");
        if (source == null) {
            return null;
        }
        Object value = source.getProperty(name);
        return value == null ? null : value.toString();
    }

    private static boolean requestsOpdTestProfile(String configuredProfiles) {
        if (configuredProfiles == null) {
            return false;
        }
        for (String profile : configuredProfiles.split(",")) {
            if ("opd-test".equals(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void requireExact(Environment environment, String property, String expected) {
        requireExact(environment, property, expected, "local");
    }

    private static void requireExact(Environment environment, String property, String expected, String profile) {
        String actual = resolvedProperty(environment, property);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("The " + profile + " profile requires " + property + "=" + expected
                    + ". Refusing startup before datasource initialization.");
        }
    }

    /**
     * A profile file is the sole source allowed to define datasource properties.
     * This rejects env/system/CLI/JSON and external-file overrides, including
     * alternate pool URLs, JNDI and custom datasource implementations.
     */
    private static void rejectExternalDatasourceOverrides(ConfigurableEnvironment environment) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            String sourceName = source.getName();
            if (isConfigurationPropertiesAdapter(sourceName)) {
                continue;
            }
            boolean bundledConfig = isBundledApplicationConfig(sourceName);
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    String key = compact(name);
                    if (key.startsWith("springdatasource") || key.startsWith("datasource")
                            || key.startsWith("hibernateconnection")
                            || key.startsWith("springjpapropertieshibernateconnection")
                            || key.startsWith("jakartapersistencejdbc")
                            || key.startsWith("javaxpersistencejdbc")
                            || key.startsWith("springjpapropertiesjakartapersistencejdbc")
                            || key.startsWith("springjpapropertiesjavaxpersistencejdbc")) {
                        if (bundledConfig && isAllowedBundledDatasourceProperty(key)) {
                            continue;
                        }
                        throw new IllegalStateException(
                                "Datasource overrides outside bundled application properties are not allowed "
                                        + "with the local profile (source: " + sourceName + ").");
                    }
                }
            }

            // Covers JSON and property sources that do not expose enumerable keys.
            for (String property : new String[] {
                    "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
                    "spring.datasource.driver-class-name", "spring.datasource.jndi-name",
                    "spring.datasource.type", "spring.datasource.hikari.jdbc-url",
                    "spring.datasource.hikari.data-source-class-name"
            }) {
                if (!bundledConfig && source.getProperty(property) != null) {
                    throw new IllegalStateException(
                            "Datasource overrides outside bundled application properties are not allowed "
                                    + "with the local profile (source: " + sourceName + ").");
                }
            }
        }
    }

    private static void rejectUnsafeSchemaGeneration(Environment environment, String profile) {
        String generateDdl = resolvedProperty(environment, "spring.jpa.generate-ddl");
        if (Boolean.parseBoolean(generateDdl)) {
            throw new IllegalStateException(
                    "The " + profile + " profile forbids spring.jpa.generate-ddl=true.");
        }

        for (PropertySource<?> source : ((ConfigurableEnvironment) environment).getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                String key = compact(name);
                if (key.startsWith("springsqlinit") || key.startsWith("springflyway")
                        || key.startsWith("springliquibase")) {
                    throw new IllegalStateException(
                            "SQL initialization and migration configuration are not allowed "
                                    + "with the local safety profile.");
                }
                if (isHibernateHbm2ddlAuto(key)) {
                    String value = resolvedProperty(environment, name);
                    if (!EXPECTED_LOCAL_DDL_AUTO.equalsIgnoreCase(value)) {
                        throw new IllegalStateException(
                                "The " + profile + " profile permits only Hibernate hbm2ddl.auto=validate.");
                    }
                } else if (isAlternateSchemaGenerationSetting(key)) {
                    throw new IllegalStateException(
                            "Alternate Hibernate/JPA schema-generation settings are not allowed "
                                    + "with the " + profile + " profile.");
                }
            }
        }
    }

    private static boolean isHibernateHbm2ddlAuto(String key) {
        return key.equals("springjpapropertieshibernatehbm2ddlauto")
                || key.equals("hibernatehbm2ddlauto");
    }

    private static boolean isAlternateSchemaGenerationSetting(String key) {
        return key.startsWith("jakartapersistenceschemageneration")
                || key.startsWith("javaxpersistenceschemageneration")
                || (key.startsWith("springjpaproperties") && key.contains("schemageneration"))
                || (key.startsWith("hibernatehbm2ddl") && !isHibernateHbm2ddlAuto(key))
                || (key.startsWith("springjpapropertieshibernatehbm2ddl")
                    && !isHibernateHbm2ddlAuto(key));
    }

    private static boolean isConfigurationPropertiesAdapter(String sourceName) {
        return "configurationProperties".equals(sourceName);
    }

    private static boolean isAllowedBundledDatasourceProperty(String key) {
        return key.equals("springdatasourceurl")
                || key.equals("springdatasourceusername")
                || key.equals("springdatasourcepassword")
                || key.equals("springdatasourcedriverclassname");
    }

    private static boolean isBundledApplicationConfig(String sourceName) {
        String normalized = sourceName.toLowerCase();
        return normalized.contains("classpath")
                && (normalized.contains("application.properties")
                    || normalized.contains("application-local.properties")
                    || normalized.contains("application-opd-test.properties"));
    }

    private static String compact(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String resolvedProperty(Environment environment, String property) {
        try {
            return environment.getProperty(property);
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            return null;
        }
    }
}
