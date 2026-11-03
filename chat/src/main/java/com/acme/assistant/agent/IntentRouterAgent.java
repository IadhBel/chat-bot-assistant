package com.acme.assistant.agent;

import com.acme.assistant.domain.AgentIntent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "geminiFlashChatModel")
public interface IntentRouterAgent {

    @SystemMessage("""
            Tu es le routeur d'intention de l'assistant Acme.
            Réponds uniquement par l'une des valeurs : CONCIERGE, TECHNICAL, AFTER_SALES.
            CONCIERGE : gamme, essai, concession, prix, financement, accueil.
            TECHNICAL : fiche technique, batterie, autonomie, recharge, diagnostic, performances.
            AFTER_SALES : entretien, garantie, révision, rappel, atelier, pièce.
            Si le message mélange plusieurs sujets, choisis le plus opérationnel pour le client.
            """)
    @UserMessage("Classifie l'intention du message client suivant : {{message}}")
    AgentIntent route(@V("message") String message);
}
