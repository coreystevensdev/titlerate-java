package dev.coreystevens.titlerate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.coreystevens.titlerate.dto.PremiumRequest;
import dev.coreystevens.titlerate.model.PolicyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PremiumControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
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
    void calculate_invalidState_returns400() throws Exception {
        var body = """
            {"state":"XX","policyType":"OWNER","amount":100000,"simultaneousIssue":false}
            """;

        mvc.perform(post("/api/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
