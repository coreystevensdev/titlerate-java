package dev.coreystevens.titlerate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real embedded server rather than MockMvc, and with no mock user.
 *
 * Both details matter. Every other test here carries @WithMockUser, which makes the
 * servlet error dispatch authenticated, and MockMvc resolves handler exceptions
 * in-process without dispatching to /error at all. Between them, a permitAll
 * endpoint can answer 400 in the suite and 403 to a real anonymous client, which is
 * what it was doing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/test-rates.sql")
class AnonymousErrorResponseTest {

    @Autowired TestRestTemplate rest;

    private ResponseEntity<String> postCalculate(String body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/calculate", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    @Test
    void invalidStateReturns400ToAnAnonymousCaller() {
        var res = postCalculate("{\"state\":\"XX\",\"policyType\":\"OWNER\",\"amount\":100000,\"simultaneousIssue\":false}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unconfiguredStatePolicyReturns404ToAnAnonymousCaller() {
        var res = postCalculate("{\"state\":\"NJ\",\"policyType\":\"OWNER\",\"amount\":50000,\"simultaneousIssue\":false}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Not 404, deliberately. An unknown path is rejected by the filter chain before
    // any handler runs, so there is nothing to report as missing, and answering 404
    // would tell an unauthenticated caller which paths exist. Pinned so that
    // permitting /error never accidentally starts disclosing the route table.
    @Test
    void unknownPathIsRejectedRatherThanDisclosed() {
        var res = rest.getForEntity("/api/nonexistent", String.class);
        assertThat(res.getStatusCode())
            .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void aValidRequestStillWorksAnonymously() {
        var res = postCalculate("{\"state\":\"PA\",\"policyType\":\"OWNER\",\"amount\":50000,\"simultaneousIssue\":false}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("basePremium");
    }
}
