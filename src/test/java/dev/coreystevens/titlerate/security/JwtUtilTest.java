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
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_withValidToken_returnsSubject() {
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractEmail_withTamperedToken_returnsNull() {
        String token = jwtUtil.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.extractEmail(tampered)).isNull();
    }

    @Test
    void extractEmail_withGarbageString_returnsNull() {
        assertThat(jwtUtil.extractEmail("not.a.token")).isNull();
    }
}
