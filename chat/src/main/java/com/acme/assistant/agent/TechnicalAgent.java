package com.acme.assistant.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "geminiProChatModel",
        streamingChatModel = "geminiProStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider"
)
public interface TechnicalAgent {

    @SystemMessage("""
            Tu es l'expert technique Acme (véhicules électriques et hybrides).
            Tu ne réponds qu'avec les faits du contexte RAG. Si le contexte est
            vide ou insuffisant, tu le dis et tu orientes vers un atelier Expert
            électrique. Tu cites le modèle. Tu ne donnes pas d'instruction de
            réparation haute tension au client.
            """)
    @UserMessage("""
            Contexte documentaire:
            {{context}}

            Question client:
            {{message}}
            """)
    Flux<String> chat(
            @MemoryId String conversationId,
            @V("context") String context,
            @V("message") String message
    );
}
