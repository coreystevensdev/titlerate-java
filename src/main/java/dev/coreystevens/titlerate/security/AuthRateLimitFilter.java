package dev.coreystevens.titlerate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Caps /api/auth/register and /api/auth/login per caller.
 *
 * Sits ahead of authentication in the chain on purpose: a login flood should be
 * refused before it costs a BCrypt verification, not after.
 *
 * The client key is getRemoteAddr(). Behind a reverse proxy that is the proxy for
 * every request, which would put the whole internet in one bucket, so
 * server.forward-headers-strategy=NATIVE is set and Tomcat's RemoteIpValve
 * rewrites it from X-Forwarded-For. That valve only believes the header when the
 * immediate peer is in its internalProxies set, private ranges by default, so a
 * caller reaching the port directly cannot spoof its way to a fresh budget.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN = "/api/auth/login";

    private final AuthRateLimiter limiter;

    public AuthRateLimitFilter(AuthRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        String client = req.getRemoteAddr();

        if (REGISTER.equals(path) && !limiter.allowRegister(client)) {
            reject(res, limiter.registerRetryAfterSeconds());
            return;
        }
        if (LOGIN.equals(path) && !limiter.allowLogin(client)) {
            reject(res, limiter.loginRetryAfterSeconds());
            return;
        }

        chain.doFilter(req, res);
    }

    private void reject(HttpServletResponse res, long retryAfterSeconds) throws IOException {
        res.setStatus(429);
        res.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"message\":\"Too many requests. Retry after "
            + retryAfterSeconds + " seconds.\"}");
    }
}
