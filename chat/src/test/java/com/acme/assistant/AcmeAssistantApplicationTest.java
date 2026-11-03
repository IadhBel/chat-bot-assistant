package com.acme.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcmeAssistantApplicationTest {

    @LocalServerPort
    private int port;

    @Test
    void contextStartsAndChatStreams() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .post()
                .uri("/api/v1/conversations/it-1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("{\"message\":\"Je voudrais un essai Clio a Paris\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }
}
