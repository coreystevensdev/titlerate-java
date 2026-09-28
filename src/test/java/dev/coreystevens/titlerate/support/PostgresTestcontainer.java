package dev.coreystevens.titlerate.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Real Postgres for the test suite, replacing in-memory H2.
 *
 * H2 could not stay once Flyway owned the schema: the migrations would have had to
 * be written in whatever SQL both engines accept, which means the tests would never
 * exercise the dialect production runs on. The generated schema already relies on
 * Postgres specifics, "timestamp with time zone" and IDENTITY columns among them.
 *
 * One bean, so Spring's context cache hands the same container to every test class
 * that imports it rather than starting one per class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainer {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Testcontainers closes it via its own JVM shutdown hook
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
