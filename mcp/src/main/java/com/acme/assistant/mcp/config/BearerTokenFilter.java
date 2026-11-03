package com.acme.assistant.mcp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Inner gate on every request except Cloud Run health probes.
 */
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String PREFIX = "Bearer ";

    private final McpProperties.Security security;
    private final byte[] expected;

    public BearerTokenFilter(McpProperties properties) {
        McpProperties.Security configured = properties.security();
        this.security = configured == null ? new McpProperties.Security(false, null) : configured;
        this.security.validate();
        this.expected = this.security.readToken() == null
                ? new byte[0]
                : this.security.readToken().getBytes(StandardCharsets.UTF_8);
        if (!this.security.enabled()) {
            log.warn("Application-level authentication is DISABLED. "
                    + "Acceptable locally; never in a deployed environment.");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!security.enabled()) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX) || !matches(header.substring(PREFIX.length()))) {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"acme-knowledge\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid or missing bearer token");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean matches(String presented) {
        return MessageDigest.isEqual(expected, presented.trim().getBytes(StandardCharsets.UTF_8));
    }
}
