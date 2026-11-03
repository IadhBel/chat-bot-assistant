package com.acme.assistant.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentIntentTest {

    @Test
    void technicalUsesProOthersUseFlash() {
        assertThat(AgentIntent.TECHNICAL.modelRole()).isEqualTo("gemini-pro");
        assertThat(AgentIntent.CONCIERGE.modelRole()).isEqualTo("gemini-flash");
        assertThat(AgentIntent.AFTER_SALES.modelRole()).isEqualTo("gemini-flash");
    }
}
