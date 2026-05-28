package com.complipilot.backend.common.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RateLimitService rateLimitService;

    public AuthRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!shouldRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "auth:" + clientIp(request);
        RateLimitDecision decision = rateLimitService.consumeAuthToken(key);

        response.setHeader(
                RATE_LIMIT_REMAINING_HEADER,
                String.valueOf(decision.remaining())
        );
        response.setHeader(
                RATE_LIMIT_RESET_HEADER,
                decision.resetAt().toString()
        );

        if (!decision.allowed()) {
            long retryAfterSeconds = Math.max(
                    1,
                    Duration.between(Instant.now(), decision.resetAt()).toSeconds()
            );

            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
            response.sendError(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many authentication requests. Please try again later."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();

        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
