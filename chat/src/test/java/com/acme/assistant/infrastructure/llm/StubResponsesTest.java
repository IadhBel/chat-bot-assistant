package com.acme.assistant.infrastructure.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubResponsesTest {

    @Test
    void classifiesTechnicalAfterSalesAndConcierge() {
        assertThat(StubResponses.classify("autonomie batterie megane")).isEqualTo("TECHNICAL");
        assertThat(StubResponses.classify("prise de rendez-vous entretien garantie")).isEqualTo("AFTER_SALES");
        assertThat(StubResponses.classify("essai clio a paris")).isEqualTo("CONCIERGE");
    }

    @Test
    void classificationPromptReturnsIntentName() {
        String reply = StubResponses.reply("Classifie l'intention du message client suivant : autonomie Megane");
        assertThat(reply).isEqualTo("TECHNICAL");
    }
}
