package com.acme.assistant.infrastructure.security;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ApiTokenWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AcmeProperties properties;

    public ApiTokenWebFilter(AcmeProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.security().enabled() || isPublic(exchange.getRequest().getURI().getPath())) {
            return chain.filter(exchange);
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String expected = properties.security().apiToken();
        if (expected == null || expected.isBlank() || header == null || !header.startsWith(BEARER_PREFIX)
                || !expected.equals(header.substring(BEARER_PREFIX.length()))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private static boolean isPublic(String path) {
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || "/".equals(path)
                || path.startsWith("/index.html")
                || path.startsWith("/assets/");
    }
}
