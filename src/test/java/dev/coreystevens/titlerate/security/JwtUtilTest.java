package dev.coreystevens.titlerate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "a-very-long-test-secret-key-minimum-32-chars!!",
            "titlerate-test",
            1L
        );
    }

    @Test
    void generateToken_returnsNonBlankString() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_withValidToken_returnsSubject() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractEmail_withTamperedToken_returnsNull() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.extractEmail(tampered)).isNull();
    }

    @Test
    void extractEmail_withGarbageString_returnsNull() {
        assertThat(jwtUtil.extractEmail("not.a.token")).isNull();
    }

    @Test
    void extractRole_withValidToken_returnsRole() {
        String token = jwtUtil.generateToken("user@example.com", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_withTamperedToken_returnsNull() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.extractRole(tampered)).isNull();
    }
}
