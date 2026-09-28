package dev.coreystevens.titlerate.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuthRateLimitFilter authRateLimitFilter;

    public SecurityConfig(JwtFilter jwtFilter, AuthRateLimitFilter authRateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.authRateLimitFilter = authRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // /error has to be here. A 400 or 404 from a permitAll endpoint is
                // re-dispatched through this filter chain as an ERROR request, and
                // without it anyRequest().authenticated() answers 403 instead, so an
                // anonymous client sees Forbidden for a validation failure. The
                // existing tests miss it twice over: they carry @WithMockUser, and
                // MockMvc resolves handler exceptions without dispatching to /error.
                //
                // /api/rates was listed here with no controller behind it.
                .requestMatchers("/error", "/actuator/health", "/api/auth/register", "/api/auth/login", "/api/calculate").permitAll()
                .requestMatchers("/api/auth/me").hasRole("USER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // Ahead of jwtFilter so a login flood is refused before it costs a
            // BCrypt verification.
            .addFilterBefore(authRateLimitFilter, JwtFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
