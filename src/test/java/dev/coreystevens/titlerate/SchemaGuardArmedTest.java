package dev.coreystevens.titlerate;

import dev.coreystevens.titlerate.support.PostgresTestcontainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the schema guard is switched on, because it is easy to switch off by
 * accident and silent when it is.
 *
 * A file at src/test/resources/application.properties occupies the same classpath
 * path as the main one and replaces it outright instead of merging, which drops
 * ddl-auto and spring.flyway.enabled for the whole suite. A stale copy left in
 * target/test-classes does the same thing after the source file is deleted. Both
 * happened while this change was being written, and three deliberate schema
 * mutations passed before either was noticed.
 *
 * What ddl-auto=validate does and does not cover, measured rather than assumed:
 * a renamed table and a renamed column both fail the boot, while changing
 * "timestamp with time zone" to "timestamp" does not. It is a structural check, so
 * a column type drifting from its entity is not something it will catch.
 */
@ActiveProfiles("test")
@Import(PostgresTestcontainer.class)
@SpringBootTest
class SchemaGuardArmedTest {

    @Autowired Environment env;

    @Test
    void hibernateValidatesRatherThanRewritingTheSchema() {
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto"))
            .as("create-drop would wipe every row on restart; none would skip the check")
            .isEqualTo("validate");
    }

    @Test
    void flywayOwnsTheSchema() {
        assertThat(env.getProperty("spring.flyway.enabled")).isEqualTo("true");
    }

    @Test
    void theTestProfileSuppliesASigningKeyWithoutShadowingTheMainConfig() {
        assertThat(env.getProperty("jwt.secret")).isNotBlank();
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isNotNull();
    }
}
