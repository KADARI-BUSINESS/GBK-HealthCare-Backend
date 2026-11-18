package com.gbkhealthcare.auth;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.MySQLDialect;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdminSchemaValidationTest {
    private static final String JDBC_URL = "jdbc:h2:mem:admin-schema-validation;MODE=MySQL;DB_CLOSE_DELAY=-1";

    @Test
    void adminSchemaMatchesHibernateMySqlMappings() throws Exception {
        createIsolatedReviewSchema();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", JDBC_URL)
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.dialect", MySQLDialect.class.getName())
                .applySetting("hibernate.hbm2ddl.auto", "validate")
                .build();

        try {
            assertDoesNotThrow(() -> {
                try (SessionFactory ignored = new MetadataSources(registry)
                        .addAnnotatedClass(AdminAccount.class)
                        .addAnnotatedClass(AdminRefreshSession.class)
                        .buildMetadata()
                        .buildSessionFactory()) {
                    // Building the SessionFactory runs Hibernate schema validation.
                }
            });
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static void createIsolatedReviewSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE admin_accounts (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        email VARCHAR(160) NOT NULL,
                        password_hash VARCHAR(100) NOT NULL,
                        enabled BIT(1) NOT NULL DEFAULT 1,
                        created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        PRIMARY KEY (id),
                        CONSTRAINT uk_admin_accounts_email UNIQUE (email)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE admin_refresh_sessions (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        admin_id BIGINT NOT NULL,
                        family_id VARCHAR(36) NOT NULL,
                        token_hash VARCHAR(64) NOT NULL,
                        created_at TIMESTAMP(6) NOT NULL,
                        expires_at TIMESTAMP(6) NOT NULL,
                        revoked_at TIMESTAMP(6) NULL,
                        PRIMARY KEY (id),
                        CONSTRAINT uk_admin_refresh_token_hash UNIQUE (token_hash),
                        INDEX idx_admin_refresh_admin_id (admin_id),
                        INDEX idx_admin_refresh_family (family_id),
                        CONSTRAINT fk_admin_refresh_admin FOREIGN KEY (admin_id) REFERENCES admin_accounts(id)
                    )
                    """);
        }
    }
}
