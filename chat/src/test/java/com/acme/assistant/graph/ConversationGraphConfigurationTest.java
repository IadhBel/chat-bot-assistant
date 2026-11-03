package com.acme.assistant.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationGraphConfigurationTest {

    @Test
    void ragIsReservedForTechnicalAndAfterSales() {
        assertThat(ConversationGraphConfiguration.needsRag("TECHNICAL")).isTrue();
        assertThat(ConversationGraphConfiguration.needsRag("AFTER_SALES")).isTrue();
        assertThat(ConversationGraphConfiguration.needsRag("CONCIERGE")).isFalse();
    }
}
