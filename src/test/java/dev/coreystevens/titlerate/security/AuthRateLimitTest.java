package dev.coreystevens.titlerate.security;

import dev.coreystevens.titlerate.support.PostgresTestcontainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Register writes a row and login is the credential-stuffing target, so both need a
 * ceiling. Limits are tiny here so the test does not have to send hundreds of
 * requests; the real values live in application.properties.
 */
@ActiveProfiles("test")
@Import(PostgresTestcontainer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "auth.ratelimit.register-per-hour=3",
    "auth.ratelimit.login-per-minute=4",
})
class AuthRateLimitTest {

    @Autowired TestRestTemplate rest;
    @Autowired AuthRateLimiter limiter;

    @BeforeEach
    void clearWindows() {
        limiter.reset();
    }

    private ResponseEntity<String> post(String path, String body) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
    }

    private ResponseEntity<String> register(String email) {
        return post("/api/auth/register", "{\"email\":\"" + email + "\",\"password\":\"a-long-enough-password\"}");
    }

    @Test
    void registerIsCappedPerCaller() {
        for (int i = 1; i <= 3; i++) {
            assertThat(register("ok" + i + "@example.com").getStatusCode())
                .as("request %d of 3 should be allowed", i)
                .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
        assertThat(register("over@example.com").getStatusCode())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void aRejectedRegisterSaysWhenToRetry() {
        for (int i = 1; i <= 3; i++) {
            register("retry" + i + "@example.com");
        }
        var res = register("blocked@example.com");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(res.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    // Logs in with correct credentials rather than wrong ones. A 401 body makes the
    // JDK's HttpURLConnection abandon the request in streaming mode, so a failed
    // login cannot be asserted through TestRestTemplate without swapping the client.
    private ResponseEntity<String> login(String email) {
        return post("/api/auth/login",
            "{\"email\":\"" + email + "\",\"password\":\"a-long-enough-password\"}");
    }

    @Test
    void loginIsCappedSeparatelyFromRegister() {
        register("login-subject@example.com");

        for (int i = 1; i <= 4; i++) {
            assertThat(login("login-subject@example.com").getStatusCode())
                .as("login %d of 4 should be allowed", i)
                .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
        assertThat(login("login-subject@example.com").getStatusCode())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Exhausting login must not spend the register budget: different abuse
        // shapes, sharing only a source address.
        assertThat(register("still-allowed@example.com").getStatusCode())
            .as("register budget must be untouched by login traffic")
            .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void theCalculatorIsNotAffected() {
        register("flooder@example.com");
        for (int i = 0; i < 6; i++) {
            login("flooder@example.com");
        }
        var res = post("/api/calculate",
            "{\"state\":\"PA\",\"policyType\":\"OWNER\",\"amount\":50000,\"simultaneousIssue\":false}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
