package dev.coreystevens.titlerate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.coreystevens.titlerate.dto.PremiumRequest;
import dev.coreystevens.titlerate.model.PolicyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/test-rates.sql")
class PremiumControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser
    void calculate_paOwner50k_returns175() throws Exception {
        var req = new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("50000"), false);

        mvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.basePremium").value(closeTo(175.00, 0.01)))
            .andExpect(jsonPath("$.simultaneousDiscount").value(closeTo(0.00, 0.01)))
            .andExpect(jsonPath("$.netPremium").value(closeTo(175.00, 0.01)));
    }

    @Test
    @WithMockUser
    void calculate_paLenderSimultaneous_appliesDiscount() throws Exception {
        // $80k is clearly in tier 1 (0-100k exclusive): rate=2.75, 30% simultaneous discount
        // base = 80 * 2.75 = 220.00, discount = 220 * 0.30 = 66.00, net = 154.00
        var req = new PremiumRequest("PA", PolicyType.LENDER, new BigDecimal("80000"), true);

        mvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simultaneousDiscount").value(closeTo(66.00, 0.01)))
            .andExpect(jsonPath("$.netPremium").value(closeTo(154.00, 0.01)));
    }

    @Test
    @WithMockUser
    void calculate_invalidState_returns400() throws Exception {
        var body = """
            {"state":"XX","policyType":"OWNER","amount":100000,"simultaneousIssue":false}
            """;

        mvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void calculate_unconfiguredStatePolicy_returns404NotRawServerError() throws Exception {
        // NJ passes the @Pattern(state) check, but test-rates.sql only seeds PA tiers.
        var req = new PremiumRequest("NJ", PolicyType.OWNER, new BigDecimal("50000"), false);

        mvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(
                "No rate schedule found for state=NJ policyType=OWNER amount=50000"));
    }
}
