package com.acme.assistant.domain;

/**
 * Specialist that owns the next turn. Produced by the router LLM, consumed by
 * LangGraph4j conditional edges. Keep the names stable: they are both graph
 * edge keys and log/metric labels.
 */
public enum AgentIntent {
    CONCIERGE,
    TECHNICAL,
    AFTER_SALES;

    public String modelRole() {
        return this == TECHNICAL ? "gemini-pro" : "gemini-flash";
    }
}
