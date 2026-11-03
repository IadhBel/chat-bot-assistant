package com.acme.assistant.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acme.mcp")
public record McpProperties(
        Security security,
        Provider provider,
        int defaultLimit,
        int maxLimit,
        int snippetChars,
        VertexRag engine
) {

    public enum Provider {
        vertex,
        stub
    }

    public Provider provider() {
        return provider == null ? Provider.stub : provider;
    }

    public int defaultLimit() {
        return defaultLimit <= 0 ? 8 : defaultLimit;
    }

    public int maxLimit() {
        return maxLimit <= 0 ? 20 : maxLimit;
    }

    public int snippetChars() {
        return snippetChars <= 0 ? 1200 : snippetChars;
    }

    public VertexRag engine() {
        return engine == null
                ? new VertexRag(null, "europe-west1", null, null, "knowledge/", "meta/corpus-generation")
                : engine;
    }

    public int clampLimit(Integer limit) {
        int requested = limit == null || limit <= 0 ? defaultLimit() : limit;
        return Math.min(requested, maxLimit());
    }

    public record Security(boolean enabled, String readToken) {

        public void validate() {
            if (enabled && (readToken == null || readToken.isBlank())) {
                throw new IllegalStateException(
                        "acme.mcp.security.read-token is required when security is enabled; "
                        + "set it from Secret Manager, or set acme.mcp.security.enabled=false "
                        + "for local development only");
            }
        }
    }

    public record VertexRag(
            String project,
            String location,
            String corpus,
            String gcsBucket,
            String gcsPrefix,
            String generationObject
    ) {
    }
}
