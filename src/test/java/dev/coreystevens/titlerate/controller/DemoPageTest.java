package dev.coreystevens.titlerate.controller;

import dev.coreystevens.titlerate.support.PostgresTestcontainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * anyRequest().authenticated() covers the root, so without an explicit
 * permitAll the demo page answers 401 to the visitor it exists for.
 */
@ActiveProfiles("test")
@Import(PostgresTestcontainer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoPageTest {

    @Autowired TestRestTemplate rest;

    @Test
    void rootServesThePageToAnAnonymousCaller() {
        ResponseEntity<String> res = rest.getForEntity("/", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType()).isNotNull();
        assertThat(res.getHeaders().getContentType().toString()).startsWith("text/html");
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().toLowerCase()).contains("<!doctype html");
    }

    /**
     * The page posts to /api/calculate, so it has to name the same fields the
     * PremiumRequest record binds and the PremiumResponse returns. A rename on
     * either side would otherwise break the form with nothing failing.
     */
    @Test
    void pageNamesEveryFieldTheApiBinds() {
        String body = rest.getForObject("/", String.class);

        assertThat(body).isNotNull();
        assertThat(body)
            .contains("state")
            .contains("policyType")
            .contains("amount")
            .contains("simultaneousIssue")
            .contains("/api/calculate");

        assertThat(body)
            .contains("basePremium")
            .contains("simultaneousDiscount")
            .contains("netPremium")
            .contains("ratePerThousandApplied")
            .contains("breakdown");
    }

    /**
     * Permitting the root must not widen anything else. /api/auth/me is the
     * canary: it is the one authenticated route with no business data behind it.
     */
    @Test
    void permittingTheRootDoesNotOpenAuthenticatedRoutes() {
        ResponseEntity<String> res = rest.getForEntity("/api/auth/me", String.class);

        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
