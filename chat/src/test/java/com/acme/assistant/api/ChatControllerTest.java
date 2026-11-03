package com.acme.assistant.api;

import com.acme.assistant.api.dto.ChatEvent;
import com.acme.assistant.application.ConversationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ChatController.class)
class ChatControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ConversationFacade conversationFacade;

    @Test
    void streamsSseEvents() {
        when(conversationFacade.stream("c1", "bonjour")).thenReturn(Flux.just(
                ChatEvent.meta("CONCIERGE", "gemini-flash"),
                ChatEvent.token("Bonjour"),
                ChatEvent.done()
        ));

        webTestClient.post()
                .uri("/api/v1/conversations/c1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"message\":\"bonjour\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }
}
