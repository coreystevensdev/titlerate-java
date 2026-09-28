package dev.coreystevens.titlerate.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sliding-window ceilings for the two unauthenticated auth endpoints.
 *
 * They are separate windows because they are different abuse shapes. Register
 * writes a row, so the cost of a flood is unbounded storage; login is the
 * credential-stuffing target and each attempt costs a BCrypt verification. A
 * shared window would let a flood of one spend the other's budget.
 */
@Component
public class AuthRateLimiter {

    private final Window register;
    private final Window login;

    public AuthRateLimiter(
        @Value("${auth.ratelimit.register-per-hour}") int registerPerHour,
        @Value("${auth.ratelimit.login-per-minute}") int loginPerMinute) {
        this.register = new Window(registerPerHour, Duration.ofHours(1));
        this.login = new Window(loginPerMinute, Duration.ofMinutes(1));
    }

    public boolean allowRegister(String client) { return register.allow(client); }

    public boolean allowLogin(String client) { return login.allow(client); }

    public long registerRetryAfterSeconds() { return register.window.toSeconds(); }

    public long loginRetryAfterSeconds() { return login.window.toSeconds(); }

    /** Clears both windows. Used by tests, and by an operator undoing a bad lockout. */
    public void reset() {
        register.clear();
        login.clear();
    }

    private static final class Window {

        /** Calls between sweeps. Without one the map keeps an entry per source
         *  address for the life of the process, which a caller rotating addresses
         *  turns into a memory leak. */
        private static final int SWEEP_EVERY = 256;

        private final Map<String, List<Long>> hits = new HashMap<>();
        private final int limit;
        private final Duration window;
        private int calls;

        Window(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }

        // nanoTime, not currentTimeMillis: this measures elapsed time, and a clock
        // adjustment mid-window should not hand out or withhold budget.
        synchronized boolean allow(String client) {
            long now = System.nanoTime();
            long cutoff = now - window.toNanos();

            if (++calls % SWEEP_EVERY == 0) {
                evictIdle(cutoff);
            }

            List<Long> recent = new ArrayList<>();
            for (long at : hits.getOrDefault(client, List.of())) {
                if (at > cutoff) {
                    recent.add(at);
                }
            }

            if (recent.size() >= limit) {
                hits.put(client, recent);
                return false;
            }

            // A rejection deliberately records nothing. If it did, a client in a
            // retry loop would keep pushing its own window forward and never recover.
            recent.add(now);
            hits.put(client, recent);
            return true;
        }

        private void evictIdle(long cutoff) {
            hits.entrySet().removeIf(e -> e.getValue().isEmpty()
                || e.getValue().get(e.getValue().size() - 1) <= cutoff);
        }

        synchronized void clear() {
            hits.clear();
            calls = 0;
        }
    }
}
