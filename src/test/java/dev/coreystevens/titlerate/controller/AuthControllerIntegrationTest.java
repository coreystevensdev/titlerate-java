package dev.coreystevens.titlerate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.coreystevens.titlerate.dto.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerAndGetToken(String email) throws Exception {
        var req = new AuthRequest(email, "correct-horse-battery-staple");
        var result = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void me_withoutToken_returns403() throws Exception {
        // No credentials at all: Spring Security treats the request as the anonymous
        // principal, which fails hasRole("USER") as an access-denied case (403), not
        // a missing-authentication case (401). Same behavior anyRequest().authenticated()
        // already used before this endpoint existed.
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    void me_withValidToken_returnsOwnProfile() throws Exception {
        String token = registerAndGetToken("me-owner@example.com");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("me-owner@example.com"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void me_returnsCallersOwnIdentity_notAnotherUsers() throws Exception {
        registerAndGetToken("first-user@example.com");
        String secondToken = registerAndGetToken("second-user@example.com");

        // /me never takes a caller-supplied id: the second user's token can only ever
        // resolve to the second user's own record, regardless of who registered first.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + secondToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("second-user@example.com"));
    }

    @Test
    void me_withTamperedToken_returns403() throws Exception {
        String token = registerAndGetToken("tampered@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        // JwtFilter can't verify the tampered signature, so it never sets an
        // Authentication, and the request falls through as anonymous, same 403 path
        // as no token at all.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
            .andExpect(status().isForbidden());
    }
}
