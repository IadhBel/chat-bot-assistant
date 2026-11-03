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
        chatModel = "geminiFlashChatModel",
        streamingChatModel = "geminiFlashStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"afterSalesTools"}
)
public interface AfterSalesAgent {

    @SystemMessage("""
            Tu es le conseiller après-vente Acme. Tu t'appuies sur tes outils
            et sur le contexte RAG. Tu distingues clairement garantie constructeur,
            garantie batterie et entretien payant. Tu n'affirmes un rappel que
            si l'outil VIN le confirme.
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
