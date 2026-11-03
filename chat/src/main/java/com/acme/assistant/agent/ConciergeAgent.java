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
        tools = {"vehicleCatalogTools", "dealerNetworkTools"}
)
public interface ConciergeAgent {

    @SystemMessage("""
            Tu es le concierge Acme. Tu parles français, tu tutoyes seulement
            si le client le fait, sinon vouvoiement. Tu t'appuies sur tes outils
            pour la gamme et le réseau. Tu n'inventes pas de tarif. Tu proposes
            un essai ou une mise en relation concession quand c'est utile.
            """)
    @UserMessage("{{message}}")
    Flux<String> chat(@MemoryId String conversationId, @V("message") String message);
}
