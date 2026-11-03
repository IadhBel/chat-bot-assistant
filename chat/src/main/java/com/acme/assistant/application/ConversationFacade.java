package com.acme.assistant.application;

import com.acme.assistant.agent.AfterSalesAgent;
import com.acme.assistant.agent.ConciergeAgent;
import com.acme.assistant.agent.TechnicalAgent;
import com.acme.assistant.api.dto.ChatEvent;
import com.acme.assistant.domain.AgentIntent;
import com.acme.assistant.graph.ConversationGraphState;
import com.acme.assistant.graph.RoutedTurn;
import com.acme.assistant.rag.RetrievedContext;
import com.acme.assistant.rag.source.KnowledgeSourceConfiguration.KnowledgeSourceRegistry;
import org.bsc.langgraph4j.CompiledGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Service
public class ConversationFacade {

    private static final Logger log = LoggerFactory.getLogger(ConversationFacade.class);

    private final CompiledGraph<ConversationGraphState> conversationGraph;
    private final ConciergeAgent conciergeAgent;
    private final TechnicalAgent technicalAgent;
    private final AfterSalesAgent afterSalesAgent;
    private final MultimodalSpecialistStreamer multimodalStreamer;
    private final KnowledgeSourceRegistry sourceRegistry;

    public ConversationFacade(
            CompiledGraph<ConversationGraphState> conversationGraph,
            ConciergeAgent conciergeAgent,
            TechnicalAgent technicalAgent,
            AfterSalesAgent afterSalesAgent,
            MultimodalSpecialistStreamer multimodalStreamer,
            KnowledgeSourceRegistry sourceRegistry
    ) {
        this.conversationGraph = conversationGraph;
        this.conciergeAgent = conciergeAgent;
        this.technicalAgent = technicalAgent;
        this.afterSalesAgent = afterSalesAgent;
        this.multimodalStreamer = multimodalStreamer;
        this.sourceRegistry = sourceRegistry;
    }

    public Flux<ChatEvent> stream(String conversationId, String message) {
        return Mono.fromCallable(() -> route(conversationId, message))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(turn -> Flux.concat(
                        Flux.just(ChatEvent.meta(
                                turn.intent().name(),
                                turn.intent().modelRole(),
                                turn.corpusGeneration()
                        )),
                        specialistStream(turn)
                                .map(ChatEvent::token)
                                .onErrorResume(error -> {
                                    log.error("Specialist failed for {}", conversationId, error);
                                    return Flux.just(ChatEvent.error("Le modèle n'a pas pu répondre."));
                                }),
                        Flux.just(ChatEvent.done())
                ));
    }

    private RoutedTurn route(String conversationId, String message) {
        ConversationGraphState state = conversationGraph.invoke(Map.of(
                ConversationGraphState.CONVERSATION_ID, conversationId,
                ConversationGraphState.USER_MESSAGE, message
        )).orElseThrow(() -> new IllegalStateException("Conversation graph produced no state"));
        List<RetrievedContext.RetrievedImage> images = state.ragImageUris().stream()
                .map(sourceRegistry::load)
                .flatMap(java.util.Optional::stream)
                .map(payload -> new RetrievedContext.RetrievedImage(
                        payload.object().uri(),
                        payload.object().contentType(),
                        null,
                        payload.bytes()
                ))
                .toList();
        return new RoutedTurn(
                state.conversationId(),
                state.userMessage(),
                AgentIntent.valueOf(state.intent()),
                state.ragContext(),
                images,
                state.corpusGeneration()
        );
    }

    private Flux<String> specialistStream(RoutedTurn turn) {
        return switch (turn.intent()) {
            case CONCIERGE -> conciergeAgent.chat(turn.conversationId(), turn.userMessage());
            case TECHNICAL -> turn.images().isEmpty()
                    ? technicalAgent.chat(turn.conversationId(), turn.ragContext(), turn.userMessage())
                    : multimodalStreamer.technical(turn.ragContext(), turn.userMessage(), turn.images());
            case AFTER_SALES -> turn.images().isEmpty()
                    ? afterSalesAgent.chat(turn.conversationId(), turn.ragContext(), turn.userMessage())
                    : multimodalStreamer.afterSales(turn.ragContext(), turn.userMessage(), turn.images());
        };
    }
}
