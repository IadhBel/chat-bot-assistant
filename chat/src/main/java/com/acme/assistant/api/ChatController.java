package com.acme.assistant.api;

import com.acme.assistant.api.dto.ChatEvent;
import com.acme.assistant.api.dto.ChatRequest;
import com.acme.assistant.application.ConversationFacade;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {

    private final ConversationFacade conversationFacade;

    public ChatController(ConversationFacade conversationFacade) {
        this.conversationFacade = conversationFacade;
    }

    @PostMapping(path = "/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEvent>> chat(
            @PathVariable String conversationId,
            @Valid @RequestBody ChatRequest request
    ) {
        return conversationFacade.stream(conversationId, request.message())
                .map(event -> ServerSentEvent.builder(event).event(event.type()).build());
    }
}
