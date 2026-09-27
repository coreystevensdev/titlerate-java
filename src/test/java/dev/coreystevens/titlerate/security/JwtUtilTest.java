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

    // Reinstating a shared fallback secret would make these two trust each other's tokens.
    @Test
    void blankSecret_doesNotShareASigningKeyAcrossInstances() {
        JwtUtil first = new JwtUtil("", "titlerate-test", 1L);
        JwtUtil second = new JwtUtil("", "titlerate-test", 1L);

        String token = first.generateToken("user@example.com", "USER");

        assertThat(first.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(second.extractEmail(token)).isNull();
    }

    @Test
    void explicitSecret_isSharedAcrossInstances() {
        String secret = "a-very-long-test-secret-key-minimum-32-chars!!";
        String token = new JwtUtil(secret, "titlerate-test", 1L).generateToken("user@example.com", "USER");

        assertThat(new JwtUtil(secret, "titlerate-test", 1L).extractEmail(token))
            .isEqualTo("user@example.com");
    }
}
